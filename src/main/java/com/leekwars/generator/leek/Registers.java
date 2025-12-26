package com.leekwars.generator.leek;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;

public class Registers {

	public final static int MAX_ENTRIES = 100;
	public final static int MAX_KEY_LENGTH = 100;
	public final static int MAX_DATA_LENGTH = 5000;

	private final TreeMap<String, String> mValues;
	private boolean mModified = false;
	private final boolean mNew;

	public Registers(boolean new_register) {
		mValues = new TreeMap<String, String>();
		mNew = new_register;
	}

	public Registers() {
		mValues = new TreeMap<String, String>();
		mNew = false;
	}

	public boolean isNew() {
		return mNew;
	}

	public boolean isModified() {
		return mModified;
	}

	public Map<String, String> getValues() {
		return mValues;
	}

	public boolean set(String key, String value) {
		if (mValues.size() > MAX_ENTRIES)
			return false;
		if (key.length() > MAX_KEY_LENGTH)
			return false;
		if (value.length() > MAX_DATA_LENGTH)
			return false;
		String val = mValues.get(key);
		if (val != null) {
			if (value.equals(val))
				return true;
		}
		mModified = true;
		mValues.put(key, value);
		return true;
	}

	public String get(String key) {
		return mValues.get(key);
	}

	public boolean delete(String key) {
		if (!mValues.containsKey(key))
			return false;
		mValues.remove(key);
		mModified = true;
		return true;
	}

	public String toJSONString() {
		ObjectNode datas = Json.createObject();
		for (Entry<String, String> entry : mValues.entrySet()) {
			datas.put(entry.getKey(), entry.getValue());
		}
		return datas.toString();
	}

	public static Registers fromJSONString(String value) {
		Registers register = new Registers();
		try {
			var datas = Json.parseObject(value);
			for (var entry : datas.properties()) {
				register.mValues.put(entry.getKey(), entry.getValue().asString());
			}
		} catch (Exception e) {
			// Nothing to do, user fault
		}
		return register;
	}
}
