package com.example.petriyov.geolocator;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocket implements IWebSocket {
	private static final String TAG = LocatorService.class.getSimpleName();
	private final WebSocketCallback webSocketCallback;
	private final ExecutorService executorService;
	private final Looper mServiceLooper;
	private final ServiceHandler mServiceHandler;
	private final String url;
	private volatile boolean wasDisconnected;
	private volatile Socket socket;

	public WebSocket(String url, WebSocketCallback webSocketCallback) {
		this.url = url;
		this.webSocketCallback = webSocketCallback;
		executorService = Executors.newCachedThreadPool();
		HandlerThread thread = new HandlerThread(LocatorService.class.getSimpleName());
		thread.start();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public void finishSocket() {
		mServiceLooper.quit();
	}

	@Override
	public void connect() {
		if (socket == null) {
			executorService.execute(new ConnectRunnable());
		}
	}

	@Override
	public void disconnect() {
		if (socket != null) {
			executorService.execute(new CloseRunnable());
		}
	}

	@Override
	public void sendMessage(String message) {
		if (!TextUtils.isEmpty(message) && socket != null) {
			Message msg = mServiceHandler.obtainMessage();
			msg.obj = message;
			mServiceHandler.sendMessage(msg);
		}
	}

	private final class ServiceHandler extends Handler {

		private BufferedWriter bufferedWriter;

		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.obj != null) {
				try {
					Log.d(TAG, "Start writing to socket");
					if (bufferedWriter == null) {
						bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					}
					bufferedWriter.write((String) msg.obj);
					bufferedWriter.newLine();
					bufferedWriter.flush();
					Log.d(TAG, "Success write to socket");
				} catch (IOException e) {
					e.printStackTrace();
					webSocketCallback.onConnectionError();
				}
			}
		}
	}

	private class CloseRunnable implements Runnable {
		@Override
		public void run() {
			if (socket != null) {
				try {
					wasDisconnected = true;
					Log.d(TAG, "Start close socket");
					socket.close();
					Log.d(TAG, "Socket closed");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	private class ConnectRunnable implements Runnable {

		@Override
		public void run() {
			connection();

		}

		private void connectSocket() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String str;
				try {
					Log.d(TAG, "Start read from socket");
					while (!socket.isClosed()) {
						sb.setLength(0);
						if (in.ready()) {
							while ((str = in.readLine()) != null) {
								Log.d(TAG, "Read from socket: " + str);
								sb.append(str);
							}
							if (sb.length() > 0) {
								Log.d(TAG, "Received: " + sb.toString());
								webSocketCallback.onMessageRecieved(sb.toString());
							}
						}
					}
					Log.d(TAG, "Socket was closed");
					connection();
				} catch (IOException e) {
					e.printStackTrace();
					connection();
				}
			} catch (IOException e) {
				e.printStackTrace();
				webSocketCallback.onConnectionError();
			}
		}


		private void connection() {
			if (!wasDisconnected) {
				try {
					Log.d(TAG, "Start connection to socket");
					URI uri = URI.create(url);
					socket = new Socket(uri.getHost(), 80);
					Log.d(TAG, "Success connection to socket");
					connectSocket();
				} catch (IOException e) {
					e.printStackTrace();
					webSocketCallback.onConnectionError();
				}
			}
		}
	}
}
