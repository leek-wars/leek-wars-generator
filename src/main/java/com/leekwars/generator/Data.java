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
import tools.jackson.databind.JsonNode;
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
		ObjectNode weapons = fetchObject(api, "weapon/get-all", "weapons");
		Util.writeFile(weapons.toString(), "data/weapons.json");

		// File chipsFile = new File("data/chips.json");
		System.out.println("Load chips from API...");
		ObjectNode chips = fetchObject(api, "chip/get-all", "chips");
		Util.writeFile(chips.toString(), "data/chips.json");

		// File summonsFile = new File("data/summons.json");
		System.out.println("Load summons from API...");
		ObjectNode summons = fetchObject(api, "summon/get-templates", "summon_templates");
		Util.writeFile(summons.toString(), "data/summons.json");

		// File fullmoonFile = new File("data/fullmoon.json");
		System.out.println("Load fullmoon from API...");
		ArrayNode f = fetchArray(api, "fight/fullmoon");
		fullmoon.clear(); // checkData() est rejouable (reload à chaud) : éviter d'empiler les dates à chaque appel
		for (var d : f) {
			var dateUTC = ZonedDateTime.of(LocalDateTime.parse(d.asString()), ZoneOffset.UTC);
			var dateLocal = dateUTC.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
			fullmoon.add(dateLocal);
		}
		// System.out.println("full moon = " + fullmoon);
		Util.writeFile(f.toString(), "data/fullmoon.json");

		// TODO
		System.out.println("Load components from API...");
		ObjectNode components = fetchObject(api, "component/get-all/dfgdfgzegktyrtytm", null);
		// Valider la forme de chaque entrée AVANT d'écrire : loadComponents() caste
		// chaque valeur en ObjectNode et plante (ClassCastException StringNode ->
		// ObjectNode) si l'API a renvoyé une enveloppe d'erreur {"error":"..."}.
		for (var entry : components.properties()) {
			if (!entry.getValue().isObject()) {
				throw new IllegalStateException("Composant '" + entry.getKey()
					+ "' n'est pas un objet : " + snippet(components.toString()));
			}
		}
		Util.writeFile(components.toString(), "data/components.json");
	}

	/**
	 * Récupère et valide une réponse game-data de l'API AVANT toute écriture sur
	 * disque. Une enveloppe d'erreur ({"error":"..."} pendant un redéploiement),
	 * une clé attendue absente ou un corps vide lèvent ici une exception : la
	 * boucle de retry de Worker rejoue le chargement et l'ancien fichier (sain)
	 * est préservé au lieu d'être écrasé par du garbage. Sans ce garde-fou, un
	 * blip transitoire corrompt data/*.json et fait planter GeneratorAPI.init().
	 *
	 * @param key clé à extraire de la racine, ou null si la racine EST l'objet attendu.
	 */
	private static ObjectNode fetchObject(String api, String path, String key) {
		JsonNode root = fetchJson(api, path);
		JsonNode node = key == null ? root : root.get(key);
		if (node == null || !node.isObject()) {
			throw new IllegalStateException("Réponse inattendue de " + path + " (clé '"
				+ key + "' absente ou non-objet) : " + snippet(root.toString()));
		}
		return (ObjectNode) node;
	}

	private static ArrayNode fetchArray(String api, String path) {
		JsonNode root = fetchJson(api, path);
		if (!root.isArray()) {
			throw new IllegalStateException("Réponse inattendue de " + path
				+ " (tableau attendu) : " + snippet(root.toString()));
		}
		return (ArrayNode) root;
	}

	private static JsonNode fetchJson(String api, String path) {
		String body = get(api + path);
		if (body == null || body.isBlank()) {
			throw new IllegalStateException("Réponse vide de " + path);
		}
		return Json.parse(body);
	}

	private static String snippet(String body) {
		return body.length() > 200 ? body.substring(0, 200) + "..." : body;
	}

    private static String get(String url) {
		Log.i(TAG, "get " + url);

		var client = HttpClient.newHttpClient();

		var request = HttpRequest.newBuilder(URI.create(url))
			.header("accept", "application/json")
			.build();

		try {
			var response = client.send(request, BodyHandlers.ofString());
			// Une réponse non-200 (5xx/maintenance pendant un redéploiement de l'API)
			// ne doit jamais être traitée comme des données valides : on lève pour que
			// la boucle de retry rejoue et préserve l'ancien fichier sain. C'est la
			// cause racine du ClassCastException : un corps d'erreur était écrit verbatim.
			if (response.statusCode() != 200) {
				throw new IllegalStateException("HTTP " + response.statusCode() + " sur " + url
					+ " : " + snippet(response.body()));
			}
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