package com.blemsgfw;

import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

public class BleMessenger {
	private static String TAG = "blemessenger";
	
	// handles to the device's bluetooth
	private BluetoothManager btMgr;
	private BluetoothAdapter btAdptr;
	private Context ctx;
	
	//  this is defined by the framework, but certainly the developer or user can change it
    private static String uuidServiceBase = "73A20000-2C47-11E4-8C21-0800200C9A66";
    private static MyAdvertiser myGattServer = null;
    private static MyCentral myGattClient = null; 
    
    private BleStatusCallback bleStatusCallback;
    
    private BleMessage blmsgOut;
	
	public BleMessenger(BluetoothManager m, BluetoothAdapter a, Context c) {
		btMgr = m;
		btAdptr = a;
		ctx = c;
	}
	
	public void sendMessage(BleMessage message, BleStatusCallback blestatuscallback) {
	
		// TODO add a way to switch b/w Peripheral and Central modes; for right now we'll start with Peripheral
		myGattServer = new MyAdvertiser(uuidServiceBase, ctx, btAdptr, btMgr, defaultHandler);
		myGattClient = new MyCentral(btAdptr, ctx, clientHandler);
		
		// add characteristics, to send/receive data as well as control
		UUID indicateData = myGattServer.addChar(MyAdvertiser.GATT_INDICATE, dataHandler);
		UUID notifyData = myGattServer.addChar(MyAdvertiser.GATT_NOTIFY, dataHandler);
		UUID readData = myGattServer.addChar(MyAdvertiser.GATT_READ, dataHandler);
		UUID writeData = myGattServer.addChar(MyAdvertiser.GATT_WRITE, dataHandler);
        
		UUID readwriteControl = myGattServer.addChar(MyAdvertiser.GATT_READWRITE, controlHandler);
        
		// give us a hook to notify the calling Activity
		bleStatusCallback = blestatuscallback;
		
		// start advertising these
		//myGattServer.advertiseNow();
		
		UUID[] serviceUuids = new UUID[1];
		serviceUuids[0] = UUID.fromString(uuidServiceBase);
		
		myGattClient.scanLeDevice(true, serviceUuids);
		
		// TODO: convert this to use a list of messages, not just a single message
		blmsgOut = message;
		
		// prep the read characteristic with the first packet in the message, but don't increment the counter
		byte[] firstPacket = blmsgOut.GetPacket(0).MessageBytes;
		myGattServer.updateCharValue(readData, firstPacket);
		
	}
	
    private void sendIndicateNotify(UUID uuid) {
    	byte[] nextPacket = blmsgOut.GetPacket().MessageBytes;
    	
    	boolean msgSent = myGattServer.updateCharValue(uuid, nextPacket);
		
    	if (blmsgOut.PendingPacketStatus()) {
    		sendIndicateNotify(uuid);
    	}
    	
    }

	
    MyGattServerHandler defaultHandler = new MyGattServerHandler() {
    	
    	public void handleReadRequest(UUID uuid) { }
    	
    	public void handleNotifyRequest(UUID uuid) { }
    	
    	public void ConnectionState(String dude, int status, int newStatus) {}

		public void incomingBytes(UUID charUUID, byte[] inData) { }
    	
    };

	

	MyGattServerHandler dataHandler = new MyGattServerHandler() {
    	
    	public void handleReadRequest(UUID uuid) {
    		
    		byte[] nextPacket = blmsgOut.GetPacket().MessageBytes;
    		
    		if (blmsgOut.PendingPacketStatus()) {    		
    			Log.v(TAG, "handle read request - call myGattServer.updateCharValue for " + uuid.toString());
    			myGattServer.updateCharValue(uuid, nextPacket);
    		} else {
    			bleStatusCallback.messageSent(uuid);
    		}
    	}
    	
    	public void handleNotifyRequest(UUID uuid) { 
    		
        	byte[] nextPacket = blmsgOut.GetPacket().MessageBytes;
        	boolean msgSent = myGattServer.updateCharValue(uuid, nextPacket);
        	
        	if (msgSent) {        	
        		Log.v(TAG, "client notified with initial message");
        	} else {
        		Log.v(TAG, "client NOT notified with initial message");
        	}
    		
        	// call your self-calling function to keep sending
        	sendIndicateNotify(uuid);
    		
    	}
    	
    	public void ConnectionState(String dude, int status, int newStatus) {}

		public void incomingBytes(UUID charUUID, byte[] inData) { }
    	
    };
    
    MyGattServerHandler controlHandler = new MyGattServerHandler() {
    	
    	public void handleReadRequest(UUID uuid) { }
    	
    	public void handleNotifyRequest(UUID uuid) { }
    	
    	public void ConnectionState(String dude, int status, int newStatus) {}

		public void incomingBytes(UUID charUUID, byte[] inData) { }
    	
    };
    
    MyGattClientHandler clientHandler = new MyGattClientHandler() {
    	
		@Override
		public void getFoundDevices(ArrayList<BluetoothDevice> devices) {
			for (BluetoothDevice b: devices) {
				Log.v(TAG, "MyGattClientHandler found device:" + b.getAddress());	
			}
			
		}

		@Override
		public void getFoundCharacteristic(String serviceUUID,
				BluetoothGattCharacteristic foundChar) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void getReadCharacteristic(String charUUID, byte[] charValue) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void getNotifyUpdate(String charUUID, byte[] charValue) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void getWriteResult(String charUUID, int result) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void reportDisconnect() {
			// TODO Auto-generated method stub
			
		}
    	
    };
	
	
}
