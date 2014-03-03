package com.felhr.bleconnector;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

public class BLEConnectedDevice extends BLEDevice
{
	private BluetoothGatt gatt;
	private BluetoothGattService service; // Current Service
	private BluetoothGattCharacteristic characteristic; // Current Characteristic
	private boolean enabledNotifications;
	
	
	public BLEConnectedDevice(BluetoothDevice device, int rssi, byte[] scanRecord, BluetoothGatt gatt, BluetoothGattService service
			,BluetoothGattCharacteristic characteristic)
	{
		super(device,rssi,scanRecord);
		this.gatt = gatt;
		this.service = service;
		this.characteristic = characteristic;
		this.enabledNotifications = false;
	}
	
	public BLEConnectedDevice(BLEDevice device)
	{
		super(device.getDevice(),device.getRssi(),device.getScanRecord());
	}
	
	public BluetoothGatt getGatt() 
	{
		return gatt;
	}

	public void setGatt(BluetoothGatt gatt) 
	{
		this.gatt = gatt;
	}

	public BluetoothGattService getService() 
	{
		return service;
	}

	public void setService(BluetoothGattService service) 
	{
		this.service = service;
	}

	public BluetoothGattCharacteristic getCharacteristic() 
	{
		return characteristic;
	}

	public void setCharacteristic(BluetoothGattCharacteristic characteristic) 
	{
		this.characteristic = characteristic;
	}
	
	public List<BluetoothGattService> getAllServices()
	{
		return gatt.getServices();
	}
	
	public List<BluetoothGattCharacteristic> getAllCharacteristics(UUID serviceuuid)
	{
		return gatt.getService(serviceuuid).getCharacteristics();
	}
	
	public boolean getNotificationsStatus()
	{
		return enabledNotifications;
	}
	
	public void notificationsOn()
	{
		enabledNotifications = true;
	}
	
	public void notificationsOff()
	{
		enabledNotifications = false;
	}
	
	

}
