package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Ignore;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.state.FightLoadout;
import com.leekwars.generator.test.LocalTrophyManager;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.weapons.Weapon;
import com.leekwars.generator.weapons.Weapons;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * Shared scaffolding for fight tests. Subclasses get a Generator, a Fight with two
 * teams, two leeks (configurable via {@link #createLeeks()}), an in-memory
 * RegisterManager, and helpers to attach AI snippets via AIFile + run the fight.
 *
 * Tests that only probe Java-side APIs should call {@link #initFightOnly()} (skips
 * the 64-turn loop). Tests that need AI execution call {@link #runFight()}.
 */
@Ignore
public abstract class FightTestBase {

	// Single shared counter ensures unique AIFile paths across the entire test suite —
	// avoids JVM class name collisions when several tests compile within the same ms.
	private static final AtomicLong AI_COUNTER = new AtomicLong(10_000_000);

	protected Generator generator;
	protected Fight fight;
	protected FarmerLog farmerLog;
	protected final HashMap<Integer, String> registerStore = new HashMap<>();

	@Before
	public void setUp() {
		generator = new Generator();
		fight = new Fight(generator);
		fight.getState().setRegisterManager(new RegisterManager() {
			@Override public String getRegisters(int leek) { return registerStore.get(leek); }
			@Override public void saveRegisters(int leek, String registers, boolean is_new) { registerStore.put(leek, registers); }
		});
		fight.setStatisticsManager(new LocalTrophyManager());
		createLeeks();
		farmerLog = new FarmerLog(fight, 0);
		// Tous les tests poireaux ont farmer=0 ; on donne un pool large par défaut pour
		// que setLoadout(name) n'échoue pas faute de potion. Les tests qui veulent
		// vérifier le comportement "pas de potion" peuvent override à 0.
		fight.getState().setRestatPotionsAvailable(0, 999);
	}

	/** Subclasses populate fight teams here. Default: 1v1 with two stock leeks. */
	protected abstract void createLeeks();

	/**
	 * Cores must be > 0 (maxOperations = cores * 1M) — with 0 cores the very first
	 * `ops()` call past the budget throws TOO_MUCH_OPERATIONS, so only the first
	 * AI instruction succeeds per turn.
	 */
	protected static Leek defaultLeek(int id, String name) {
		return new Leek(id, name, 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 8, 30, 0, false, 0, 0, "", 0, "", "", "", 0);
	}

	protected void attachAI(Leek leek, String code) {
		long uid = AI_COUNTER.incrementAndGet();
		AIFile file = new AIFile("<test_" + uid + ">", code, System.currentTimeMillis(),
			LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	/** Runs the full fight (initFight + turn loop + finishFight + register save). */
	protected void runFight() throws Exception {
		fight.startFight(true);
		// Mirror Generator.runScenario register save loop — production persists modified
		// registers to the manager at fight end.
		var rm = fight.getState().getRegisterManager();
		for (var entity : fight.getState().getEntities().values()) {
			if (!entity.isSummon() && entity.getRegisters() != null
				&& (entity.getRegisters().isModified() || entity.getRegisters().isNew())) {
				rm.saveRegisters(entity.getId(), entity.getRegisters().toJSONString(), entity.getRegisters().isNew());
			}
		}
	}

	/**
	 * Initializes the fight state without running turns. Use for tests that only
	 * probe Java-side APIs (removeLife, applyEffect, etc.) — saves 64 turns of AI
	 * execution per test.
	 */
	protected void initFightOnly() throws Exception {
		fight.initFight();
	}

	/**
	 * Register a synthetic Weapon for tests. Idempotent: returns the existing
	 * registration when the id is already known. Use ids in the high range (≥ 88000)
	 * to avoid collisions with the real catalog.
	 */
	protected static Weapon registerWeapon(int id, String name, boolean forgotten) {
		Weapon existing = Weapons.getWeapon(id);
		if (existing != null) return existing;
		Weapon w = new Weapon(
			id, 5, 1, 6, Json.createArray(),
			(byte) 1, (byte) 1, true, id, name, Json.createArray(), 0, forgotten);
		Weapons.addWeapon(w);
		return w;
	}

	/** Empty FightLoadout — clears equipment when applied. */
	protected static FightLoadout emptyLoadout(String name) {
		return new FightLoadout(name,
			new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
			new HashMap<>());
	}
}
