package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.ComponentInstance;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.FightLoadout;
import com.leekwars.generator.state.LoadoutRules;

import tools.jackson.databind.JsonNode;

/**
 * setLoadout(objet) et getInventory() : un équipement construit en code, appliqué en beforeFight
 * avec un exemplaire de chaque objet par poireau de l'éleveur. Les vraies règles (niveaux,
 * capital) sont celles du serveur : ici des règles factices, la stat = le capital investi.
 */
public class TestLoadoutObject extends FightTestBase {

	private static final int FARMER = 7;
	private Leek a1, a2, b1;
	// Inventaire que les règles rendent à la demande, et nombre de demandes.
	private Map<Integer, Integer> lazyItems = null;
	private int itemRequests = 0;
	private List<ComponentInstance> lazyPieces = List.of();

	private static Leek farmerLeek(int id, String name, int farmer) {
		return new Leek(id, name, farmer, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 8, 30, 0, false, 0, 0, "", 0, "", "", "", 0);
	}

	@Override
	protected void createLeeks() {
		a1 = farmerLeek(1, "A1", FARMER);
		a2 = farmerLeek(2, "A2", FARMER);
		fight.getState().addEntity(0, a1);
		fight.getState().addEntity(0, a2);
		b1 = farmerLeek(3, "B1", 8);
		fight.getState().addEntity(1, b1);
		Generator.setLoadoutRules(new LoadoutRules() {
			@Override public FightLoadout resolve(JsonNode loadout, int level) {
				Map<Integer, Integer> stats = new HashMap<>();
				stats.put(Entity.STAT_STRENGTH, loadout.get("stats").get("strength").asInt());
				// Composants : le modèle et la force que doit ajouter la pièce améliorée voulue.
				var components = new ArrayList<FightLoadout.ComponentChoice>();
				for (var c : loadout.get("components")) {
					var wanted = c.path("stats").has("strength") ? Map.of(Entity.STAT_STRENGTH, c.get("stats").get("strength").asInt()) : null;
					components.add(new FightLoadout.ComponentChoice(c.get("item").asInt(), wanted));
				}
				return new FightLoadout(loadout.get("name").asString(), ids(loadout.get("weapons")), ids(loadout.get("forgotten_weapons")),
					ids(loadout.get("chips")), stats, stats, false, components);
			}
			@Override public int totalCapital(int level) { return level * 5; }
			@Override public Map<Integer, Map<Integer, Integer>> farmerItems(java.util.Set<Integer> farmers) {
				itemRequests++;
				var items = new HashMap<Integer, Map<Integer, Integer>>();
				if (lazyItems != null && farmers.contains(FARMER)) items.put(FARMER, lazyItems);
				return items;
			}
			@Override public Map<Integer, List<ComponentInstance>> freeComponents(java.util.Set<Integer> farmers) {
				return farmers.contains(FARMER) ? Map.of(FARMER, lazyPieces) : Map.of();
			}
		});
	}

	@After
	public void resetRules() {
		Generator.setLoadoutRules(null);
	}

	private static List<Integer> ids(JsonNode list) {
		var out = new ArrayList<Integer>();
		for (var id : list) out.add(id.asInt());
		return out;
	}

	private void own(Map<Integer, Integer> items) {
		fight.getState().setFarmerItems(FARMER, items);
		fight.getState().setRestatPotionsAvailable(FARMER, 10);
	}

	/** Combat où seuls les deux poireaux de l'éleveur ont une IA. */
	private void run(String codeA1, String codeA2) throws Exception {
		attachAI(a1, codeA1);
		attachAI(a2, codeA2);
		attachAI(b1, "");
		runFight();
	}

	private static List<Integer> weapons(Leek leek) {
		return leek.getWeapons().stream().map(w -> w.getId()).toList();
	}

	private static final String TANK = "{weapons: [WEAPON_M_LASER, 'pistol'], chips: [CHIP_FLASH], stats: {strength: 50}}";

	@Test
	public void appliesAnObjectBuiltInCode() throws Exception {
		own(Map.of(47, 1, 37, 1, 6, 1));
		run("function beforeFight() { setRegister('ok', '' + setLoadout(" + TANK + ")) }", "");
		Assert.assertEquals("true", a1.getRegister("ok"));
		Assert.assertEquals(List.of(47, 37), weapons(a1));
		Assert.assertEquals(List.of(6), a1.getChips().stream().map(c -> c.getId()).toList());
		Assert.assertEquals(50, a1.getStrength());
	}

