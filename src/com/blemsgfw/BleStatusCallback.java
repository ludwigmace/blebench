package com.blemsgfw;

import java.util.UUID;

public interface BleStatusCallback {

	public void messageSent (UUID uuid);
	
	public void remoteServerAdded(String serverName);
	
}
