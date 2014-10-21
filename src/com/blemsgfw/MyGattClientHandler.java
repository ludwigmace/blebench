package com.blemsgfw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

public interface MyGattClientHandler {
	
	//public void getFoundDevice(BluetoothDevice device, int rssi, byte[] scanRecord);
	
	public void getFoundDevices(ArrayList<BluetoothDevice> devices);
	
	public void getFoundCharacteristic(String serviceUUID, BluetoothGattCharacteristic foundChar);
	public void getFoundCharacteristics(List<BluetoothGattCharacteristic> foundChars);
	
	public void getReadCharacteristic(String charUUID, byte[] charValue);
	
	public void getNotifyUpdate(String charUUID, byte[] charValue);
	
	public void getWriteResult(String charUUID, int result);
	
	public void reportDisconnect();
	
}
