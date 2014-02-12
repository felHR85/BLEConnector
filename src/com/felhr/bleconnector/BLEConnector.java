package com.felhr.bleconnector;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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
	
	private Context context;
	private Handler mHandler;
	private BluetoothAdapter bleAdapter;
	private boolean genericScanning;
	
	private HashMap<String,BLEDevice> connectedDevices;
	
	private UUID requestedService;
	private UUID requestedCharacteristic;
	
	private BLEBuffer buffer;

	public BLEConnector(Context context)
	{
		this.context = context;
		buffer = new BLEBuffer(128);
		mHandler = new Handler();
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
					bleAdapter.stopLeScan(mScanCallback);
					Intent intent = new Intent(ACTION_SCANNING_TERMINATED);
					context.sendBroadcast(intent);
				}
			}, scanTime);
			genericScanning = false;
			requestedService = uuidService;
			requestedCharacteristic = uuidCharacteristic;
			bleAdapter.startLeScan(new UUID[]{uuidService}, mScanCallback);
		}
	}

	public void connect(long scanTime)
	{
		mHandler.postDelayed(new Runnable()
		{
			@Override
			public void run() 
			{
				bleAdapter.stopLeScan(mScanCallback);
				Intent intent = new Intent(ACTION_SCANNING_TERMINATED);
				context.sendBroadcast(intent);
			}
		}, scanTime);
		genericScanning = true;
		bleAdapter.startLeScan(mScanCallback);
	}
	
	public void writeCharacteristic(String deviceAddress, byte[] message)
	{
		
	}
	
	public void readCharacteristic(String deviceAddress, byte[] message)
	{
		
	}
	
	public void registerForNotifications(String deviceAddress, UUID uuidService, UUID uuidCharacteristic)
	{
		
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
			if(genericScanning)
			{
				List<BluetoothGattService> services = gatt.getServices();
				String[] uuids = new String[services.size()];
				for(int i=0;i<=services.size()-1;i++)
				{
					uuids[i] = services.get(i).getUuid().toString();
				}
				
				Intent intent = new Intent(ACTION_BROADCAST_UUIDS);
				intent.putExtra(UUIDS_TAG, uuids);
				intent.putExtra(DEVICE_TAG, gatt.getDevice().getName());
				intent.putExtra(ADDRESS_TAG, gatt.getDevice().getAddress());
				context.sendBroadcast(intent);
				
			}else
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
	
	/* Inner classes */
	
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
}
