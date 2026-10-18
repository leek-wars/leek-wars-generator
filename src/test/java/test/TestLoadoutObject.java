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
				return new FightLoadout(loadout.get("name").asString(), ids(loadout.get("weapons")), ids(loadout.get("forgotten_weapons")),
					ids(loadout.get("chips")), stats, stats, false, new ArrayList<>());
			}
			@Override public int totalCapital(int level) { return level * 5; }
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

	private static final String TANK = "{weapons: [WEAPON_M_LASER, 'pistol'], chips: [CHIP_FLASH], stats: {strength: 50}}";

	@Test
	public void appliesAnObjectBuiltInCode() throws Exception {
		own(Map.of(47, 1, 37, 1, 6, 1));
		attachAI(a1, "function beforeFight() { setRegister('ok', '' + setLoadout(" + TANK + ")) }");
		attachAI(a2, "");
		attachAI(b1, "");
		runFight();
		Assert.assertEquals("true", a1.getRegister("ok"));
		Assert.assertEquals(List.of(47, 37), a1.getWeapons().stream().map(w -> w.getId()).toList());
		Assert.assertEquals(List.of(6), a1.getChips().stream().map(c -> c.getId()).toList());
		Assert.assertEquals(50, a1.getStrength());
	}

	@Test
	public void oneCopyPerLeek() throws Exception {
		// Un seul laser : le second poireau de l'éleveur qui le demande ne l'a pas.
		own(Map.of(47, 1, 37, 2, 6, 2));
		String code = "function beforeFight() { setLoadout(" + TANK + ") }";
		attachAI(a1, code);
		attachAI(a2, code);
		attachAI(b1, "");
		runFight();
		long lasers = List.of(a1, a2).stream().filter(l -> l.getWeapons().stream().anyMatch(w -> w.getId() == 47)).count();
		Assert.assertEquals(1, lasers);
		Assert.assertTrue(a1.getWeapons().stream().anyMatch(w -> w.getId() == 37));
		Assert.assertTrue(a2.getWeapons().stream().anyMatch(w -> w.getId() == 37));
	}

	@Test
	public void inventoryCountsWhatTheOtherLeeksDoNotWear() throws Exception {
		own(Map.of(47, 1, 37, 2, 6, 1, 292, 1));
		attachAI(a1, "function beforeFight() { setLoadout({weapons: ['pistol'], chips: [CHIP_FLASH], stats: {strength: 1}}) }");
		attachAI(a2, "function beforeFight() { var inv = getInventory()\n"
			+ "setRegister('inv', jsonEncode([inv.weapons, inv.chips, inv.components, inv.capital])) }");
		attachAI(b1, "");
		runFight();
		// a1 porte un pistolet et le flash : il reste au second un laser, un pistolet, le core3, pas de flash.
		Assert.assertEquals("[[{\"item\":37,\"quantity\":1},{\"item\":47,\"quantity\":1}],[],[{\"item\":292,\"quantity\":1}],50]", a2.getRegister("inv"));
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
		Assert.assertEquals(List.of(47), a1.getWeapons().stream().map(w -> w.getId()).toList());
		Assert.assertEquals(70, a1.getStrength());
	}

	private void attachJsAI(Leek leek, String code) {
		var file = new leekscript.compiler.AIFile("loadout_object_" + System.nanoTime() + ".js", code,
			System.currentTimeMillis(), leekscript.compiler.LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new com.leekwars.generator.leek.LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	@Test
	public void forgottenWeaponsStayCandidatesAndCountOnce() throws Exception {
		// Deux armes oubliées listées comme armes, dont une aussi en candidate : une seule portée.
		own(Map.of(116, 1, 117, 1, 37, 1));
		attachAI(a1, "function beforeFight() { setLoadout({weapons: [116, 117, 'pistol'], forgotten_weapons: [116], stats: {strength: 1}}) }");
		attachAI(a2, "");
		attachAI(b1, "");
		runFight();
		var ids = a1.getWeapons().stream().map(w -> w.getId()).toList();
		Assert.assertEquals(1, ids.stream().filter(id -> id == 116 || id == 117).count());
		Assert.assertTrue(ids.contains(37));
		Assert.assertEquals(ids.size(), ids.stream().distinct().count());
	}

	@Test
	public void withoutStatsOnlyTheGearChanges() throws Exception {
		own(Map.of(37, 1));
		attachAI(a1, "function beforeFight() { setLoadout({weapons: ['pistol']}) }");
		attachAI(a2, "");
		attachAI(b1, "");
		runFight();
		Assert.assertEquals(List.of(37), a1.getWeapons().stream().map(w -> w.getId()).toList());
		Assert.assertEquals(100, a1.getStrength()); // capital gardé, aucune potion
		Assert.assertEquals(10, fight.getState().getRestatPotionsAvailable(FARMER));
	}

	@Test
	public void unknownInventoryRefusesAnObject() throws Exception {
		attachAI(a1, "function beforeFight() { setRegister('ok', '' + setLoadout(" + TANK + ")) }");
		attachAI(a2, "");
		attachAI(b1, "");
		runFight();
		Assert.assertEquals("false", a1.getRegister("ok"));
	}

	@Test
	public void aNumberIsStillALoadoutName() throws Exception {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_STRENGTH, 300);
		a1.addLoadout(new FightLoadout("1", List.of(), List.of(), List.of(), stats));
		own(Map.of());
		attachAI(a1, "function beforeFight() { var n = 1\nsetRegister('ok', '' + setLoadout(n)) }");
		attachAI(a2, "");
		attachAI(b1, "");
		runFight();
		Assert.assertEquals("true", a1.getRegister("ok"));
		Assert.assertEquals(300, a1.getStrength());
	}

	@Test
	public void withoutRulesAnObjectIsRefused() throws Exception {
		Generator.setLoadoutRules(null);
		attachAI(a1, "function beforeFight() { setRegister('ok', '' + setLoadout(" + TANK + ")) }");
		attachAI(a2, "");
		attachAI(b1, "");
		runFight();
		Assert.assertEquals("false", a1.getRegister("ok"));
	}
}
