package test;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.FightLoadout;

/**
 * End-to-end tests that build a Fight, attach LeekScript AIs as AIFile, and run
 * fight.startFight() — exercising the full Fight.runHooks orchestration, the
 * turn loop, and post-fight cleanup. Distinct from TestHooks which calls
 * runHook() directly without going through Fight.startFight().
 */
public class TestHooksFight extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private static FightLoadout statsLoadout(String name, int life, int strength, int agility) {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_LIFE, life);
		stats.put(Entity.STAT_STRENGTH, strength);
		stats.put(Entity.STAT_AGILITY, agility);
		return new FightLoadout(name, java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats);
	}

	// ---------- Hook orchestration ----------

	@Test
	public void beforeFightCalledViaStartFight() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('hook', '1'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1", leek1.getRegister("hook"));
	}

	@Test
	public void afterFightCalledViaStartFight() throws Exception {
		attachAI(leek1, "function afterFight() { setRegister('after', '1'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1", leek1.getRegister("after"));
	}

	@Test
	public void bothHooksCalledOnSameLeek() throws Exception {
		attachAI(leek1,
			"function beforeFight() { setRegister('phase', 'before'); }"
			+ "function afterFight() { setRegister('phase', getRegister('phase') + ',after'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("before,after", leek1.getRegister("phase"));
	}

	@Test
	public void hooksCalledOnEachEntity() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('me', 'leek1'); }");
		attachAI(leek2, "function beforeFight() { setRegister('me', 'leek2'); }");
		runFight();
		Assert.assertEquals("leek1", leek1.getRegister("me"));
		Assert.assertEquals("leek2", leek2.getRegister("me"));
	}

	@Test
	public void noHookDefinedIsNoOp() throws Exception {
		attachAI(leek1, "var dummy = 1;");
		attachAI(leek2, "");
		runFight();
		// No crash, and no register written
		Assert.assertNull(leek1.getRegister("anything"));
	}

	// ---------- Phase flag transitions ----------

	@Test
	public void hookPhaseIsBeforeFightDuringBeforeFight() throws Exception {
		// Marker: setLoadout('__nope__') returns false outside of beforeFight phase,
		// returns false inside too if name unknown — but only inside the phase will
		// the warning code be 1006 (LOADOUT_NOT_FOUND), not 1007 (out-of-hook).
		// Easier: write the registered phase via setRegister directly.
		attachAI(leek1, "function beforeFight() { setRegister('inHook', setLoadout('__never__') ? 'true' : 'false'); }"
			+ "function afterFight() { setRegister('outHook', setLoadout('__never__') ? 'true' : 'false'); }");
		attachAI(leek2, "");
		runFight();
		// Inside beforeFight: setLoadout looks up name (not found) → false, but no out-of-hook warning
		Assert.assertEquals("false", leek1.getRegister("inHook"));
		// Outside beforeFight (in afterFight): setLoadout returns false because phase != BEFORE_FIGHT
		Assert.assertEquals("false", leek1.getRegister("outHook"));
	}

	@Test
	public void hookPhaseIsResetBetweenHookAndTurn() throws Exception {
		// beforeFight runs, then turn 1 runs the AI's main code.
		// During main, hookPhase must be NONE (not BEFORE_FIGHT).
		// Marker: setLoadout from main code triggers SET_LOADOUT_OUT_OF_HOOK warning + returns false.
		attachAI(leek1,
			"function beforeFight() {}"
			+ "setRegister('main_setLoadout', setLoadout('whatever') ? 'true' : 'false');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("false", leek1.getRegister("main_setLoadout"));
	}

	// ---------- setLoadout end-to-end ----------

	@Test
	public void setLoadoutInBeforeFightAppliesStatsForTheFight() throws Exception {
		leek1.addLoadout(statsLoadout("pvp", 800, 250, 50));
		// Initial leek strength = 100. After beforeFight setLoadout('pvp'), should be 250.
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }"
			+ "setRegister('strength_turn1', '' + getStrength());"
			+ "setRegister('life_turn1', '' + getTotalLife());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("250", leek1.getRegister("strength_turn1"));
		Assert.assertEquals("800", leek1.getRegister("life_turn1"));
	}

	@Test
	public void setLoadoutInBeforeFightReflectedInActionsSnapshot() throws Exception {
		// Regression: the `leeks` snapshot in actions JSON (consumed by the client to seed
		// initial life / max-life display) must reflect post-beforeFight stats, not the
		// leek's pre-loadout values. Otherwise the report shows a wrong max-life bar.
		leek1.addLoadout(statsLoadout("pvp", 800, 250, 50));
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }");
		attachAI(leek2, "");
		runFight();
		var leeksJson = fight.getState().getActions().toJSON().get("leeks");
		tools.jackson.databind.JsonNode leek1Json = null;
		for (var node : leeksJson) {
			if (node.get("name").asString().equals("L1")) { leek1Json = node; break; }
		}
		Assert.assertNotNull(leek1Json);
		Assert.assertEquals(800, leek1Json.get("life").intValue());
		Assert.assertEquals(250, leek1Json.get("strength").intValue());
	}

	@Test
	public void setLoadoutDoesNotAffectOtherLeeks() throws Exception {
		leek1.addLoadout(statsLoadout("pvp", 800, 250, 50));
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }");
		attachAI(leek2, "setRegister('opp_strength', '' + getStrength());");
		runFight();
		// leek2 still has its initial strength
		Assert.assertEquals("100", leek2.getRegister("opp_strength"));
	}

	@Test
	public void setLoadoutMultipleCallsLastWins() throws Exception {
		leek1.addLoadout(statsLoadout("strong", 500, 300, 0));
		leek1.addLoadout(statsLoadout("fast", 500, 0, 300));
		attachAI(leek1, "function beforeFight() { setLoadout('strong'); setLoadout('fast'); }"
			+ "setRegister('s', '' + getStrength());"
			+ "setRegister('a', '' + getAgility());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("0", leek1.getRegister("s"));
		Assert.assertEquals("300", leek1.getRegister("a"));
	}

	@Test
	public void setLoadoutWithUnknownNameKeepsCurrentEquipment() throws Exception {
		// No loadouts registered, so setLoadout('whatever') fails — stats unchanged.
		attachAI(leek1, "function beforeFight() { setLoadout('whatever'); }"
			+ "setRegister('s', '' + getStrength());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("100", leek1.getRegister("s"));
	}

	@Test
	public void setLoadoutWithChangeStatsFalseKeepsStatsApplyItems() throws Exception {
		// changeStats=false : items appliqués mais stats non touchées (et pas de potion consommée).
		fight.getState().setRestatPotionsAvailable(0, 0);
		leek1.addLoadout(statsLoadout("pvp", 800, 250, 50));
		attachAI(leek1, "function beforeFight() { setLoadout('pvp', false); }"
			+ "setRegister('s', '' + getStrength());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("100", leek1.getRegister("s"));
	}

	@Test
	public void setLoadoutWithoutPotionAppliesItemsOnly() throws Exception {
		// Pas de potion + stats qui diffèrent → items appliqués, stats inchangées, warning.
		fight.getState().setRestatPotionsAvailable(0, 0);
		leek1.addLoadout(statsLoadout("pvp", 800, 250, 50));
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }"
			+ "setRegister('s', '' + getStrength());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("100", leek1.getRegister("s"));
	}

	@Test
	public void setLoadoutConsumesOnePotionPerStatsChange() throws Exception {
		fight.getState().setRestatPotionsAvailable(0, 1);
		leek1.addLoadout(statsLoadout("pvp", 800, 250, 50));
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals(Integer.valueOf(1), fight.getState().getRestatPotionsConsumed().get(0));
		Assert.assertEquals(0, fight.getState().getRestatPotionsAvailable(0));
	}

	@Test
	public void setLoadoutWithSameStatsDoesNotConsumePotion() throws Exception {
		fight.getState().setRestatPotionsAvailable(0, 1);
		// Loadout identique aux stats actuelles → pas de consommation.
		leek1.addLoadout(statsLoadout("same", 500, 100, 100));
		attachAI(leek1, "function beforeFight() { setLoadout('same'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertNull(fight.getState().getRestatPotionsConsumed().get(0));
		Assert.assertEquals(1, fight.getState().getRestatPotionsAvailable(0));
	}

	// ---------- setLoadout : potion de restat vs composants (capital) ----------

	/**
	 * Loadout dont on connaît la part de capital : `life/strength/agility` sont les stats
	 * finales, `*Capital` la part financée par le capital investi (le reste venant du niveau
	 * et des composants).
	 */
	private static FightLoadout capitalLoadout(String name, int life, int strength, int agility,
		int lifeCapital, int strengthCapital, int agilityCapital, boolean overCapital) {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_LIFE, life);
		stats.put(Entity.STAT_STRENGTH, strength);
		stats.put(Entity.STAT_AGILITY, agility);
		var capital = new HashMap<Integer, Integer>();
		capital.put(Entity.STAT_LIFE, lifeCapital);
		capital.put(Entity.STAT_STRENGTH, strengthCapital);
		capital.put(Entity.STAT_AGILITY, agilityCapital);
		return new FightLoadout(name, java.util.Collections.emptyList(), java.util.Collections.emptyList(),
			java.util.Collections.emptyList(), stats, capital, overCapital);
	}

	/** Part des stats de leek1 (500 vie / 100 force / 100 agilité) issue du capital. */
	private void setLeek1Capital() {
		var capital = new HashMap<Integer, Integer>();
		capital.put(Entity.STAT_LIFE, 400);
		capital.put(Entity.STAT_STRENGTH, 100);
		capital.put(Entity.STAT_AGILITY, 100);
		leek1.setCapitalStats(capital);
	}

	@Test
	public void setLoadoutWithOnlyComponentsNeedsNoPotion() throws Exception {
		// Même répartition de capital, mais des composants en plus (+110 vie) : aucun restat
		// n'est nécessaire, donc aucune potion — et les composants s'appliquent bien.
		fight.getState().setRestatPotionsAvailable(0, 0);
		setLeek1Capital();
		leek1.addLoadout(capitalLoadout("components", 610, 100, 100, 400, 100, 100, false));
		attachAI(leek1, "function beforeFight() { setLoadout('components'); }"
			+ "setRegister('l', '' + getTotalLife());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("610", leek1.getRegister("l"));
		Assert.assertNull(fight.getState().getRestatPotionsConsumed().get(0));
	}

	@Test
	public void setLoadoutWithAdditiveCapitalNeedsNoPotion() throws Exception {
		// Capital investi en plus sur la force, rien de retiré ailleurs : pas de restat.
		fight.getState().setRestatPotionsAvailable(0, 0);
		setLeek1Capital();
		leek1.addLoadout(capitalLoadout("additive", 500, 150, 100, 400, 150, 100, false));
		attachAI(leek1, "function beforeFight() { setLoadout('additive'); }"
			+ "setRegister('s', '' + getStrength());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("150", leek1.getRegister("s"));
		Assert.assertNull(fight.getState().getRestatPotionsConsumed().get(0));
	}

	@Test
	public void setLoadoutLoweringCapitalConsumesPotion() throws Exception {
		// Force réduite au profit de l'agilité : réallocation → potion.
		fight.getState().setRestatPotionsAvailable(0, 1);
		setLeek1Capital();
		leek1.addLoadout(capitalLoadout("reroll", 500, 50, 150, 400, 50, 150, false));
		attachAI(leek1, "function beforeFight() { setLoadout('reroll'); }"
			+ "setRegister('s', '' + getStrength());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("50", leek1.getRegister("s"));
		Assert.assertEquals(Integer.valueOf(1), fight.getState().getRestatPotionsConsumed().get(0));
	}

	@Test
	public void setLoadoutWithoutPotionStillAppliesComponents() throws Exception {
		// Réallocation refusée faute de potion : le capital reste celui du poireau, mais les
		// composants du loadout (+110 vie) s'équipent quand même.
		fight.getState().setRestatPotionsAvailable(0, 0);
		setLeek1Capital();
		leek1.addLoadout(capitalLoadout("reroll", 610, 50, 150, 400, 50, 150, false));
		attachAI(leek1, "function beforeFight() { setLoadout('reroll'); }"
			+ "setRegister('l', '' + getTotalLife());"
			+ "setRegister('s', '' + getStrength());"
			+ "setRegister('a', '' + getAgility());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("610", leek1.getRegister("l"));
		Assert.assertEquals("100", leek1.getRegister("s"));
		Assert.assertEquals("100", leek1.getRegister("a"));
	}

	@Test
	public void setLoadoutOverCapitalDoesNotApplyStats() throws Exception {
		// Loadout conçu pour un poireau bien plus gros : les caractéristiques ne s'appliquent
		// pas (et aucune potion n'est gaspillée), seul l'équipement suit.
		fight.getState().setRestatPotionsAvailable(0, 999);
		setLeek1Capital();
		leek1.addLoadout(capitalLoadout("huge", 5000, 900, 100, 4900, 900, 100, true));
		attachAI(leek1, "function beforeFight() { setLoadout('huge'); }"
			+ "setRegister('l', '' + getTotalLife());"
			+ "setRegister('s', '' + getStrength());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("500", leek1.getRegister("l"));
		Assert.assertEquals("100", leek1.getRegister("s"));
		Assert.assertNull(fight.getState().getRestatPotionsConsumed().get(0));
	}

	// ---------- getWinner ----------

	@Test
	public void getWinnerReturnsValueInAfterFight() throws Exception {
		// Simple AI for both: do nothing, fight will run to MAX_TURNS, drawCheckLife=true compares life.
		// They start with equal life so it should be a draw → -1.
		attachAI(leek1, "function afterFight() { setRegister('w', '' + getWinner()); }");
		attachAI(leek2, "");
		runFight();
		String winner = leek1.getRegister("w");
		// Since fights without weapons typically end in a draw with equal HP, winner = -1
		Assert.assertNotNull(winner);
		Assert.assertTrue("getWinner returned: " + winner, winner.equals("-1") || winner.equals("0") || winner.equals("1"));
	}

	@Test
	public void getWinnerInBeforeFightReturnsMinusOne() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('w_pre', '' + getWinner()); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("-1", leek1.getRegister("w_pre"));
	}

	// ---------- Info-masking during beforeFight ----------

	@Test
	public void beforeFightCannotReadOpponentStrength() throws Exception {
		attachAI(leek1, "function beforeFight() {"
			+ "  var enemies = getEnemies();"
			+ "  var f = getForce(enemies[0]);"
			+ "  setRegister('opp_str', f == null ? 'null' : '' + f);"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("null", leek1.getRegister("opp_str"));
	}

	@Test
	public void beforeFightCanReadOwnStrength() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('my_str', '' + getStrength()); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("100", leek1.getRegister("my_str"));
	}

	@Test
	public void beforeFightCanReadOpponentNameLevelCell() throws Exception {
		// Public info should remain visible during beforeFight
		attachAI(leek1, "function beforeFight() {"
			+ "  var enemies = getEnemies();"
			+ "  setRegister('opp_name', getName(enemies[0]));"
			+ "  setRegister('opp_level', '' + getLevel(enemies[0]));"
			+ "  setRegister('opp_cell_null', getCell(enemies[0]) == null ? 'true' : 'false');"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("L2", leek1.getRegister("opp_name"));
		Assert.assertEquals("10", leek1.getRegister("opp_level"));
		Assert.assertEquals("false", leek1.getRegister("opp_cell_null"));
	}

	@Test
	public void afterFightCanReadOpponentStrength() throws Exception {
		// Masking is only during beforeFight (symmetry of execution), not afterFight.
		attachAI(leek1, "function afterFight() { var enemies = getEnemies(); setRegister('opp_str', '' + getForce(enemies[0])); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("100", leek1.getRegister("opp_str"));
	}

	@Test
	public void sayAndLamaAreDeniedInHooks() throws Exception {
		// say() / lama() consume TP and emit an entity-less action that the report
		// attributes to the current-turn entity. During a hook no turn is active, so
		// the action would be misattributed (leak to the last-played leek) and freeze
		// the client report (entity undefined). They must be denied, like useChip etc.
		attachAI(leek1,
			"function afterFight() {"
			+ "  setRegister('say_result', say('gg') ? 'true' : 'false');"
			+ "  setRegister('lama_result', '' + lama());"
			+ "  setRegister('reached_end', '1');"  // execution continues after the denied calls
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("false", leek1.getRegister("say_result"));
		Assert.assertEquals("null", leek1.getRegister("lama_result"));
		Assert.assertEquals("1", leek1.getRegister("reached_end"));
		// No SAY (203) / LAMA (204) action must have leaked into the report stream.
		String actions = fight.getState().getActions().toJSON().toString();
		Assert.assertFalse("No SAY action expected from a hook", actions.contains("[203,"));
	}

	// ---------- Hook robustness (errors / ops) ----------

	@Test
	public void beforeFightExceptionDoesNotCrashFight() throws Exception {
		// AI throws null pointer in beforeFight — fight should still run.
		attachAI(leek1, "function beforeFight() { var x = null; var y = x[0]; }"
			+ "setRegister('turn1_ran', '1');");
		attachAI(leek2, "");
		runFight();
		// Turn 1 main code still runs even though hook failed
		Assert.assertEquals("1", leek1.getRegister("turn1_ran"));
	}

	@Test
	public void afterFightExceptionDoesNotPreventOtherEntities() throws Exception {
		attachAI(leek1, "function afterFight() { var x = null; var y = x[0]; }");
		attachAI(leek2, "function afterFight() { setRegister('reached', '1'); }");
		runFight();
		Assert.assertEquals("1", leek2.getRegister("reached"));
	}

	@Test
	public void beforeFightInfiniteLoopHitsOpsLimit() throws Exception {
		// Beforefight gets turn-1 ops + 1M bonus. An infinite loop should hit the limit
		// and return without crashing the fight — turn 1 main code should still run.
		attachAI(leek1, "function beforeFight() { while (true) { var x = 1; } }"
			+ "setRegister('turn1_ran', '1');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1", leek1.getRegister("turn1_ran"));
	}

	// ---------- Cross-entity + state integrity ----------

	@Test
	public void registersFromHooksPersistToManager() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('persisted', 'yes'); }");
		attachAI(leek2, "");
		runFight();
		// After the fight, register manager should hold the value
		Assert.assertNotNull("Register manager should have stored leek1 registers", registerStore.get(leek1.getId()));
		Assert.assertTrue("Stored JSON should contain 'persisted'", registerStore.get(leek1.getId()).contains("persisted"));
	}

	// ---------- afterFight() for leeks that died during the fight (issue #4170) ----------

	@Test
	public void afterFightRunsForLeekThatDiedDuringFight() throws Exception {
		// Regression #4170: a leek that dies mid-fight is removed from the turn order
		// (state.getOrder()), yet afterFight() must still run for it, otherwise its
		// registers are never saved, its debug is dropped, and the hook can even run
		// for the wrong leek. Iterating the initial boot order (which retains dead
		// entities) fixes this.
		attachAI(leek1, "function afterFight() { setRegister('ran', 'leek1'); }");
		attachAI(leek2, "function afterFight() { setRegister('ran', 'leek2'); }");

		// Lethal poison applied before the fight starts: it survives initFight (which
		// does not clear entity effects) and kills leek1 on its first turn. The value
		// is amplified by the caster's power (see EffectPoison.apply), so 300 becomes
		// 600 >= 500 life.
		Effect.createEffect(fight.getState(), Effect.TYPE_POISON, 10, 1, 300, 300, false,
			leek1, leek2, null, 0, false, 0, 1, 0, 0);

		runFight();

		Assert.assertTrue("leek1 should have died during the fight", leek1.isDead());
		Assert.assertEquals("afterFight() must still run for the dead leek", "leek1", leek1.getRegister("ran"));
		Assert.assertEquals("leek2's afterFight() ran on leek2", "leek2", leek2.getRegister("ran"));
	}























	@Test
	public void hookSeesCurrentFightContext() throws Exception {
		// getFightType / getFightContext should work in beforeFight (they don't depend on opponent equipment)
		attachAI(leek1, "function beforeFight() {"
			+ "  setRegister('type', '' + getFightType());"
			+ "  setRegister('context', '' + getFightContext());"
			+ "}");
		attachAI(leek2, "");
		runFight();
		// Default state.type and state.context — both 0 in this test setup
		Assert.assertNotNull(leek1.getRegister("type"));
		Assert.assertNotNull(leek1.getRegister("context"));
	}
}
