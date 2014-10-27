package com.blemsgfw;

public class BlePeer {

	private String peerAddress;
	private byte[] encryptKey;

	public BlePeer(String PeerAddress) {
		peerAddress = PeerAddress;
	}
	
	public String RecipientName() {
		return peerAddress;
	}
	
	public byte[] RecipientKey() {
		return encryptKey;
	}
	
}
