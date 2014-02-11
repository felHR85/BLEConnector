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
import android.os.Handler;

public class BLEConnector 
{
	private Context context;
	private Handler mHandler;
	private BluetoothAdapter bleAdapter;
	private boolean genericScanning;
	
	private HashMap<String,BLEDevice> connectedDevices;
	
	private UUID requestedService;
	private UUID requestedCharacteristic;

	public BLEConnector(Context context)
	{
		this.context = context;
		mHandler = new Handler();
		final BluetoothManager bluetoothManager =
				(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		bleAdapter = bluetoothManager.getAdapter();
	}

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
			}
		}, scanTime);
		genericScanning = true;
		bleAdapter.startLeScan(mScanCallback);
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
				//Broadcast List of UUIDS (TO DO)
				
			}else
			{
				BluetoothGattService service = gatt.getService(requestedService);
				if(service != null)
				{
					BluetoothGattCharacteristic characteristic = service.getCharacteristic(requestedCharacteristic);
					if(characteristic != null)
					{
						// Add service and characteristic to convenient connectedDevices register (TO DO)
					}else
					{
						gatt.close();
					}
					
				}else
				{
					// Broadcast no service available (TO DO)
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
			// Add deviceBle to connectedDevices (TO DO)
		}
	}
}
