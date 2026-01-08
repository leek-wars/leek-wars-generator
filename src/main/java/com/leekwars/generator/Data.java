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

import com.leekwars.generator.util.Json;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class Data {

	private static final String TAG = Data.class.getSimpleName();

	public static List<LocalDate> fullmoon = new ArrayList<>();

	public static void checkData(String api) {

		new File("data").mkdir();

		System.out.println("Check api: " + api);
		// File weaponsFile = new File("data/weapons.json");
		System.out.println("Load weapons from API...");
		ObjectNode weapons = (ObjectNode) Json.parseObject(get(api + "weapon/get-all", "")).get("weapons");
		Util.writeFile(weapons.toString(), "data/weapons.json");

		// File chipsFile = new File("data/chips.json");
		System.out.println("Load chips from API...");
		ObjectNode chips = (ObjectNode) Json.parseObject(get(api + "chip/get-all", "")).get("chips");
		Util.writeFile(chips.toString(), "data/chips.json");

		// File summonsFile = new File("data/summons.json");
		System.out.println("Load summons from API...");
		ObjectNode summons = (ObjectNode) Json.parseObject(get(api + "summon/get-templates", "")).get("summon_templates");
		Util.writeFile(summons.toString(), "data/summons.json");

		// File fullmoonFile = new File("data/fullmoon.json");
		System.out.println("Load fullmoon from API...");
		ArrayNode f = Json.parseArray(get(api + "fight/fullmoon", ""));
		for (var d : f) {
			var dateUTC = ZonedDateTime.of(LocalDateTime.parse(d.asString()), ZoneOffset.UTC);
			var dateLocal = dateUTC.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
			fullmoon.add(dateLocal);
		}
		// System.out.println("full moon = " + fullmoon);
		Util.writeFile(f.toString(), "data/fullmoon.json");

		// TODO
		System.out.println("Load components from API...");
		ObjectNode components = Json.parseObject(get(api + "component/get-all/dfgdfgzegktyrtytm", ""));
		Util.writeFile(components.toString(), "data/components.json");
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