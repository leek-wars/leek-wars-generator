package com.leekwars.generator;

import java.io.File;
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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class Data {

	private static final String TAG = Data.class.getSimpleName();

	public static List<LocalDate> fullmoon = new ArrayList<>();

	public static void checkData(String api) {

		new File("data").mkdir();

		System.out.println("Check api: " + api);
		// File weaponsFile = new File("data/weapons.json");
		System.out.println("Load weapons from API...");
		JSONObject weapons = JSON.parseObject(get(api + "weapon/get-all", "")).getJSONObject("weapons");
		Util.writeFile(weapons.toJSONString(), "data/weapons.json");

		// File chipsFile = new File("data/chips.json");
		System.out.println("Load chips from API...");
		JSONObject chips = JSON.parseObject(get(api + "chip/get-all", "")).getJSONObject("chips");
		Util.writeFile(chips.toJSONString(), "data/chips.json");

		// File summonsFile = new File("data/summons.json");
		System.out.println("Load summons from API...");
		JSONObject summons = JSON.parseObject(get(api + "summon/get-templates", "")).getJSONObject("summon_templates");
		Util.writeFile(summons.toJSONString(), "data/summons.json");

		// File fullmoonFile = new File("data/fullmoon.json");
		System.out.println("Load fullmoon from API...");
		JSONArray f = JSON.parseArray(get(api + "fight/fullmoon", ""));
		for (var d : f) {
			var dateUTC = ZonedDateTime.of(LocalDateTime.parse((String) d), ZoneOffset.UTC);
			var dateLocal = dateUTC.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
			fullmoon.add(dateLocal);
		}
		// System.out.println("full moon = " + fullmoon);
		Util.writeFile(f.toJSONString(), "data/fullmoon.json");

		// TODO
		System.out.println("Load components from API...");
		JSONObject components = JSON.parseObject(get(api + "component/get-all/dfgdfgzegktyrtytm", ""));
		Util.writeFile(components.toJSONString(), "data/components.json");
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
		// System.out.println("f = " + fullmoon);
		for (var d : fullmoon) {
			// System.out.println("d = " + d);
			if (d.equals(today)) return true;
		}
		return false;
	}
}