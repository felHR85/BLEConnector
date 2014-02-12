package com.felhr.bleconnector;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

public class BLEDevice 
{
	private BluetoothDevice device;
	private BluetoothGatt gatt;
	private BluetoothGattService service; // Current Service
	private BluetoothGattCharacteristic characteristic; // Current Characteristic
	
	public BLEDevice(BluetoothDevice device, BluetoothGatt gatt, BluetoothGattService service
			,BluetoothGattCharacteristic characteristic)
	{
		this.device = device;
		this.gatt = gatt;
		this.service = service;
		this.characteristic = characteristic;
	}
	
	public BLEDevice(BluetoothDevice device, BluetoothGatt gatt)
	{
		this.device = device;
		this.gatt = gatt;
	}

	public BluetoothDevice getDevice() 
	{
		return device;
	}

	public void setDevice(BluetoothDevice device) 
	{
		this.device = device;
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
	
	

}
