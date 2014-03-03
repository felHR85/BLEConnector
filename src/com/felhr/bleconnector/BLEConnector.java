package com.felhr.bleconnector;

import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class BLEConnector 
{
	public static final String ACTION_BROADCAST_UUIDS = "com.felhr.bleconnector.action_broadcast_uuids";
	public static final String ACTION_DEVICE_CONNECTED = "com.felhr.bleconnector.action_device_connected";
	public static final String ACTION_DEVICE_SPOTTED = "com.felhr.bleconnector.action_device_spotted";
	public static final String ACTION_NO_CHARACTERISTIC = "com.felhr.bleconnector.action_no_characteristic";
	public static final String ACTION_NO_SERVICE = "com.felhr.bleconnector.action_no_service";
	public static final String ACTION_SCANNING_TERMINATED = "com.felhr.bleconnector.action_scanning_terminated";
	
	public static final String UUIDS_TAG = "com.felhr.bleconnector.uuids_tag";
	public static final String DEVICE_TAG = "com.felhr.bleconnector.device_tag";
	public static final String ADDRESS_TAG = "com.felhr.bleconnector.address_tag";
	public static final String RSSI_TAG = "com.felhr.bleconnector.rssi_tag";
	
	// Bluetooth Low Energy Assigned UUIDS
	private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // TO DO
	
	private int connectMode; // 0 if you are going to spot devices only, 1 if you are going to perform a connection attempt.
	
	private Context context;
	private Handler mHandler;
	private BluetoothAdapter bleAdapter;
	private WorkerThread workerThread;
	private boolean isScanning;
	private AtomicBoolean operationReady;
	
	private BLEConnectedDevices connectedDevices; //Internally it is a hashtable
	private Hashtable<String,BLEDevice> spottedDevices;
	
	private UUID requestedService;
	private UUID requestedCharacteristic;
	
	private BLEBuffer buffer;

	public BLEConnector(Context context)
	{
		this.context = context;
		buffer = new BLEBuffer(128);
		spottedDevices = new Hashtable<String,BLEDevice>();
		mHandler = new Handler();
		workerThread = new WorkerThread();
		workerThread.start();
		operationReady = new AtomicBoolean(true);
		final BluetoothManager bluetoothManager =
				(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		bleAdapter = bluetoothManager.getAdapter();
	}

	/* Public Methods */
	
	public void findBLEDevices(long scanTime)
	{
		spottedDevices.clear();
		connectMode = 0;
		startScan(scanTime);
	}
	
	public void connect(String device, UUID uuidService, UUID uuidCharacteristic)
	{
		// TO-DO
	}
	
	public UUID[] listOfServices(String deviceAddress)
	{
		BLEConnectedDevice device = connectedDevices.get(deviceAddress);
		if(device == null)
			return null;
		List<BluetoothGattService> servicesList = device.getAllServices();
		UUID[] uuids = new UUID[servicesList.size()];
		for(int i=0;i<=servicesList.size()-1;i++)
		{
			BluetoothGattService service = servicesList.get(i);
			uuids[i] = service.getUuid();
		}
		return uuids;
	}
	
	public UUID[] listOfCharacteristics(String deviceAddress, UUID uuidService)
	{
		BLEConnectedDevice device = connectedDevices.get(deviceAddress);
		if(device == null)
			return null;
		BluetoothGattService service = device.getGatt().getService(uuidService);
		List<BluetoothGattCharacteristic> characteristicsList = service.getCharacteristics();
		UUID[] uuids = new UUID[characteristicsList.size()];
		for(int i=0;i<=characteristicsList.size()-1;i++)
		{
			BluetoothGattCharacteristic characteristic = characteristicsList.get(i);
			uuids[i] = characteristic.getUuid();
		}
		return uuids;
	}
	
	public boolean changeCurrentService(String deviceAddress, UUID uuidService)
	{
		BLEConnectedDevice device = connectedDevices.get(deviceAddress);
		BluetoothGattService service = device.getGatt().getService(uuidService);
		if(service == null)
			return false;
		connectedDevices.get(deviceAddress).setService(service);
		return true;
	}
	
	public boolean changeCurrentCharacteristic(String deviceAddress, UUID uuidCharacteristic)
	{
		BLEConnectedDevice device = connectedDevices.get(deviceAddress);
		BluetoothGattService service = device.getService();
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuidCharacteristic);
		if(characteristic == null)
			return false;
		connectedDevices.get(deviceAddress).setCharacteristic(characteristic);
		return true;
	}
	
	public void writeCharacteristic(String deviceAddress, byte[] message)
	{
		BLEConnectedDevice device = connectedDevices.get(deviceAddress);
		buffer.putToOutput(new QueuedMessage(message,device));
	}
	
	public boolean registerForNotifications(String deviceAddress, UUID uuidService, UUID uuidCharacteristic,
			BLENotificationCallback mCallback)
	{
		BluetoothGattCharacteristic characteristic = connectedDevices.get(deviceAddress).getCharacteristic();
		BluetoothGattDescriptor clientCharConfigDescriptor = 
				characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION);
		clientCharConfigDescriptor.setValue(isSetProperty(PropertyType.NOTIFY,characteristic.getProperties()) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
				: BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
		
		workerThread.setCallback(mCallback);
		return connectedDevices.get(deviceAddress).getGatt().writeDescriptor(clientCharConfigDescriptor);
	}

	/* Private Functions */
	
	private void startScan(long scanTime)
	{
		if(bleAdapter != null && bleAdapter.isEnabled())
		{
			mHandler.postDelayed(new Runnable()
			{
				@Override
				public void run() 
				{
					if(isScanning)
					{
						bleAdapter.stopLeScan(mScanCallback);
						Intent intent = new Intent(ACTION_SCANNING_TERMINATED);
						context.sendBroadcast(intent);
					}
				}
			}, scanTime);
			isScanning = true;
			bleAdapter.startLeScan(mScanCallback);
		}
	}

	/* Bluetooth Low Energy API Callbacks */

	private final BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback()
	{

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) 
		{
			if(connectMode == 0) // Store a spotted device
			{
				spottedDevices.put(device.getAddress(), new BLEDevice(device, rssi, scanRecord));
				Intent intent = new Intent(ACTION_DEVICE_SPOTTED);
				intent.putExtra(DEVICE_TAG,device.getName());
				intent.putExtra(ADDRESS_TAG, device.getAddress());
				intent.putExtra(RSSI_TAG, rssi);
				context.sendBroadcast(intent);
			}else if(connectMode == 1)
			{
				
			}
			
		}

	};
	
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
	{
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
		{
			if(status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED)
			{
				gatt.discoverServices();
			}
		}
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{

			BluetoothGattService service = gatt.getService(requestedService);
			if(service != null)
			{
				BluetoothGattCharacteristic characteristic = service.getCharacteristic(requestedCharacteristic);
				if(characteristic != null)
				{
					BLEConnectedDevice bleDevice  = connectedDevices.get(gatt.getDevice().getAddress());
					bleDevice.setService(service);
					bleDevice.setCharacteristic(characteristic);
					String name = bleDevice.getDevice().getName();
					String address = bleDevice.getDevice().getAddress();
					connectedDevices.put(bleDevice.getDevice().getAddress(), bleDevice);

					Intent intent = new Intent(ACTION_DEVICE_CONNECTED);
					intent.putExtra(DEVICE_TAG,name);
					intent.putExtra(ADDRESS_TAG, address);
					context.sendBroadcast(intent);

				}else
				{
					connectedDevices.remove(gatt.getDevice().getAddress());
					Intent intent = new Intent(ACTION_NO_CHARACTERISTIC);
					context.sendBroadcast(intent);
					gatt.close();
				}

			}else
			{
				connectedDevices.remove(gatt.getDevice().getAddress());
				Intent intent = new Intent(ACTION_NO_SERVICE);
				context.sendBroadcast(intent);
				gatt.close();
			}

		}
		
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
		{
			synchronized(this)
			{
				connectedDevices.setAllNotifications(true);
				operationReady.set(true);
				notify();
			}	
		}
		
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
		{
			byte[] data = characteristic.getValue();
			String deviceAddress = gatt.getDevice().getAddress();
			workerThread.receiveNotifications(deviceAddress, data);
		}
	};
	
	
	/* Operation Threads*/
	
	private class WorkerThread extends Thread
	{
		private BLENotificationCallback mCallback;
		private boolean started;
		
		public WorkerThread()
		{
			started = true;
		}
		
		public void setCallback(BLENotificationCallback mCallback)
		{
			this.mCallback = mCallback;
		}
		
		@Override
		public void run()
		{
			sendMessage();
		}
		
		private synchronized void sendMessage()
		{
			while(started)
			{
				QueuedMessage message = buffer.getFromOutput();
				while(!operationReady.get())
				{
					try 
					{
						wait();
					} catch (InterruptedException e) 
					{
						e.printStackTrace();
					}
				}
				connectedDevices.setAllNotifications(false);
				BluetoothGatt gatt = message.getDevice().getGatt();
				BluetoothGattCharacteristic characteristic = message.getDevice().getCharacteristic();
				characteristic.setValue(message.getMessage());
				gatt.writeCharacteristic(characteristic);
				operationReady.set(false);
			}
		}
		
		public void receiveNotifications(String deviceAddress, byte[] data)
		{
			mCallback.onReceivedNotification(deviceAddress, data);
		}
	}
	
	/* Inner classes and Enums */
	
	protected class QueuedMessage
	{
		private byte[] message;
		private BLEConnectedDevice device;
		
		public QueuedMessage(byte[] message, BLEConnectedDevice device)
		{
			this.message = message;
			this.device = device;
		}

		public byte[] getMessage() 
		{
			return message;
		}

		public void setMessage(byte[] message) 
		{
			this.message = message;
		}

		public BLEConnectedDevice getDevice() 
		{
			return device;
		}

		public void setDevice(BLEConnectedDevice device) 
		{
			this.device = device;
		}	
	}
	
	public interface BLENotificationCallback
	{
		public void onReceivedNotification(String addressDevice, byte[] data);
	}
	
	private enum PropertyType
	{
        BROADCAST(1),
        READ(2),
        WRITE_NO_RESPONSE(4),
        WRITE(8),
        NOTIFY(16),
        INDICATE(32),
        SIGNED_WRITE(64),
        EXTENDED_PROPS(128);

        private final int value;

        private PropertyType(int value)
        {
            this.value = value;
        }

        public int getValue() 
        {
            return value;
        }
    }
	
	 private boolean isSetProperty(PropertyType property, int properties) 
	 {
		 boolean isSet = ((properties >> (property.ordinal())) & 1) != 0;
		 return isSet;
	 }
}
