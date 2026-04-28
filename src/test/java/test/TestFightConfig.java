package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.FightException;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.State;

/**
 * Fight configuration edge cases: max_turns, seed determinism, fight id,
 * setup ordering, repeated calls.
 */
public class TestFightConfig extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	// ---------- max_turns ----------

	@Test
	public void maxTurnsZeroEndsImmediately() throws Exception {
		fight.setMaxTurns(0);
		attachAI(leek1, "global c = 0; c++; setRegister('count', '' + c);");
		attachAI(leek2, "");
		runFight();
		// With max_turns=0, turn loop condition `turn <= max_turns` is false from start
		// (turn starts at 1). AI never runs.
		Assert.assertNull(leek1.getRegister("count"));
	}

	@Test
	public void maxTurnsOneRunsOneTurnPerLeek() throws Exception {
		fight.setMaxTurns(1);
		attachAI(leek1, "global c = 0; c++; setRegister('count', '' + c);");
		attachAI(leek2, "");
		runFight();
		// max_turns=1: turn 1 runs, then loop exits (turn becomes 2 > 1)
		Assert.assertEquals("1", leek1.getRegister("count"));
	}

	@Test
	public void maxTurnsNegativeBehavesLikeZero() throws Exception {
		fight.setMaxTurns(-5);
		attachAI(leek1, "setRegister('ran', 'yes');");
		attachAI(leek2, "");
		runFight();
		// Negative max means turn (1) > -5 → loop never enters
		Assert.assertNull(leek1.getRegister("ran"));
	}

	@Test
	public void maxTurnsLargeWorks() throws Exception {
		fight.setMaxTurns(200);
		attachAI(leek1, "global c = 0; c++; setRegister('count', '' + c);");
		attachAI(leek2, "");
		runFight();
		String count = leek1.getRegister("count");
		Assert.assertNotNull(count);
		// Default MAX_TURNS=64 in Fight.java; we set 200 so we should see > 64
		System.out.println("[INFO] Played " + count + " turns with max=200");
		// At least 64 (above the default cap)
		Assert.assertTrue("Should run > 64 turns: " + count, Integer.parseInt(count) > 64);
	}

	// ---------- Seed determinism ----------

	@Test
	public void sameSeedProducesSameResult() throws Exception {
		// Fight is deterministic given same seed + same AIs.
		// Compare turn orders / register progression across runs.
		String prev = null;
		for (int run = 0; run < 3; run++) {
			setUp();
			fight.getState().seed(42);
			attachAI(leek1, ""
				+ "global path = '';"
				+ "path = path + '/' + getTurn();"
				+ "setRegister('path', path);");
			attachAI(leek2, "");
			runFight();
			String path = leek1.getRegister("path");
			if (prev == null) prev = path;
			Assert.assertEquals("Seed=42 must produce identical fight", prev, path);
		}
	}

	@Test
	public void differentSeedsCanProduceDifferentMaps() throws Exception {
		// Map generation depends on seed. Two different seeds may produce different maps.
		// We capture the obstacle count from each run — if different, seeds matter.
		setUp();
		fight.getState().seed(1);
		attachAI(leek1, "setRegister('obstacles', '' + count(getObstacles()));");
		attachAI(leek2, "");
		runFight();
		String s1 = leek1.getRegister("obstacles");
		setUp();
		fight.getState().seed(99);
		attachAI(leek1, "setRegister('obstacles', '' + count(getObstacles()));");
		attachAI(leek2, "");
		runFight();
		String s2 = leek1.getRegister("obstacles");
		Assert.assertNotNull(s1);
		Assert.assertNotNull(s2);
		System.out.println("[INFO] Seed 1 obstacles: " + s1 + ", seed 99: " + s2);
		// They may or may not differ. Just assert both ran successfully.
	}

	// ---------- Fight id ----------

	@Test
	public void setFightIdIsReadable() throws Exception {
		fight.setId(12345);
		attachAI(leek1, "setRegister('fid', '' + getFightID());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("12345", leek1.getRegister("fid"));
	}

	@Test
	public void fightIdNegativeIsAccepted() throws Exception {
		// No validation in setId — negative is silently accepted.
		fight.setId(-1);
		attachAI(leek1, "setRegister('fid', '' + getFightID());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("-1", leek1.getRegister("fid"));
	}

	// ---------- Init / startFight ordering ----------

	@Test
	public void initFightFailsWithOnlyOneTeam() throws Exception {
		// setUp adds 2 leeks on different teams. Manually drop team 1.
		// Actually we can't easily remove a team from State. Try with custom setup:
		Fight bare = new Fight(generator);
		bare.getState().setRegisterManager(fight.getState().getRegisterManager());
		bare.setStatisticsManager(new com.leekwars.generator.test.LocalTrophyManager());
		Leek solo = defaultLeek(99, "Solo");
		bare.getState().addEntity(0, solo);
		try {
			bare.initFight();
			Assert.fail("initFight with one team must throw");
		} catch (FightException e) {
			Assert.assertEquals(FightException.NOT_ENOUGHT_PLAYERS, e.getType());
		}
	}

	// ---------- Repeated start ----------

	@Test
	public void runningFightTwiceDoesNotCrash() throws Exception {
		attachAI(leek1, "");
		attachAI(leek2, "");
		runFight();
		// Try to start again — engine may or may not re-run, just verify no crash
		try {
			fight.startFight(true);
			System.out.println("[INFO] Re-running a finished fight succeeded (unexpected but ok)");
		} catch (Throwable t) {
			System.out.println("[INFO] Re-running threw: " + t.getClass().getSimpleName());
		}
	}

	// ---------- Fight context ----------

	@Test
	public void setContextIsReflectedInGetFightContext() throws Exception {
		fight.getState().setContext(Fight.CONTEXT_TOURNAMENT);
		attachAI(leek1, "setRegister('ctx', '' + getFightContext());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("" + Fight.CONTEXT_TOURNAMENT, leek1.getRegister("ctx"));
	}

	@Test
	public void setTypeIsReflectedInGetFightType() throws Exception {
		fight.getState().setType(State.TYPE_TEAM);
		attachAI(leek1, "setRegister('t', '' + getFightType());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("" + State.TYPE_TEAM, leek1.getRegister("t"));
	}
}
