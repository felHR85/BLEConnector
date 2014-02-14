package com.felhr.bleconnector;

import java.util.Hashtable;
import java.util.Map;

public class BLEConnectedDevices extends Hashtable<String,BLEDevice>
{
	
	private static final long serialVersionUID = 1L;
	
	public BLEConnectedDevices()
	{
		super();
	}
	
	public void setAllNotifications(boolean value)
	{
		for(Map.Entry<String, BLEDevice> entry: this.entrySet())
		{
			BLEDevice device = entry.getValue();
			if(device.getNotificationsStatus())
			{
				device.getGatt().setCharacteristicNotification(device.getCharacteristic(), value);
			}
		}
	}
	
	public boolean setNotifications(String deviceAddress, boolean value)
	{
		BLEDevice device = get(deviceAddress);
		if(device.getNotificationsStatus())
		{
			return device.getGatt().setCharacteristicNotification(device.getCharacteristic(), value);
		}else
		{
			return false;
		}
		
	}
}
