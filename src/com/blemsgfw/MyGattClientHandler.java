package com.blemsgfw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

public interface MyGattClientHandler {
	
	//public void getFoundDevice(BluetoothDevice device, int rssi, byte[] scanRecord);
	
	public void getFoundDevices(ArrayList<BluetoothDevice> devices);
	
	public void getFoundCharacteristic(String serviceUUID, BluetoothGattCharacteristic foundChar);
	public void getFoundCharacteristics(List<BluetoothGattCharacteristic> foundChars);
	public void getFoundCharacteristics(BluetoothGatt gatt, List<BluetoothGattCharacteristic> foundChars);
	
	public void getReadCharacteristic(BluetoothGattCharacteristic readChar, byte[] charValue, int status);
	
	public void getReadCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic readChar, byte[] charValue, int status);
	
	public void readCharacteristicReturned(BluetoothGatt gatt, BluetoothGattCharacteristic readChar, byte[] charValue, int status);
	
	public void getNotifyUpdate(String charUUID, byte[] charValue);
	
	public void getWriteResult(BluetoothGattCharacteristic writtenCharacteristic, int result);
	
	public void reportDisconnect();
	
}
