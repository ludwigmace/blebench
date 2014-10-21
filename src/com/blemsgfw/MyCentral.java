package com.blemsgfw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class MyCentral {
	
	private static final String TAG = "BLECC";
	
	private boolean bFound;
	
	private Context ctx;
	private Activity myActivity;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private static String strSvcUuidBase = "73A20000-2C47-11E4-8C21-0800200C9A66";
    
    
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BluetoothGattCharacteristic clientReadCharacteristic;
    
    private boolean bScan;
    
    private int mConnectionState = STATE_DISCONNECTED;
    
    
	// scanning happens asynchronously, so get a link to a Handler
    private Handler mHandler;
    
    private boolean mScanning;
    private ArrayList<BluetoothDevice> foundDevices;
    
    private BluetoothAdapter centralBTA;
    private BluetoothGatt mBluetoothGatt;
    
    private MyGattClientHandler gattClientHandler;
    
    // scan for 2 1/2 seconds at a time
    private static final long SCAN_PERIOD = 2500;
    
    MyCentral(BluetoothAdapter btA, Context ctx, MyGattClientHandler myHandler) {
    	centralBTA = btA;
    	
    	this.ctx = ctx;
    	//myActivity = act;
    	
        // to be used for scanning for LE devices
        mHandler = new Handler();
        
        foundDevices = new ArrayList<BluetoothDevice>(); 
        
        mScanning = false;
        
        gattClientHandler = myHandler;
        
    }

    public void initConnect(BluetoothDevice b) {
    	mBluetoothGatt = b.connectGatt(ctx, false, mGattCallback);
    }
    
    public void connectAddress(String btAddress){
    	BluetoothDevice b = centralBTA.getRemoteDevice(btAddress);
    	mBluetoothGatt = b.connectGatt(ctx, false, mGattCallback);
    }
    
    public void getRSSI() {
    	mBluetoothGatt.readRemoteRssi();
    	Log.v("BLECC", "trying to read remote RSSI");
    }
    
    public boolean subscribeToNotify(UUID uuid) {
    	
    	boolean result = false;
    	
    	//List<BluetoothGattService> foundServices = mBluetoothGatt.getServices();
    	BluetoothGattService s = mBluetoothGatt.getService(UUID.fromString(strSvcUuidBase));
    	BluetoothGattCharacteristic c = null;
    	
    	// check the services
		Log.v(TAG, "try to pull characteristic via uuid:" + uuid.toString());
		try {
			c = s.getCharacteristic(uuid);
		} catch (Exception e) {
			c = null;
			Log.v(TAG, "error while trying to pull characteristic by UUID from service:" + s.getUuid().toString());
		}
    	
    	if (c != null) {
        	int cProps = c.getProperties();
        	
	   	     if ((cProps & (BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0) {
		    	 Log.v(TAG, "sub for notifications from " + c.getUuid().toString().substring(0,8));
			
				// enable notifications for this guy
				mBluetoothGatt.setCharacteristicNotification(c, true);
				
				// tell the other guy that we want characteristics enabled
				BluetoothGattDescriptor descriptor = c.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
				
				// if it's a notification value, subscribe by setting the descriptor to ENABLE_NOTIFICATION_VALUE
				if ((cProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				}

				// if it's an INDICATION value, subscribe by setting the descriptor to ENABLE_INDICATION_VALUE				
				if ((cProps & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
				}
				
				mBluetoothGatt.writeDescriptor(descriptor);
				
				result = true;
			
			}
    	} else {
    		Log.v(TAG, "can't pull characteristic so i can't sub it");
    	}
    	    	
		return result;

    }
    
    public boolean isScanning() {
    	return mScanning;
    }
    
    public boolean submitCharacteristicWriteRequest(BluetoothGattCharacteristic writeChar, byte[] val) {
		
    	boolean charWrote = false;
    	Log.v(TAG, "characteristic found by char, issuing write request " + writeChar.getUuid().toString());
		
    	try {
    		writeChar.setValue(val);
    		writeChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		
    		mBluetoothGatt.writeCharacteristic(writeChar);
    		charWrote = true;
    	} catch (Exception e) {
    		Log.v(TAG, "cannot write char ");
    	}
		
		return charWrote;
    }
    
    public boolean submitCharacteristicWriteRequest(UUID uuid, byte[] val) {
    	boolean charWrote = false;
    	
    	// this was already populated when you connected, so might as well re-use it
    	// does this stay populated?
    	// List<BluetoothGattService> foundServices = mBluetoothGatt.getServices();
    	Log.v(TAG, "write request submitted for " + uuid.toString());
    	BluetoothGattCharacteristic c = null;
    	
    	BluetoothGattService s = mBluetoothGatt.getService(UUID.fromString(strSvcUuidBase));

		try {
			Log.v(TAG, "try to pull characteristic via uuid:" + uuid.toString());
			c = s.getCharacteristic(uuid);
		} catch (Exception e) {
			c = null;
			Log.v(TAG, "error while trying to pull characteristic by UUID from service:" + s.getUuid().toString());
		}
    	
    	
    	if (c != null) {
    		Log.v(TAG, "characteristic found, issuing write request");
    	 	c.setValue(val);
        	c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
    		
    		mBluetoothGatt.writeCharacteristic(c);
    		charWrote = true;
    	}
    	
    	
    	return charWrote;
    }
    
    public boolean submitCharacteristicWriteRequest(UUID uuid, String val) {
    	return submitCharacteristicWriteRequest(uuid, val.getBytes());
    	
    }
    
    public boolean submitCharacteristicReadRequest(UUID uuid) {
    	// locate the characteristic via the string representation of its UUID, then "read" its value
    	
    	boolean charFound = false;
    	
    	// this was already populated when you connected, so might as well re-use it
    	// does this stay populated?
    	//List<BluetoothGattService> foundServices = mBluetoothGatt.getServices();
    	BluetoothGattService s = mBluetoothGatt.getService(UUID.fromString(strSvcUuidBase));
    	BluetoothGattCharacteristic c = null;
    	
		try {
			Log.v(TAG, "try to pull characteristic via uuid:" + uuid.toString());
			c = s.getCharacteristic(uuid);
		} catch (Exception e) {
			c = null;
			Log.v(TAG, "error while trying to pull characteristic by UUID from service:" + s.getUuid().toString());
		}
    	
    	if (c != null) {
    		Log.v(TAG, "characteristic found, issuing read request");
    		mBluetoothGatt.readCharacteristic(c);
    		charFound = true;
    	}
    	
    	return charFound;
    	
    }
    
    
 // when a device is found from the ScanLeDevice method, call this
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        
    	@Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
    		//Log.v(TAG, "LeScanCallback sez:" + device.getAddress());
    		if (!foundDevices.contains(device)) {
    			foundDevices.add(device);
        	}
    		
       }
    };
    
    public ArrayList<BluetoothDevice> getAdvertisers() {
    
    	return foundDevices;
    }
    
    private void connectDevices() {
    	
    	BluetoothDevice b = foundDevices.get(0);
    	b.connectGatt(ctx, false, mGattCallback);
    }
    
    public void scanLeDevice(final boolean enable, UUID[] serviceUuids) {
        if (enable) {

        	// call STOP after SCAN_PERIOD ms, which will spawn a thread to stop the scan
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    centralBTA.stopLeScan(mLeScanCallback);
                    connectDevices();
        			//gattClientHandler.getFoundDevices(foundDevices);
        			//Log.v(TAG, "scan stopped, found " + String.valueOf(foundDevices.size()) + " devices");
                }
            }, SCAN_PERIOD);

            // start scanning!
            mScanning = true;
            
            // will only scan for "normal" UUIDs
            centralBTA.startLeScan(mLeScanCallback);
            //centralBTA.startLeScan(serviceUuids, mLeScanCallback);

        } else {
        	
        	// the "enable" variable passed wa False, so turn scanning off
            mScanning = false;
            centralBTA.stopLeScan(mLeScanCallback);
            
        }

    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    	@Override
    	// Characteristic notification
    	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    	    //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
    		gattClientHandler.getNotifyUpdate(characteristic.getUuid().toString(), characteristic.getValue());
    		
    		
    		Log.v(TAG, "characteristic changed val is " + new String(characteristic.getValue()));
    	}
    	
    	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    		//defaultHandler.DoStuff(gatt.getDevice().getAddress() + " - " + String.valueOf(rssi));
    	}
    	
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                
                Log.v(TAG, "Connected to GATT server.");
                gatt.discoverServices();
                //Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                gattClientHandler.reportDisconnect();
                Log.i(TAG, "Disconnected from GATT server.");
                
            }
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        	gattClientHandler.getWriteResult(characteristic.getUuid().toString(), status);
          Log.v(TAG, "write submitted val:" + new String(characteristic.getValue()) + " - result:" + String.valueOf(status));
          
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            	Log.v("SERVICES", "services discovered");
            	
            	// now that services have been discovered, let's pull them down
            	List<BluetoothGattService> foundServices = gatt.getServices();
            	
            	Log.v(TAG, "found " + String.valueOf(foundServices.size()) + " service(s)");
            	
            	/*
            	for (BluetoothGattService s : foundServices) {
            		Log.v("SERVICES", "services found:" + s.getUuid().toString());
            	}
            	*/
            	// we're pulling a specific service
            	BluetoothGattService s = gatt.getService(UUID.fromString(strSvcUuidBase));

            	if (s != null) {
            		if (s.getCharacteristics() != null) {
            			gattClientHandler.getFoundCharacteristics(s.getCharacteristics());
            		} else {
            			Log.v(TAG, "can't find characteristics");	
            		}
            	} else {
            		Log.v(TAG, "can't find service " + strSvcUuidBase);
            	}
		        
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	
            	gattClientHandler.getReadCharacteristic(characteristic.getUuid().toString(), characteristic.getValue());
            	
                Log.v(TAG, "+read " + characteristic.getUuid().toString() + ": " + new String((characteristic.getValue())));
            } else {
            	Log.v(TAG, "-fail read " + characteristic.getUuid().toString());
            }
        }
    };
    
}
