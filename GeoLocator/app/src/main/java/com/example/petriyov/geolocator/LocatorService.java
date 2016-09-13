package com.example.petriyov.geolocator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.maps.model.LatLng;

public class LocatorService extends Service implements WebSocketCallback {

	public static final String EXTRA_LAT_LNG = "extra_lat_lng";
	public static final String ACTION_LAT_LNG_RECEIVED = "action_lat_lng_received";
	public static final String ACTION_CONNECTION_ERROR = "action_connection_error";
	public static final String ACTION_CONNECT = "action_connect";
	public static final String ACTION_DISCONNECT = "action_disconnect";
	public static final String ACTION_SEND_LOCATION = "action_send_location";
	private final static String SERVICE_URL = "ws://mini-mdt.wheely.com/?username=a&password=a";
	private IWebSocket webSocket;
	private LocalBroadcastManager localBroadcastManager;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		switch (intent.getAction()) {
			case ACTION_CONNECT:
				webSocket.connect();
				break;
			case ACTION_DISCONNECT:
				webSocket.disconnect();
				break;
			case ACTION_SEND_LOCATION:
				LatLng latLng = intent.getParcelableExtra(EXTRA_LAT_LNG);
				webSocket.sendMessage(GeoUtils.convertLatLngToRequest(latLng));
				break;
			default:
				break;
		}

		return START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		webSocket = new WebSocket(SERVICE_URL, this);
	}

	@Override
	public void onDisconnectFinished() {
		stopSelf();
	}

	@Override
	public void onConnectionError() {
		localBroadcastManager.sendBroadcast(new Intent(ACTION_CONNECTION_ERROR));
	}

	@Override
	public void onMessageRecieved(String message) {
		Intent intent = new Intent(ACTION_LAT_LNG_RECEIVED);
		intent.putParcelableArrayListExtra(EXTRA_LAT_LNG, GeoUtils.convertResponseToLatLng(message));
		localBroadcastManager.sendBroadcast(intent);
	}
}
