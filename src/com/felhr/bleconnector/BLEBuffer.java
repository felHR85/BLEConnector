package com.felhr.bleconnector;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.felhr.bleconnector.BLEConnector.QueuedMessage;

public class BLEBuffer 
{
	private BlockingQueue<QueuedMessage> inputBuffer;
	private BlockingQueue<QueuedMessage> outputBuffer;
	
	public BLEBuffer(int bufferSize)
	{
		outputBuffer = new ArrayBlockingQueue<QueuedMessage>(bufferSize);
		inputBuffer = new ArrayBlockingQueue<QueuedMessage>(bufferSize);
	}
	
	public boolean putToOutput(QueuedMessage message)
	{
		try 
		{
			outputBuffer.put(message);
			return true;
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean putToInput(QueuedMessage message)
	{
		try 
		{
			inputBuffer.put(message);
			return true;
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public QueuedMessage getFromOutput()
	{
		try 
		{
			return outputBuffer.take();
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public QueuedMessage getFromInput()
	{
		try 
		{
			return inputBuffer.take();
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
			return null;
		}
	}

}
