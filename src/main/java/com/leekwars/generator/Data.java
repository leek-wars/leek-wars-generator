package com.leekwars.generator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class Data {

	private static final String TAG = Data.class.getSimpleName();

	public static void checkData(String api) {
		File weaponsFile = new File("data/weapons.json");
		if (!weaponsFile.exists()) {
			Log.i(TAG, "Load weapons from API...");
			JSONObject weapons = get(api + "weapon/get-all", "").getJSONObject("weapons");
			Util.writeFile(weapons.toJSONString(), "data/weapons.json");
		}
		File chipsFile = new File("data/chips.json");
		if (!chipsFile.exists()) {
			Log.i(TAG, "Load chips from API...");
			JSONObject chips = get(api + "chip/get-all", "").getJSONObject("chips");
			Util.writeFile(chips.toJSONString(), "data/chips.json");
		}
		File summonsFile = new File("data/summons.json");
		if (!summonsFile.exists()) {
			Log.i(TAG, "Load summons from API...");
			JSONObject summons = get(api + "summon/get-templates", "").getJSONObject("summon_templates");
			Util.writeFile(summons.toJSONString(), "data/summons.json");
		}
		File functionsFile = new File("data/functions.json");
		if (!functionsFile.exists()) {
			Log.i(TAG, "Load functions from API...");
			JSONObject functions = get(api + "function/operations", "");
			Util.writeFile(functions.toJSONString(), "data/functions.json");
		}
	}

    private static JSONObject get(String path, String urlParameters) {
		HttpURLConnection connection = null;
		try {
			// Create connection
			URL url = new URL(path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return JSON.parseObject(response.toString());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
    }
}