package com.example.blebench;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import com.blemsgfw.BleMessage;
import com.blemsgfw.BleMessenger;
import com.blemsgfw.BleRecipient;
import com.blemsgfw.BleStatusCallback;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// because this is using BLE, we'll need to get the adapter and manager from the main context and thread 
		BluetoothManager btMgr = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdptr = btMgr.getAdapter();
        
        // check to see if the bluetooth adapter is enabled
        if (!btAdptr.isEnabled()) {
        	Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	startActivityForResult(enableBtIntent, RESULT_OK);
        }
		
		// generate message of particular byte size
		byte[] bytesMessage = benchGenerateMessage(992);

		// declare a new message
		BleMessage blm = new BleMessage();
		
		
		// add a recipient
		BleRecipient br1 = new BleRecipient("macon_schoonmaker");
		blm.AddRecipient(br1);
		
		// set the message
		blm.setMessage(bytesMessage);
		
		// create a messenger and add the message to send, along with the context (for bluetooth operations)
		BleMessenger blmgr = new BleMessenger(btMgr, btAdptr, this);
		
		// this should queue the message up to send
		blmgr.sendMessage(blm, bleMessageStatus);
		
		/*
		BleMessager blm = new BleMessager();
		BleMessagerOptions blmOpts = new BleMessagerOptions();
		BleMessagerRecipients blmRecipients = new BleMessagerRecipients();
		BleMessage blmMessage = new BleMessagerMessage();

		blmRecipients.AddRecipient(recipient1, encryptkey1);

		blmMessage.Set(messageData);

		blmOpts(keyBoolEncrypt, true);
		blmOpts(keyMinRecipientsBeforeConsideredSent, 1);
		blmOpts(keyTimeout, 600);

		blm.SendWhenever(blmRecipients, blmMessage, blmOpts, CallBackWhenSent);
		 */
		
	}

	BleStatusCallback bleMessageStatus = new BleStatusCallback() {

		@Override
		public void messageSent(UUID uuid) {
			
			final String sUUID = uuid.toString(); 
			
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                	showMessage("message sent:" + sUUID);
                }
            });
		}
		
		
	};
	
	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	private byte[] benchGenerateMessage(int MessageSize) {
		// get the lorem text from file
		byte[] bytesLorem = null;
		byte[] bytesMessage = null;
		InputStream is = getResources().openRawResource(R.raw.lorem);
    			
		int currentMessageLength = 0;
		int maxcount = 0;
		
		while ((currentMessageLength < MessageSize) && maxcount < 1000) {
			maxcount++;
	    	try {
	    		if (currentMessageLength == 0) {
	    			bytesMessage = ByteStreams.toByteArray(is);
	    		}
	    		is.reset();
	    		bytesLorem = ByteStreams.toByteArray(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	
	    	try {
				is.reset();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	bytesMessage = Bytes.concat(bytesMessage, bytesLorem);
	    	
	    	currentMessageLength = bytesMessage.length;
    	
		}
		
		return Arrays.copyOf(bytesMessage, MessageSize);
	}
}
