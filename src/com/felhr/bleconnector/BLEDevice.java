package com.felhr.bleconnector;

import android.bluetooth.BluetoothDevice;

public class BLEDevice 
{
	private BluetoothDevice device;
	private int rssi;
	private byte[] scanRecord;
	// Decoded ScanRecord bytes should be here too
	
	public BLEDevice(BluetoothDevice device, int rssi, byte[] scanRecord) 
	{
		this.device = device;
		this.rssi = rssi;
		this.scanRecord = scanRecord;
	}

	public BluetoothDevice getDevice() 
	{
		return device;
	}

	public void setDevice(BluetoothDevice device) 
	{
		this.device = device;
	}

	public int getRssi() 
	{
		return rssi;
	}

	public void setRssi(int rssi) 
	{
		this.rssi = rssi;
	}

	public byte[] getScanRecord() 
	{
		return scanRecord;
	}

	public void setScanRecord(byte[] scanRecord) 
	{
		this.scanRecord = scanRecord;
	}
	
}
