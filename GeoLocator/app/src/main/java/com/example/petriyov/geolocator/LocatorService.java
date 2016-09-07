package com.example.petriyov.geolocator;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.JsonReader;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocatorService extends Service {

	public static final String EXTRA_LAT_LNG = "extra_lat_lng";
	public static final String ACTION_LAT_LNG_RECEIVED = "action_lat_lng_received";
	public static final String ACTION_CONNECTION_ERROR = "action_connection_error";
	public static final String ACTION_CONNECT = "action_connect";
	public static final String ACTION_DISCONNECT = "action_disconnect";
	public static final String ACTION_SEND_LOCATION = "action_send_location";
	private final static String SERVICE_URL = "ws://min-mdt.wheely.com?username=a&password=a";
	private static final String TAG = LocatorService.class.getSimpleName();
	private volatile boolean wasDisconnected;
	private volatile Socket socket;
	private LocalBroadcastManager localBroadcastManager;
	private ExecutorService executorService;
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	private static ArrayList<LatLng> convertResponseToLatLng(String serverResponse) {
		ArrayList<LatLng> result = new ArrayList<>();
		JsonReader jsonReader = new JsonReader(new BufferedReader(new StringReader(serverResponse)));
		try {
			jsonReader.beginArray();
			while (jsonReader.hasNext()) {
				jsonReader.beginObject();
				jsonReader.nextInt();
				result.add(new LatLng(jsonReader.nextDouble(), jsonReader.nextDouble()));
				jsonReader.endObject();
			}
			jsonReader.endArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				jsonReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private static String convertLatLngToRequest(LatLng latLng) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("lat", latLng.latitude);
			jsonObject.put("long", latLng.longitude);
			return jsonObject.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		switch (intent.getAction()) {
			case ACTION_CONNECT:
				if (socket == null) {
					executorService.execute(new ConnectRunnable());
				}
				break;
			case ACTION_DISCONNECT:
				if (socket != null) {
					executorService.execute(new CloseRunnable());
				}
				break;
			case ACTION_SEND_LOCATION:
				if (socket != null) {
					LatLng latLng = intent.getParcelableExtra(EXTRA_LAT_LNG);
					if (latLng != null) {
						Message msg = mServiceHandler.obtainMessage();
						msg.arg1 = startId;
						msg.obj = latLng;
						mServiceHandler.sendMessage(msg);
					}
				}
				break;
			default:
				break;
		}

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		mServiceLooper.quit();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		executorService = Executors.newCachedThreadPool();
		HandlerThread thread = new HandlerThread(LocatorService.class.getSimpleName());
		thread.start();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	private Intent buildGeoIntent(ArrayList<LatLng> latLng) {
		Intent intent = new Intent(ACTION_LAT_LNG_RECEIVED);
		intent.putParcelableArrayListExtra(EXTRA_LAT_LNG, latLng);
		return intent;
	}

	private void sendConnectionErrorMessage() {
		localBroadcastManager.sendBroadcast(new Intent(ACTION_CONNECTION_ERROR));
	}

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			String message = convertLatLngToRequest((LatLng) msg.obj);
			if (message != null) {
				BufferedWriter bufferedWriter = null;
				try {
					bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					bufferedWriter.write(message);
				} catch (IOException e) {
					e.printStackTrace();
					sendConnectionErrorMessage();
				} finally {
					if (bufferedWriter != null) {
						try {
							bufferedWriter.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			stopSelf(msg.arg1);
		}
	}

	private class CloseRunnable implements Runnable {
		@Override
		public void run() {
			if (socket != null) {
				try {
					wasDisconnected = true;
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			stopSelf();
		}
	}

	private class ConnectRunnable implements Runnable {

		@Override
		public void run() {
			connection();
			stopSelf();
		}

		private void connectSocket() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String str;
				try {
					while (!socket.isClosed()) {
						sb.setLength(0);
						while ((str = in.readLine()) != null) {
							sb.append(str);
						}
						if (sb.length() > 0) {
							Log.d(TAG, "Received: " + sb.toString());
							localBroadcastManager.sendBroadcast(buildGeoIntent(convertResponseToLatLng(sb.toString())));
						}
					}
					connection();
				} catch (IOException e) {
					e.printStackTrace();
					connection();
				}
			} catch (IOException e) {
				e.printStackTrace();
				sendConnectionErrorMessage();
			}
		}


		private void connection() {
			if (!wasDisconnected) {
				try {
					URI uri = URI.create(SERVICE_URL);
					socket = new Socket(uri.getHost(), uri.getPort());
					connectSocket();
				} catch (IOException e) {
					e.printStackTrace();
					sendConnectionErrorMessage();
				}
			}
		}
	}

}