	@Test
	public void oneCopyPerLeek() throws Exception {
		// Un seul laser : le second poireau de l'éleveur qui le demande ne l'a pas.
		own(Map.of(47, 1, 37, 2, 6, 2));
		String code = "function beforeFight() { setLoadout(" + TANK + ") }";
		run(code, code);
		long lasers = List.of(a1, a2).stream().filter(l -> l.getWeapons().stream().anyMatch(w -> w.getId() == 47)).count();
		Assert.assertEquals(1, lasers);
		Assert.assertTrue(a1.getWeapons().stream().anyMatch(w -> w.getId() == 37));
		Assert.assertTrue(a2.getWeapons().stream().anyMatch(w -> w.getId() == 37));
	}

	@Test
	public void inventoryCountsWhatTheOtherLeeksDoNotWear() throws Exception {
		own(Map.of(47, 1, 37, 2, 6, 1, 292, 1));
		run("function beforeFight() { setLoadout({weapons: ['pistol'], chips: [CHIP_FLASH], stats: {strength: 1}}) }", "function beforeFight() { var inv = getInventory()\n"
			+ "setRegister('inv', jsonEncode([inv.weapons, inv.chips, inv.components, inv.capital])) }");
		// a1 porte un pistolet et le flash : il reste au second un laser, un pistolet, le core3, pas de flash.
		Assert.assertEquals("[[{\"item\":37,\"quantity\":1},{\"item\":47,\"quantity\":1}],[],[{\"alterations\":[],\"base\":{\"cores\":10,\"frequency\":30,\"tp\":1},\"item\":292,\"quantity\":1}],50]", a2.getRegister("inv"));
	}

	@Test
	public void jsInventoryAndObjectLoadout() throws Exception {
		own(Map.of(47, 1, 6, 1));
		attachJsAI(a1, "function beforeFight() { var inv = Fight.me.getInventory();"
			+ " var ok = Fight.me.setLoadout({ weapons: inv.weapons.map(function (w) { return w.item; }), chips: [], stats: { strength: 70 } });"
			+ " Registers.set('ok', '' + ok + ' ' + inv.capital); }\nfunction turn() {}");
		attachAI(a2, "");
		attachAI(b1, "");
		runFight();
		Assert.assertEquals("true 50", a1.getRegister("ok"));
		Assert.assertEquals(List.of(47), weapons(a1));
		Assert.assertEquals(70, a1.getStrength());
	}

	private void attachJsAI(Leek leek, String code) {
		attachAI(leek, new leekscript.compiler.AIFile("loadout_object_" + System.nanoTime() + ".js", code,
			System.currentTimeMillis(), leekscript.compiler.LeekScript.LATEST_VERSION, leek.getId(), false));
	}

	@Test
	public void forgottenWeaponsStayCandidatesAndCountOnce() throws Exception {
		// Deux armes oubliées listées comme armes, dont une aussi en candidate : une seule portée.
		own(Map.of(116, 1, 117, 1, 37, 1));
		run("function beforeFight() { setLoadout({weapons: [116, 117, 'pistol'], forgotten_weapons: [116], stats: {strength: 1}}) }", "");
		var ids = weapons(a1);
		Assert.assertEquals(1, ids.stream().filter(id -> id == 116 || id == 117).count());
		Assert.assertTrue(ids.contains(37));
		Assert.assertEquals(ids.size(), ids.stream().distinct().count());
	}

	@Test
	public void anUnownedForgottenWeaponIsRefused() throws Exception {
		own(Map.of(37, 1));
		run("function beforeFight() { setLoadout({weapons: [116, 'pistol'], stats: null}) }", "");
		Assert.assertEquals(List.of(37), weapons(a1));
		Assert.assertEquals(100, a1.getStrength()); // stats: null = pas de répartition
	}

	@Test
	public void withoutStatsOnlyTheGearChanges() throws Exception {
		own(Map.of(37, 1));
		run("function beforeFight() { setLoadout({weapons: ['pistol']}) }", "");
		Assert.assertEquals(List.of(37), weapons(a1));
		Assert.assertEquals(100, a1.getStrength()); // capital gardé, aucune potion
		Assert.assertEquals(10, fight.getState().getRestatPotionsAvailable(FARMER));
	}

