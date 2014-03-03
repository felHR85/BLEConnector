package com.felhr.bleconnector;

import java.util.Hashtable;
import java.util.Map;

public class BLEConnectedDevices extends Hashtable<String,BLEConnectedDevice>
{
	
	private static final long serialVersionUID = 1L;
	
	public BLEConnectedDevices()
	{
		super();
	}
	
	public boolean setAllNotifications(boolean value)
	{
		boolean response = false;
		for(Map.Entry<String, BLEConnectedDevice> entry: this.entrySet())
		{
			BLEConnectedDevice device = entry.getValue();
			if(device.getNotificationsStatus())
			{
				response = true;
				device.getGatt().setCharacteristicNotification(device.getCharacteristic(), value);
			}
		}
		return response;
	}
	
	public boolean setNotifications(String deviceAddress, boolean value)
	{
		BLEConnectedDevice device = get(deviceAddress);
		if(device.getNotificationsStatus())
		{
			return device.getGatt().setCharacteristicNotification(device.getCharacteristic(), value);
		}else
		{
			return false;
		}
		
	}
}
