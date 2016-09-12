package com.example.petriyov.geolocator;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

public class  LoginActivity extends Activity implements View.OnClickListener {

	private BroadcastReceiver locatorRecevier = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getAction()) {
				case LocatorService.ACTION_CONNECTION_ERROR:
					break;
				case LocatorService.ACTION_LAT_LNG_RECEIVED:
					break;
				default:
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		findViewById(R.id.connect).setOnClickListener(this);
		findViewById(R.id.disconnect).setOnClickListener(this);
		findViewById(R.id.send).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		Intent intent;
		switch (view.getId()) {
			case R.id.connect:
				intent = new Intent(this, LocatorService.class);
				intent.setAction(LocatorService.ACTION_CONNECT);
				startService(intent);
				break;
			case R.id.disconnect:
				intent = new Intent(this, LocatorService.class);
				intent.setAction(LocatorService.ACTION_DISCONNECT);
				startService(intent);
				break;
			case R.id.send:
				intent = new Intent(this, LocatorService.class);
				intent.setAction(LocatorService.ACTION_SEND_LOCATION);
				intent.putExtra(LocatorService.EXTRA_LAT_LNG, new LatLng(55.373703, 37.474764));
				startService(intent);
				break;
			default:
				break;
		}
	}
}
