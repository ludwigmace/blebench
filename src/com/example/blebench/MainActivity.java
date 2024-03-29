package com.example.blebench;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.UUID;

import com.blemsgfw.BleMessage;
import com.blemsgfw.BleMessenger;
import com.blemsgfw.BleMessengerOptions;
import com.blemsgfw.BlePeer;
import com.blemsgfw.BleRecipient;
import com.blemsgfw.BleStatusCallback;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final String TAG = "blebench";

	BleMessenger bleMessenger;
	
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

        // get an identifier for this installation
        String myIdentifier = Installation.id(this);
        
        // get your name (that name part isn't working)
        String userName = getUserName(this.getContentResolver());
        EditText yourNameControl = (EditText) findViewById(R.id.your_name);
        yourNameControl.setText(userName);

        BleMessengerOptions bo = new BleMessengerOptions();
       
        bo.FriendlyName = userName;
        bo.Identifier = myIdentifier;
        
        KeyStuff rsaKey = null;
        
		try {
			rsaKey = new KeyStuff(this, myIdentifier);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		bo.PublicKey = rsaKey.PublicKey();
		Log.v(TAG, "pubkey size in bytes:" + String.valueOf(bo.PublicKey.length));
       
        
		// create a messenger along with the context (for bluetooth operations)
		bleMessenger = new BleMessenger(bo, btMgr, btAdptr, this);
        
		// generate message of particular byte size
		byte[] bytesMessage = benchGenerateMessage(45);

		/*
		// declare a new message
		BleMessage blm = new BleMessage();
		
		// add a recipient
		BleRecipient br1 = new BleRecipient("macon_schoonmaker");
		blm.AddRecipient(br1);
		
		// set the message
		blm.setMessage(bytesMessage);

		 */		
		// this should queue the message up to send
		//blmgr.sendMessage(blm, bleMessageStatus);
		
		//blmgr.attendMessage(blmIn, bleMessageStatus);
		
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
		public void handleReceivedMessage(String recipientFingerprint, String senderFingerprint, byte[] payload, String msgType) {
			// check our array for friends with the senderFingerprint
			// if we don't, then parse the public key & friendly name out of the payload
			
		}
		
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

		@Override
		public void remoteServerAdded(String serverName) {
			showMessage(serverName);
		}

		@Override
		public void foundPeer(BlePeer blePeer) {
			final String peerName = blePeer.GetName(); 
			
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                	showMessage("peer found:" + peerName);
                }
            });
			
		}
		
		
	};
	
	public void handleButtonFindAFriend(View view) {
		bleMessenger.showFound(bleMessageStatus);
	}
	
	private void showMessage(String msg) {

		final String message = msg;
		final Context fctx = this;
		
		runOnUiThread(new Runnable() {
			  public void run() {
				  Toast.makeText(fctx, message, Toast.LENGTH_LONG).show();
			  }
			});
		
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
	
	private String getUserName(ContentResolver cr) {
        
        String displayName = "";
         
        Cursor c = cr.query(ContactsContract.Profile.CONTENT_URI, null, null, null, null); 
         
        try {
            if (c.moveToFirst()) {
                displayName = c.getString(c.getColumnIndex("display_name"));
            }  else {
            	displayName = "nexus5";
            	Log.v(TAG, "can't get user name; no error");	
            }
            
        } catch (Exception x) {
        	Log.v(TAG, "can't get user name; error");
        	
		} finally {
            c.close();
        }
        
        return displayName;
	}
	
}
