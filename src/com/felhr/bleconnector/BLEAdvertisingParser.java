package com.felhr.bleconnector;

import java.util.ArrayList;
import java.util.List;

public class BLEAdvertisingParser 
{
	private final static int AD_INCOMPLETE_16UUID_SERVICES = 0x02;
	private final static int AD_COMPLETE_16UUID_SERVICES = 0x03;
	private final static int AD_INCOMPLETE_128UUID_SERVICES = 0x06;
	private final static int AD_COMPLETE_128UUID_SERVICES = 0x07;
	
	private BLEAdvertisingParser()
	{
		
	}
	
	public static String[] parseUUIDS(byte[] advPacket)
	{
		List<String> uuidsList = new ArrayList<String>();
		int lengthAdv = advPacket.length;
		int i = 0;
		if(lengthAdv != 0)
		{
			while(i < lengthAdv)
			{
				int lengthData = advPacket[i];
				int type =  advPacket[i+1];
				int j = i + lengthData;
				switch(type)
				{
				case AD_INCOMPLETE_16UUID_SERVICES:
					while(j <= i-lengthData)
					{
						int service = advPacket[j-1] << 8 | advPacket[j];
						uuidsList.add(Integer.toHexString(service));
						j-=2;
					}
					break;
				case AD_COMPLETE_16UUID_SERVICES:
					while(j <= i-lengthData)
					{
						int service = advPacket[j-1] << 8 | advPacket[j];
						uuidsList.add(Integer.toHexString(service));
						j-=2;
					}
					break;
				case AD_INCOMPLETE_128UUID_SERVICES:
					break;
				case AD_COMPLETE_128UUID_SERVICES:
					break;
				default:
					break;
				}
				i = i + lengthData;
			}
			
			return (String[]) uuidsList.toArray();
		}else
		{
			return null;
		}
	}
	
}
