package com.loopj.android.http;

import android.util.Log;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONObjectProxy extends JSONObject {

	private JSONObject jsonObject;

	public JSONObjectProxy(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	/**
	 * 
	 */
	public JSONObjectProxy() {
		// TODO Auto-generated constructor stub
		jsonObject = new JSONObject();
	}

	public JSONObject accumulate(String name, Object value)
			throws JSONException {
		return jsonObject.accumulate(name, value);
	}

	public boolean equals(Object o) {
		return jsonObject.equals(o);
	}

	public Object get(String name) throws JSONException {
		return jsonObject.get(name);
	}

	public boolean getBoolean(String name) throws JSONException {
		return jsonObject.getBoolean(name);
	}

	public Boolean getBooleanOrNull(String name) {
		try {
			return Boolean.valueOf(jsonObject.getBoolean(name));
		} catch (JSONException e) {
			Log.v(JSONObjectProxy.class.getName(), e.getMessage());
			return false;
		}
	}

	public double getDouble(String name) throws JSONException {
		return jsonObject.getDouble(name);
	}

	public int getInt(String name) throws JSONException {
		return jsonObject.getInt(name);
	}

	public Integer getIntOrNull(String name) {
		try {
			return Integer.valueOf(jsonObject.getInt(name));
		} catch (JSONException e) {
			Log.v(JSONObjectProxy.class.getName(), e.getMessage());
			return null;
		}
	}

	public JSONArrayPoxy getJSONArray(String name) throws JSONException {
		return new JSONArrayPoxy(jsonObject.getJSONArray(name));
	}

	public JSONArrayPoxy getJSONArrayOrNull(String name) {
		try {
			return new JSONArrayPoxy(jsonObject.getJSONArray(name));
		} catch (JSONException e) {
			return null;
		}
	}

	public JSONObjectProxy getJSONObject(String name) throws JSONException {
		return new JSONObjectProxy(jsonObject.getJSONObject(name));
	}

	public JSONObjectProxy getJSONObjectOrNull(String name) {
		try {
			return new JSONObjectProxy(jsonObject.getJSONObject(name));
		} catch (JSONException e) {
			return null;
		}
	}

	public long getLong(String name) throws JSONException {
		return jsonObject.getLong(name);
	}

	public Long getLongOrNull(String name) {
		try {
			return Long.valueOf(jsonObject.getLong(name));
		} catch (JSONException e) {
		    Log.v(JSONObjectProxy.class.getName(), e.getMessage());
			return null;
		}
	}

	public String getString(String name) throws JSONException {
		return jsonObject.getString(name);
	}

	public String getStringOrNull(String name) {
		try {
			String value = jsonObject.getString(name);
			if ("null".equals(value)) {
				return null;
			}
			return value;
		} catch (JSONException e) {
		    Log.v(JSONObjectProxy.class.getName(), e.getMessage());
			return null;
		}
	}

	public boolean has(String name) {
		return jsonObject.has(name);
	}

	public int hashCode() {
		return jsonObject.hashCode();
	}

	public boolean isNull(String name) {
		return jsonObject.isNull(name);
	}

	public Iterator keys() {
		return jsonObject.keys();
	}

	public int length() {
		return jsonObject.length();
	}

	public JSONArray names() {
		return jsonObject.names();
	}

	public Object opt(String name) {
		return jsonObject.opt(name);
	}

	public boolean optBoolean(String name, boolean fallback) {
		return jsonObject.optBoolean(name, fallback);
	}

	public boolean optBoolean(String name) {
		return jsonObject.optBoolean(name);
	}

	public double optDouble(String name, double fallback) {
		return jsonObject.optDouble(name, fallback);
	}

	public double optDouble(String name) {
		return jsonObject.optDouble(name);
	}

	public int optInt(String name, int fallback) {
		return jsonObject.optInt(name, fallback);
	}

	public int optInt(String name) {
		return jsonObject.optInt(name);
	}

	public JSONArray optJSONArray(String name) {
		return jsonObject.optJSONArray(name);
	}

	public JSONObject optJSONObject(String name) {
		return jsonObject.optJSONObject(name);
	}

	public long optLong(String name, long fallback) {
		return jsonObject.optLong(name, fallback);
	}

	public long optLong(String name) {
		return jsonObject.optLong(name);
	}

	public String optString(String name, String fallback) {
		return jsonObject.optString(name, fallback);
	}

	public String optString(String name) {
		return jsonObject.optString(name);
	}

	public JSONObject put(String name, boolean value) throws JSONException {
		return jsonObject.put(name, value);
	}

	public JSONObject put(String name, double value) throws JSONException {
		return jsonObject.put(name, value);
	}

	public JSONObject put(String name, int value) throws JSONException {
		return jsonObject.put(name, value);
	}

	public JSONObject put(String name, long value) throws JSONException {
		return jsonObject.put(name, value);
	}

	public JSONObject put(String name, Object value) throws JSONException {
		return jsonObject.put(name, value);
	}

	public JSONObject putOpt(String name, Object value) throws JSONException {
		return jsonObject.putOpt(name, value);
	}

	public Object remove(String name) {
		return jsonObject.remove(name);
	}

	public JSONArray toJSONArray(JSONArray names) throws JSONException {
		return jsonObject.toJSONArray(names);
	}

	public String toString() {
		return jsonObject.toString();
	}

	public String toString(int indentSpaces) throws JSONException {
		return jsonObject.toString(indentSpaces);
	}

}
