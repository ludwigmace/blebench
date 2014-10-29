package com.blemsgfw;

public class BlePeer {

	private String peerAddress;
	private String peerName;

	public BlePeer(String PeerAddress) {
		peerAddress = PeerAddress;
		peerName="";
	}
	
	public String RecipientAddress() {
		return peerAddress;
	}
	
	public void SetName(String PeerName) {
		peerName = PeerName;
	}
	
	public String GetName() {
		return peerName;
	}
	
}
