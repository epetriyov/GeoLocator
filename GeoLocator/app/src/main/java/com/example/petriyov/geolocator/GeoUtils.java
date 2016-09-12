package com.example.petriyov.geolocator;

import android.util.JsonReader;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public final class GeoUtils {

	private GeoUtils() {

	}


	public static String convertLatLngToRequest(LatLng latLng) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("lat", latLng.latitude);
			jsonObject.put("lon", latLng.longitude);
			return jsonObject.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ArrayList<LatLng> convertResponseToLatLng(String serverResponse) {
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
}
