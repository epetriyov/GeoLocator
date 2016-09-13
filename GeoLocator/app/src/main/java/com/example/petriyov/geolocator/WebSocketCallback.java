package com.example.petriyov.geolocator;

public interface WebSocketCallback {

	void onDisconnectFinished();

	void onConnectionError();

	void onMessageRecieved(String message);
}
