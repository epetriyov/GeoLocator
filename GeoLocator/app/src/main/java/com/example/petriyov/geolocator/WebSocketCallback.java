package com.example.petriyov.geolocator;

public interface WebSocketCallback {


	void onConnectionError();

	void onMessageRecieved(String message);
}
