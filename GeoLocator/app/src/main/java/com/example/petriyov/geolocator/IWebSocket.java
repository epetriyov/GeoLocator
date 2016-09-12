package com.example.petriyov.geolocator;

public interface IWebSocket {

	void sendMessage(String message);

	void connect();

	void disconnect();

	void finishSocket();
}
