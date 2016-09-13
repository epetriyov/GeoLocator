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
	private static final String TAG = WebSocket.class.getSimpleName();
	private final WebSocketCallback webSocketCallback;
	private final ExecutorService executorService;
	private final String url;
	private volatile boolean wasDisconnected;
	private volatile Socket socket;
	private Writer writer;

	public WebSocket(String url, WebSocketCallback webSocketCallback) {
		this.url = url;
		this.webSocketCallback = webSocketCallback;
		executorService = Executors.newCachedThreadPool();
	}

	@Override
	public void connect() {
		if (socket == null) {
			executorService.execute(new Runnable() {
				@Override
				public void run() {
					connectSocket();
				}
			});
		}
	}

	@Override
	public void disconnect() {
		if (socket != null) {
			executorService.execute(new Runnable() {
				@Override
				public void run() {
					if (socket != null) {
						try {
							wasDisconnected = true;
							Log.d(TAG, "Start close socket");
							socket.close();
							writer.finishHandler();
							webSocketCallback.onDisconnectFinished();
							Log.d(TAG, "Socket closed");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
	}

	@Override
	public void sendMessage(String message) {
		if (!TextUtils.isEmpty(message) && socket != null) {
			writer.writeMessage(message);
		}
	}

	private void startRead() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String str;
			try {
				Log.d(TAG, "Start read from socket");
				while (!socket.isClosed()) {
					sb.setLength(0);
					while ((str = in.readLine()) != null) {
						Log.d(TAG, "Read from socket: " + str);
						sb.append(str);
					}
					if (sb.length() > 0) {
						Log.d(TAG, "Received: " + sb.toString());
						webSocketCallback.onMessageRecieved(sb.toString());
					}
				}
				Log.d(TAG, "Socket was closed");
				connectSocket();
			} catch (IOException e) {
				e.printStackTrace();
				connectSocket();
			}
		} catch (IOException e) {
			e.printStackTrace();
			webSocketCallback.onConnectionError();
		}
	}

	private void connectSocket() {
		if (!wasDisconnected) {
			try {
				Log.d(TAG, "Start connection to socket");
				URI uri = URI.create(url);
				socket = new Socket(uri.getHost(), 80);
				Log.d(TAG, "Success connection to socket");
				startWrite();
				startRead();
			} catch (IOException e) {
				e.printStackTrace();
				webSocketCallback.onConnectionError();
			}
		}
	}

	private void startWrite() throws IOException {
		if (writer == null) {
			HandlerThread handlerThread = new HandlerThread(TAG);
			handlerThread.start();
			writer = new Writer(handlerThread.getLooper());
		} else {
			writer.updateSocket();
		}
	}

	private class Writer extends Handler {
		private final Looper writeLooper;
		private BufferedWriter bufferedWriter;

		public Writer(Looper looper) throws IOException {
			super(looper);
			writeLooper = looper;
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		}

		public void writeMessage(String message) {
			Message msg = obtainMessage();
			msg.obj = message;
			sendMessage(msg);
		}

		@Override
		public void handleMessage(Message msg) {
			try {
				Log.d(TAG, "Start writing to socket");
				synchronized (bufferedWriter) {
					bufferedWriter.write((String) msg.obj);
					bufferedWriter.flush();
				}
				Log.d(TAG, "Success write to socket");
			} catch (IOException e) {
				e.printStackTrace();
				webSocketCallback.onConnectionError();
			}
		}

		public void finishHandler() {
			writeLooper.quit();
		}

		public void updateSocket() throws IOException {
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		}
	}
}
