package com.felhr.bleconnector;

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
	public static final String ACTION_NO_CHARACTERISTIC = "com.felhr.bleconnector.action_no_characteristic";
	public static final String ACTION_NO_SERVICE = "com.felhr.bleconnector.action_no_service";
	public static final String ACTION_SCANNING_TERMINATED = "com.felhr.bleconnector.action_scanning_terminated";
	
	public static final String UUIDS_TAG = "com.felhr.bleconnector.uuids_tag";
	public static final String DEVICE_TAG = "com.felhr.bleconnector.device_tag";
	public static final String ADDRESS_TAG = "com.felhr.bleconnector.address_tag";
	
	// Bluetooth Low Energy Assigned UUIDS
	private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString(""); // TO DO
	
	private Context context;
	private Handler mHandler;
	private BluetoothAdapter bleAdapter;
	private WorkerThread workerThread;
	private boolean isScanning;
	private AtomicBoolean operationReady;
	
	private BLEConnectedDevices connectedDevices;
	
	private UUID requestedService;
	private UUID requestedCharacteristic;
	
	private BLEBuffer buffer;

	public BLEConnector(Context context)
	{
		this.context = context;
		buffer = new BLEBuffer(128);
		mHandler = new Handler();
		workerThread = new WorkerThread();
		workerThread.start();
		operationReady = new AtomicBoolean(true);
		final BluetoothManager bluetoothManager =
				(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		bleAdapter = bluetoothManager.getAdapter();
	}

	/* Public Methods */
	
	public void connect(long scanTime, UUID uuidService, UUID uuidCharacteristic)
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
			requestedService = uuidService;
			requestedCharacteristic = uuidCharacteristic;
			isScanning = true;
			bleAdapter.startLeScan(new UUID[]{uuidService}, mScanCallback); // This would only work if service data is in adv packets
		}
	}
	
	public UUID[] listOfServices(String deviceAddress)
	{
		BLEDevice device = connectedDevices.get(deviceAddress);
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
		BLEDevice device = connectedDevices.get(deviceAddress);
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
		BLEDevice device = connectedDevices.get(deviceAddress);
		BluetoothGattService service = device.getGatt().getService(uuidService);
		if(service == null)
			return false;
		connectedDevices.get(deviceAddress).setService(service);
		return true;
	}
	
	public boolean changeCurrentCharacteristic(String deviceAddress, UUID uuidCharacteristic)
	{
		BLEDevice device = connectedDevices.get(deviceAddress);
		BluetoothGattService service = device.getService();
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuidCharacteristic);
		if(characteristic == null)
			return false;
		connectedDevices.get(deviceAddress).setCharacteristic(characteristic);
		return true;
	}
	
	public void writeCharacteristic(String deviceAddress, byte[] message)
	{
		BLEDevice device = connectedDevices.get(deviceAddress);
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


	/* Bluetooth Low Energy API Callbacks */

	private final BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback()
	{

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) 
		{
			new GATTConnectionThread(device, rssi).start();
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
					BLEDevice bleDevice  = connectedDevices.get(gatt.getDevice().getAddress());
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
	
	private class GATTConnectionThread extends Thread
	{
		private BluetoothDevice device;
		private int rssi;
		public GATTConnectionThread(BluetoothDevice device,int rssi)
		{
			this.device = device;
			this.rssi = rssi;
		}
		
		@Override
		public void run()
		{
			BluetoothGatt gatt = device.connectGatt(context, false, mGattCallback);
			BLEDevice deviceBle = new BLEDevice(device,gatt);
			connectedDevices.put(device.getAddress(), deviceBle);
		}
	}
	
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
				connectedDevices.setAllNotifications(false);
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
		private BLEDevice device;
		
		public QueuedMessage(byte[] message, BLEDevice device)
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

		public BLEDevice getDevice() 
		{
			return device;
		}

		public void setDevice(BLEDevice device) 
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
