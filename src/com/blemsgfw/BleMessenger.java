package com.blemsgfw;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.blebench.KeyStuff;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class BleMessenger {
	private static String TAG = "blemessenger";
	
	// handles to the device's bluetooth
	private BluetoothManager btMgr;
	private BluetoothAdapter btAdptr;
	private Context ctx;
	
	private boolean messageInbound;
	private boolean messageOutbound;
	
	//  this is defined by the framework, but certainly the developer or user can change it
    private static String uuidServiceBase = "73A20000-2C47-11E4-8C21-0800200C9A66";
    private static MyAdvertiser myGattServer = null;
    private static MyCentral myGattClient = null; 
    
    private BleStatusCallback bleStatusCallback;
        
    private BleMessage blmsgOut;
    private BleMessage blmsgIn;
    
    private String myIdentifier;
    private String myFriendlyName;
    
    private ArrayList<BlePeer> peerList;
	
	public BleMessenger(BleMessengerOptions options, BluetoothManager m, BluetoothAdapter a, Context c) {
		myIdentifier = options.Identifier;
		myFriendlyName = options.FriendlyName;
		
		btMgr = m;
		btAdptr = a;
		ctx = c;
		
		peerList = new ArrayList<BlePeer>();
		
		// create your server for listening and your client for looking; Android can be both at the same time
		myGattServer = new MyAdvertiser(uuidServiceBase, ctx, btAdptr, btMgr, defaultHandler);
		myGattClient = new MyCentral(btAdptr, ctx, clientHandler);
		
		// set up a read characteristic for your id
		String charIdentifier = "100";
		String strUUID =  uuidServiceBase.substring(0, 4) + new String(new char[4-charIdentifier.length()]).replace("\0", "0") + charIdentifier + uuidServiceBase.substring(8, uuidServiceBase.length());
		UUID idUUID = UUID.fromString(strUUID);		
		idUUID = myGattServer.addChar(MyAdvertiser.GATT_READ, idUUID, controlHandler);
		myGattServer.updateCharValue(idUUID, myIdentifier + "|" + myFriendlyName);
		
		// set up a write characteristic to receive id information
		charIdentifier = "101";
		strUUID = uuidServiceBase.substring(0, 4) + new String(new char[4-charIdentifier.length()]).replace("\0", "0") + charIdentifier + uuidServiceBase.substring(8, uuidServiceBase.length());
		idUUID = UUID.fromString(strUUID);		
		idUUID = myGattServer.addChar(MyAdvertiser.GATT_READWRITE, idUUID, controlHandler);
		myGattServer.updateCharValue(idUUID, "i'm listening");

		// advertising doesn't take much energy, so go ahead and do it
		myGattServer.advertiseNow();
		
	}
	
	public void showFound() {
		// actually return a list of some sort
		myGattClient.scanLeDevice(true);
	}
	
	public void attendMessage(BleMessage message, BleStatusCallback blestatuscallback) {
		// TODO add a way to switch b/w Peripheral and Central modes; for right now we'll start with Central
		
		// need to set some variables here . . .
		myGattClient = new MyCentral(btAdptr, ctx, clientHandler);
		blmsgIn = message;
		messageInbound = true;
		myGattClient.scanLeDevice(true);
		
	}
	
	public void sendMessage(BleMessage message, BleStatusCallback blestatuscallback) {
	
		// TODO add a way to switch b/w Peripheral and Central modes; for right now we'll start with Peripheral
		//myGattServer = new MyAdvertiser(uuidServiceBase, ctx, btAdptr, btMgr, defaultHandler);

		//myGattClient = new MyCentral(btAdptr, ctx, clientHandler);
		
		// add characteristics, to send/receive data as well as control
		UUID indicateData = myGattServer.addChar(MyAdvertiser.GATT_INDICATE, dataHandler);
		UUID notifyData = myGattServer.addChar(MyAdvertiser.GATT_NOTIFY, dataHandler);
		UUID readData = myGattServer.addChar(MyAdvertiser.GATT_READ, dataHandler);
		UUID writeData = myGattServer.addChar(MyAdvertiser.GATT_WRITE, dataHandler);
        
		UUID readwriteControl = myGattServer.addChar(MyAdvertiser.GATT_READWRITE, controlHandler);
        
		// give us a hook to notify the calling Activity
		bleStatusCallback = blestatuscallback;
		
		// start advertising these
		myGattServer.advertiseNow();
		
		myGattClient.scanLeDevice(true);
		
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
    
    private void sendWrite(BluetoothGattCharacteristic writeChar) {
    	
    	// if we've got packets pending send, then send them
    	if (blmsgOut.PendingPacketStatus()) {
    		byte[] nextPacket = blmsgOut.GetPacket().MessageBytes;
    		
    		Log.v(TAG, "send pending packet to " + writeChar.getUuid().toString());
    		myGattClient.submitCharacteristicWriteRequest(writeChar, nextPacket);
    	} else {
    		Log.v(TAG, "all pending packets sent");
    	}

    }

    
    private void getRead(BluetoothGattCharacteristic readChar) {
    	Log.v(TAG, "send pending packet to " + readChar.getUuid().toString());
    	myGattClient.submitCharacteristicReadRequest(readChar);

    }

    public void parseIncomingMsg(BluetoothGattCharacteristic incomingChar, byte[] incomingBytes, boolean initNewRead) {
    	
    	// read in the bytes to build the message - if it needs another read, read in
    	// probably need to build in a failsafe as well
    	
    	boolean readMore = false;
    	
    	Log.v(TAG, "incoming byte (string): " + new String(incomingBytes));
    	
    	readMore = blmsgIn.BuildMessage(incomingBytes);
    	
    	if (readMore) {
    		if (initNewRead) {
    			getRead(incomingChar);
    		}
    	} else {
            String pendingmsg = "";
            
            ArrayList<BlePacket> blms = blmsgIn.GetAllPackets();
            
            for (BlePacket b : blms) {
            	pendingmsg = pendingmsg + new String(b.MessageBytes);
            }
            
            pendingmsg = pendingmsg.trim();
            
            Log.v(TAG, "msg fin: " + pendingmsg);
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
			
			// loop over all the found devices
			// add them 
			for (BluetoothDevice b: devices) {
				Log.v(TAG, "(BleMessenger)MyGattClientHandler found device:" + b.getAddress());
				BlePeer blePeer = new BlePeer(b.getAddress());
				
				//myGattClient.connectAddress(b.getAddress());
			}
			
		}

		public void readCharacteristicReturned(BluetoothGatt gatt, BluetoothGattCharacteristic readChar, byte[] charValue, int status) {
			// a characteristic came in outta the blue - what do i do with it?
			
			
		}
		
		
		
		@Override
		public void getFoundCharacteristics(BluetoothGatt gatt, List<BluetoothGattCharacteristic> foundChars) {
			
			// TODO: what stage is this particular gatt instance in?
			String gattServiceAddress = gatt.getDevice().getAddress();
			String currentStateForService = "id";
			
			String charIdentifier = "100";
			String readIdChar =  uuidServiceBase.substring(0, 4) + new String(new char[4-charIdentifier.length()]).replace("\0", "0") + charIdentifier + uuidServiceBase.substring(8, uuidServiceBase.length());
			
			charIdentifier = "101";
			String writeIdChar =  uuidServiceBase.substring(0, 4) + new String(new char[4-charIdentifier.length()]).replace("\0", "0") + charIdentifier + uuidServiceBase.substring(8, uuidServiceBase.length());
			
			String myIdentifying = myIdentifier + "|" + myFriendlyName;
			
			// if we're in the ID phase
			if (currentStateForService.equalsIgnoreCase("id")) {
				
				for (BluetoothGattCharacteristic c : foundChars) {
					// if this is the read id characteristic, grab the value
					if (c.getUuid().toString().equalsIgnoreCase(readIdChar)) {
						myGattClient.submitCharacteristicReadRequest(c);
					}
					
					// if this is the write id characteristic, write to it
					if (c.getUuid().toString().equalsIgnoreCase(writeIdChar)) {
						myGattClient.submitCharacteristicWriteRequest(c, myIdentifying.getBytes());
					}
				}
			}
			

			
		}
		
		@Override
		public void getFoundCharacteristics(List<BluetoothGattCharacteristic> foundChars) {
			
			// after the characteristics are found, this is where we can check on 'em
			
			// this is called after all the characteristics for our service have been identified
			
			// loop over characteristics - we need to tie this back to our read/write logic somehow
			/*
			for (BluetoothGattCharacteristic c : foundChars) {
				int perms = c.getPermissions();
				int cProps = c.getProperties();
				
				// take a single writable characteristic (in this case, the last one)
				// how do you find the correct characteristic to write to?
	    		if ((cProps & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0 ) {
	    			// this is a write characteristic
	    			w = c;
	    			Log.v("CHARS", "found write characteristic:" + w.getUuid().toString() + " " + cProps);
	    		}
	    		
	    		// and then you wait
		   	    if ((cProps & (BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0) {
			    	 Log.v(TAG, "sub for notifications:" + c.getUuid().toString().substring(0,8));

			    	 if (messageInbound) {
			    		 //myGattClient.subscribeToNotify(c);
			    	 }
				}
		   	    
	    		// and then you wait
		   	    if ((cProps & (BluetoothGattCharacteristic.PROPERTY_READ)) != 0) {
			    	 Log.v(TAG, "found read characteristic:" + c.getUuid().toString().substring(0,8));

			    	 if (messageInbound) {
			    		 r = c;
			    	 }
				}
	    		
		   	    
	    		
			}*/
			
			// this should be the initial method called upon finding the characteristic
			/*
			if (w != null && messageOutbound) {
				sendWrite(w);
			}
			
			if (r != null && messageInbound) {
				Log.v(TAG, "first read");
				getRead(r);
			}
			*/
		}
		
		@Override
		public void getFoundCharacteristic(String serviceUUID,
				BluetoothGattCharacteristic foundChar) {
			
		}

		@Override
		public void getReadCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic readChar, byte[] charValue, int status) {
			String currentStateForService = "id";
			
			if (currentStateForService.equalsIgnoreCase("id")) {
				// hmmm, after the read characteristic is read, and after write characteristic is written, we need to disconnect 
			}
			
			if (currentStateForService.equalsIgnoreCase("data")) {
				
			}
			
			// this part is for data
			//parseIncomingMsg(readChar, charValue, true);
			
		}

		@Override
		public void getNotifyUpdate(String charUUID, byte[] charValue) {
			
		}

		@Override
		public void getWriteResult(BluetoothGattCharacteristic writtenCharacteristic, int result) {
			sendWrite(writtenCharacteristic);
			Log.v(TAG, "write result is:" + String.valueOf(result));
			
		}

		@Override
		public void reportDisconnect() {

			
		}

		@Override
		public void getReadCharacteristic(BluetoothGattCharacteristic readChar,
				byte[] charValue, int status) {
			// TODO Auto-generated method stub
			
		}
    	
    };

}
