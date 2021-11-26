package com.leekwars.generator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class Data {

	private static final String TAG = Data.class.getSimpleName();

	public static List<LocalDate> fullmoon = new ArrayList<>();

	public static void checkData(String api) {
		Log.i(TAG, "Check api: " + api);
		// File weaponsFile = new File("data/weapons.json");
		Log.i(TAG, "Load weapons from API...");
		JSONObject weapons = JSON.parseObject(get(api + "weapon/get-all", "")).getJSONObject("weapons");
		Util.writeFile(weapons.toJSONString(), "data/weapons.json");

		// File chipsFile = new File("data/chips.json");
		Log.i(TAG, "Load chips from API...");
		JSONObject chips = JSON.parseObject(get(api + "chip/get-all", "")).getJSONObject("chips");
		Util.writeFile(chips.toJSONString(), "data/chips.json");

		// File summonsFile = new File("data/summons.json");
		Log.i(TAG, "Load summons from API...");
		JSONObject summons = JSON.parseObject(get(api + "summon/get-templates", "")).getJSONObject("summon_templates");
		Util.writeFile(summons.toJSONString(), "data/summons.json");

		// File functionsFile = new File("data/functions.json");
		Log.i(TAG, "Load functions from API...");
		JSONObject functions = JSON.parseObject(get(api + "function/operations", ""));
		Util.writeFile(functions.toJSONString(), "data/functions.json");

		// File fullmoonFile = new File("data/fullmoon.json");
		Log.i(TAG, "Load fullmoon from API...");
		var f = JSON.parseArray(get(api + "fight/fullmoon", ""));
		for (var d : f) {
			var dateUTC = ZonedDateTime.of(LocalDateTime.parse((String) d), ZoneOffset.UTC);
			var dateLocal = dateUTC.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
			fullmoon.add(dateLocal);
		}
		// System.out.println("full moon = " + fullmoon);
		Util.writeFile(f.toJSONString(), "data/fullmoon.json");
	}

    private static String get(String url, String urlParameters) {
		Log.i(TAG, "get " + url);

		var client = HttpClient.newHttpClient();

		var request = HttpRequest.newBuilder(URI.create(url))
			.header("accept", "application/json")
			.build();

		try {
			var response = client.send(request, BodyHandlers.ofString());
			return response.body();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace(System.out);
		}
		return null;
    }

	public static boolean isFullMoon() {
		var today = LocalDate.now();
		for (var d : fullmoon) {
			if (d.equals(today)) return true;
		}
		return false;
	}
}