package com.example.blebench;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;

// taken from: https://android.googlesource.com/platform/development/+/master/samples/Vault/src/com/example/android/vault/SecretKeyWrapper.java
public class KeyStuff {

	private final Cipher mCipher;
	private final KeyPair mPair;
	
	public KeyStuff(Context context, String alias) throws GeneralSecurityException, IOException {

		mCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		
		final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
	    keyStore.load(null);
	
	    // if there isn't already a keypair, generate one
	    if (!keyStore.containsAlias(alias)) {
	    	generateKeyPair(context, alias);
	    }
		
	    final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
	      
	    mPair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
	    
	}
	
	
	// generate the keypair and add it 
    private static void generateKeyPair(Context context, String alias)
            throws GeneralSecurityException {
        final Calendar start = new GregorianCalendar();
        final Calendar end = new GregorianCalendar();
        end.add(Calendar.YEAR, 100);
        final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal("CN=" + alias))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
        final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
        gen.initialize(spec);
        gen.generateKeyPair();
    }
    
    public byte[] PublicKey () {
    	return mPair.getPublic().getEncoded();
    }
    
    // Wrap a SecretKey using the public key assigned to this wrapper.
    public byte[] wrap(SecretKey key) throws GeneralSecurityException {
        mCipher.init(Cipher.WRAP_MODE, mPair.getPublic());
        return mCipher.wrap(key);
    }
    
    // Unwrap a SecretKey using the private key assigned to this
    public SecretKey unwrap(byte[] blob) throws GeneralSecurityException {
        mCipher.init(Cipher.UNWRAP_MODE, mPair.getPrivate());
        return (SecretKey) mCipher.unwrap(blob, "AES", Cipher.SECRET_KEY);
    }
	
}