	@Test
	public void statsWithoutCapitalKeepTheCapital() throws Exception {
		// Ce que rend getLoadout d'un fichier sans stats : toutes à zéro. Pas une répartition, pas plus
		// qu'une clé inconnue (lue à zéro par la normalisation).
		own(Map.of(37, 2));
		run("function beforeFight() { setLoadout({weapons: ['pistol'], stats: {strength: 0, life: 0}}) }",
			"function beforeFight() { setLoadout({weapons: ['pistol'], stats: {strenght: 100}}) }");
		Assert.assertEquals(List.of(37), weapons(a1));
		Assert.assertEquals(100, a1.getStrength());
		Assert.assertEquals(100, a2.getStrength());
		Assert.assertEquals(10, fight.getState().getRestatPotionsAvailable(FARMER));
	}

	@Test
	public void unknownInventoryRefusesAnObject() throws Exception {
		run("function beforeFight() { setRegister('ok', '' + setLoadout(" + TANK + ")) }", "");
		Assert.assertEquals("false", a1.getRegister("ok"));
	}

	@Test
	public void aNumberIsStillALoadoutName() throws Exception {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_STRENGTH, 300);
		a1.addLoadout(new FightLoadout("1", List.of(), List.of(), List.of(), stats));
		own(Map.of());
		run("function beforeFight() { var n = 1\nsetRegister('ok', '' + setLoadout(n)) }", "");
		Assert.assertEquals("true", a1.getRegister("ok"));
		Assert.assertEquals(300, a1.getStrength());
	}

	@Test
	public void withoutRulesAnObjectIsRefused() throws Exception {
		Generator.setLoadoutRules(null);
		run("function beforeFight() { setRegister('ok', '' + setLoadout(" + TANK + ")) }", "");
		Assert.assertEquals("false", a1.getRegister("ok"));
	}

	@Test
	public void theInventoryIsAskedOnlyWhenNeeded() throws Exception {
		lazyItems = Map.of(37, 1);
		run("function beforeFight() { setRegister('ok', '' + setLoadout({weapons: ['pistol']})) }", "");
		Assert.assertEquals("true", a1.getRegister("ok"));
		Assert.assertEquals(List.of(37), weapons(a1));
		Assert.assertEquals(1, itemRequests);
	}

	@Test
	public void noInventoryRequestWithoutLoadout() throws Exception {
		run("", "");
		Assert.assertEquals(0, itemRequests);
	}

	@Test
	public void aSavedLoadoutAlsoTakesOneCopyPerLeek() throws Exception {
		// Un laser et une plaque de fer (vie 50, résistance 40) pour deux poireaux qui prennent le même ensemble.
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_STRENGTH, 50);
		stats.put(Entity.STAT_RESISTANCE, 40);
		for (var leek : List.of(a1, a2)) {
			leek.addLoadout(new FightLoadout("tank", List.of(47, 37), List.of(), List.of(), stats, Map.of(Entity.STAT_STRENGTH, 50), false,
				List.of(new FightLoadout.ComponentChoice(294, null))));
		}
		own(Map.of(47, 1, 37, 2, 294, 1));
		run("function beforeFight() { setLoadout('tank') }", "function beforeFight() { setLoadout('tank') }");
		var first = weapons(a1).contains(47) ? a1 : a2;
		var second = first == a1 ? a2 : a1;
		Assert.assertEquals(List.of(47, 37), weapons(first));
		Assert.assertEquals(List.of(37), weapons(second));
		Assert.assertEquals(40, first.getStat(Entity.STAT_RESISTANCE));
		Assert.assertEquals(0, second.getStat(Entity.STAT_RESISTANCE));
		Assert.assertEquals(50, second.getStrength());
	}

	@Test
	public void aSavedLoadoutIsKeptWhenTheInventoryIsUnknown() throws Exception {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_STRENGTH, 50);
		for (var leek : List.of(a1, a2)) leek.addLoadout(new FightLoadout("tank", List.of(47), List.of(), List.of(), stats));
		run("function beforeFight() { setLoadout('tank') }", "function beforeFight() { setLoadout('tank') }");
		Assert.assertEquals(List.of(47), weapons(a1));
		Assert.assertEquals(List.of(47), weapons(a2));
	}

	@Test
	public void anEnemyLeekOfTheSameFarmerTakesNothing() throws Exception {
		// Combat de test entre deux poireaux du même éleveur : chaque équipe a ses exemplaires.
		b1.setFarmer(FARMER);
		own(Map.of(47, 1));
		attachAI(a1, "function beforeFight() { setLoadout({weapons: [WEAPON_M_LASER]}) }");
		attachAI(a2, "");
		attachAI(b1, "function beforeFight() { setLoadout({weapons: [WEAPON_M_LASER]}) }");
		runFight();
		Assert.assertEquals(List.of(47), weapons(a1));
		Assert.assertEquals(List.of(47), weapons(b1));
	}

	@Test
	public void improvedPiecesShowInTheInventoryAndCanBeTaken() throws Exception {
		// Deux core3 dont un amélioré (+7 force), lu à la demande : le premier poireau le prend.
		lazyItems = Map.of(292, 2);
		lazyPieces = List.of(new ComponentInstance(900, 292, Map.of(Entity.STAT_STRENGTH, 7)));
		fight.getState().setRestatPotionsAvailable(FARMER, 10);
		String take = "setLoadout({components: [{item: 292, stats: {strength: 7}}], stats: {strength: 10}})";
		run("function beforeFight() { setRegister('inv', jsonEncode(getInventory().components))\n" + take + " }",
			"function beforeFight() { " + take + "\nsetRegister('inv', jsonEncode(getInventory().components)) }");
		Assert.assertEquals("[{\"alterations\":[{\"strength\":7}],\"base\":{\"cores\":10,\"frequency\":30,\"tp\":1},\"item\":292,\"quantity\":2}]", a1.getRegister("inv"));
		Assert.assertEquals(17, a1.getStrength());
		Assert.assertEquals(10, a2.getStrength());
		// Le second a pris la pièce de base : son inventaire ne compte que la sienne, sans amélioration.
		Assert.assertEquals("[{\"alterations\":[],\"base\":{\"cores\":10,\"frequency\":30,\"tp\":1},\"item\":292,\"quantity\":1}]", a2.getRegister("inv"));
	}

	/** Entrée du poireau dans l'instantané `leeks` envoyé au client. */
	private JsonNode snapshot(Leek leek) {
		for (var node : fight.getState().getActions().toJSON().get("leeks")) {
			if (node.get("name").asString().equals(leek.getName())) return node;
		}
		return null;
	}

	@Test
	public void theFightRecordsTheEquipmentPlayed() throws Exception {
		// Le rapport montre l'équipement joué, pas celui d'avant combat : armes, puces, composants
		// (la pièce améliorée avec son altération), et le nom de l'ensemble.
		lazyItems = Map.of(292, 2, 47, 1, 6, 1);
		lazyPieces = List.of(new ComponentInstance(900, 292, Map.of(Entity.STAT_STRENGTH, 7)));
		fight.getState().setRestatPotionsAvailable(FARMER, 10);
		run("function beforeFight() { setLoadout({name: 'Tank', weapons: [WEAPON_M_LASER], chips: [CHIP_FLASH], components: [{item: 292, stats: {strength: 7}}], stats: {strength: 10}}) }",
			"function beforeFight() { setLoadout({components: [{item: 292}], stats: {strength: 10}}) }");
		var tank = snapshot(a1);
		var equipment = tank.get("equipment");
		Assert.assertEquals("[47]", equipment.get("weapons").toString());
		Assert.assertEquals("[6]", equipment.get("chips").toString());
		Assert.assertEquals("[{\"template\":292,\"stats\":{\"strength\":7}}]", equipment.get("components").toString());
		Assert.assertEquals("Tank", equipment.get("loadout").asString());
		Assert.assertEquals(17, tank.get("strength").intValue());
		// Équipement construit sans nom : marqué quand même, la pièce de base sans altération.
		var other = snapshot(a2).get("equipment");
		Assert.assertEquals("", other.get("loadout").asString());
		Assert.assertEquals("[{\"template\":292}]", other.get("components").toString());
		// Sans setLoadout(), le poireau joue son équipement d'avant combat : pas de nom d'ensemble.
		var enemy = snapshot(b1).get("equipment");
		Assert.assertFalse(enemy.has("loadout"));
		Assert.assertEquals("[]", enemy.get("weapons").toString());
	}
}
