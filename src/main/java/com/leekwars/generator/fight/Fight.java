package com.leekwars.generator.fight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.action.Action;
import com.leekwars.generator.action.ActionAIError;
import com.leekwars.generator.action.ActionEndTurn;
import com.leekwars.generator.action.ActionEntityTurn;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.Order;
import com.leekwars.generator.state.State;
import com.leekwars.generator.fight.entity.BulbAI;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.runner.values.FunctionLeekValue;

public class Fight {

	public final static String TAG = Fight.class.getSimpleName();

	// Shared constants are sourced from State (single source of truth).
	// Re-exported here for the natives that reference Fight.X publicly.
	public final static int MAX_TURNS = State.MAX_TURNS;
	public static final int TYPE_SOLO_GARDEN = State.TYPE_SOLO_GARDEN;
	public static final int TYPE_SOLO_TEST = State.TYPE_SOLO_TEST;
	public static final int TYPE_NORMAL_WHAT = State.TYPE_NORMAL_WHAT;
	public static final int TYPE_TEAM_GARDEN = State.TYPE_TEAM_GARDEN;
	public static final int TYPE_SOLO_CHALLENGE = State.TYPE_SOLO_CHALLENGE;
	public static final int TYPE_FARMER_GARDEN = State.TYPE_FARMER_GARDEN;
	public static final int TYPE_SOLO_TOURNAMENT = State.TYPE_SOLO_TOURNAMENT;
	public static final int TYPE_TEAM_TEST = State.TYPE_TEAM_TEST;
	public static final int TYPE_FARMER_TOURNAMENT = State.TYPE_FARMER_TOURNAMENT;
	public static final int TYPE_TEAM_TOURNAMENT = State.TYPE_TEAM_TOURNAMENT;
	public static final int TYPE_FARMER_CHALLENGE = State.TYPE_FARMER_CHALLENGE;
	public static final int TYPE_FARMER_TEST = State.TYPE_FARMER_TEST;
	public static final int FULL_TYPE_BATTLE_ROYALE = State.FULL_TYPE_BATTLE_ROYALE;
	public final static int CONTEXT_TEST = State.CONTEXT_TEST;
	public final static int CONTEXT_CHALLENGE = State.CONTEXT_CHALLENGE;
	public final static int CONTEXT_GARDEN = State.CONTEXT_GARDEN;
	public final static int CONTEXT_TOURNAMENT = State.CONTEXT_TOURNAMENT;
	public final static int CONTEXT_BATTLE_ROYALE = State.CONTEXT_BATTLE_ROYALE;
	public final static int TYPE_SOLO = State.TYPE_SOLO;
	public final static int TYPE_FARMER = State.TYPE_FARMER;
	public final static int TYPE_TEAM = State.TYPE_TEAM;
	public final static int TYPE_BATTLE_ROYALE = State.TYPE_BATTLE_ROYALE;
	public final static int TYPE_WAR = State.TYPE_WAR;
	public final static int TYPE_CHEST_HUNT = State.TYPE_CHEST_HUNT;
	public final static int TYPE_COLOSSUS = State.TYPE_COLOSSUS;
	public final static int SUMMON_LIMIT = State.SUMMON_LIMIT;

	// Fight-specific constants
	public final static int TYPE_BOSS = 4;
	public final static int FLAG_STATIC = 1;
	public final static int FLAG_PERFECT = 2;
	public final static int MAX_LOG_COUNT = 5000;

	private int mWinteam = -1;
	public Generator generator;
	private int mId;
	private int mBoss;
	private int mStartFarmer = -1;
	private int max_turns = MAX_TURNS;
	public long executionTime = 0;
	FightListener listener;
	private State state = new State();


	public Fight(Generator generator) {
		this(generator, null);
	}

	public Fight(Generator generator, FightListener listener) {

		this.generator = generator;
		this.listener = listener;
	}

	public void addFlag(int team, int flag) {
		state.getTeams().get(team).addFlag(flag);
	}

	public HashSet<Integer> getFlags(int team) {
		return state.getTeams().get(team).getFlags();
	}

	public int getWinner() {
		return mWinteam;
	}

	public void setTeamID(int team, int id) {
		if (team < state.getTeams().size()) {
			state.getTeams().get(team).setID(id);
		}
	}

	public void setStartFarmer(int startFarmer) {
		mStartFarmer = startFarmer;
	}

	public int getStartFarmer() {
		return mStartFarmer;
	}

	public int getTeamID(int team) {
		if (team < state.getTeams().size()) {
			return state.getTeams().get(team).getID();
		}
		return -1;
	}

	public int getId() {
		return mId;
	}

	public int getBoss() {
		return mBoss;
	}

	public void log(Action log) {
		state.getActions().log(log);
	}

	public List<Entity> getTeamLeeks(int team) {
		List<Entity> leeks = new ArrayList<Entity>();
		if (team < state.getTeams().size()) {
			for (Entity e : state.getTeams().get(team).getEntities()) {
				if (!e.isDead() && e.getType() == Entity.TYPE_LEEK)
					leeks.add(e);
			}
		}
		return leeks;
	}

	public List<Entity> getTeamEntities(int team) {
		return state.getTeams().get(team).getEntities();
	}

	public Entity getEntity(int id) {
		return state.getEntity(id);
	}

	public Entity getEntity(long id) {
		return state.getEntity(id);
	}

	private boolean canStartFight() {
		if (state.getTeams().size() < 2) return false;
		return true;
	}

	public void startFight(boolean drawCheckLife) throws Exception {

		initFight();

		for (var entity : state.getEntities().values()) {

			// Build AI after the fight is ready (static init)
			var ai = EntityAI.build(this.generator, (AIFile) entity.getAIFile(), entity);
			entity.setAI(ai);
			((EntityAI) ai).setFight(this);
			((EntityAI) ai).init();
			((EntityAI) ai).getRandom().seed(state.getSeed());

			// Check all entities characteristics
			state.statistics.init(entity);
			state.statistics.characteristics(entity);

			// Start fight for entity
			entity.startFight();
		}

		runHooks("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		// Snapshot of initial entity state (life, stats, equipment) sent to the client
		// is captured here so that any setLoadout() applied in beforeFight() is reflected
		// in the report's max-life / displayed stats.
		state.recordInitialState();

		Log.i(TAG, "Turn 1");

		// On lance les tours
		while (state.getOrder().getTurn() <= max_turns && state.getState() == State.STATE_RUNNING) {

			startTurn();

			if (state.getOrder().current() == null) {
				finishFight();
				break;
			}
		}
		// Si match nul
		if (state.getOrder().getTurn() == Fight.MAX_TURNS + 1) {
			finishFight();
		}

		// On supprime toutes les invocations
		var entities = state.getAllEntities(true);
		for (Entity e : entities) {
			if (e.isSummon())
				state.removeInvocation(e, true);
		}

		// Calcul de l'équipe gagnante
		computeWinner(drawCheckLife);

		runHooks("afterFight", EntityAI.HookPhase.AFTER_FIGHT);

		state.statistics.endFight(state.getEntities().values());
		if (listener != null) {
			listener.newTurn(this);
		}
		state.getActions().addOpsAndTimes(state.statistics);
	}

	public void computeWinner(boolean drawCheckLife) {
		mWinteam = -1;

		if (state.getType() == State.TYPE_CHEST_HUNT) {
			// Chest hunt (free-for-all): all alive players win if all chests are dead
			boolean chestsAlive = false;
			for (var team : state.getTeams()) {
				if (team.containsChest() && team.isAlive()) { chestsAlive = true; break; }
			}
			if (!chestsAlive) {
				mWinteam = -2; // Special: all alive players win
			}
			return;
		}

		int alive = 0;
		for (int t = 0; t < state.getTeams().size(); ++t) {
			if (!state.getTeams().get(t).isDead() && !state.getTeams().get(t).containsChest()) {
				alive++;
				mWinteam = t;
			}
		}
		if (alive != 1) {
			mWinteam = -1;
		}
		// Égalité : on regarde la vie des équipes
		if (mWinteam == -1 && drawCheckLife) {
			// Find the team with the strictly highest life among all teams. This generalizes
			// the 1v1 tiebreaker to BR / multi-team fights — the previous implementation only
			// compared teams 0 and 1, leaving teams 2+ unable to win on a life tiebreaker.
			int maxLife = Integer.MIN_VALUE;
			int winners = 0;
			int candidate = -1;
			for (int t = 0; t < state.getTeams().size(); ++t) {
				int life = state.getTeams().get(t).getLife();
				if (life > maxLife) {
					maxLife = life;
					candidate = t;
					winners = 1;
				} else if (life == maxLife) {
					winners++;
				}
			}
			if (winners == 1) {
				mWinteam = candidate;
			}
		}
	}

	public void initFight() throws FightException {

		if (state.getTeams().size() < 2) {
			throw new FightException(FightException.NOT_ENOUGHT_PLAYERS);
		}
		if (state.getTeams().get(0).size() == 0 || state.getTeams().get(1).size() == 0) {
			if (state.getContext() == Fight.CONTEXT_TOURNAMENT) {
				// Si c'est un tournoi faut un gagnant
				if (state.getTeams().get(0).size() == 0) {
					mWinteam = 1;
				} else {
					mWinteam = 0;
				}
			}
			throw new FightException(FightException.NOT_ENOUGHT_PLAYERS);
		}

		if (!canStartFight()) {
			throw new FightException(FightException.CANT_START_FIGHT);
		}

		// Init the state
		this.state.init();
	}

	public void finishFight() {
		state.setState(State.STATE_FINISHED);
	}

	private void runHooks(String name, EntityAI.HookPhase phase) {
		// Iterate in turn order (deterministic) rather than state.getEntities()
		// (HashMap, non-deterministic).
		for (var entity : state.getOrder().getEntities()) {
			var ai = entity.getAI();
			if (!(ai instanceof EntityAI)) continue;
			var entityAI = (EntityAI) ai;
			if (!entityAI.isValid()) continue;
			if (!entityAI.hasHook(name)) continue;
			entityAI.setEntity(entity);
			entityAI.runHook(name, phase);
		}
	}

	public void startTurn() throws Exception {

		var current = state.getOrder().current();
		if (current == null) {
			return;
		}

		state.getActions().log(new ActionEntityTurn(current));
		if (listener != null) {
			listener.newTurn(this);
		}
		// Log.i(TAG, "Start turn of " + current.getName());

		current.startTurn();

		if (!current.isDead()) {

			var ai = (EntityAI) current.getAI();
			if (ai != null) {
				if (ai.isValid()) {
					ai.setEntity(current);

					// System.out.println("Run " + current.getName() + " ai...");
					long startTime = System.nanoTime();
					ai.runTurn(state.getOrder().getTurn());
					long endTime = System.nanoTime();

					state.statistics.addTimes(current, endTime - startTime, ai.operations());
					executionTime += endTime - startTime;
					current.addOperations(ai.operations());
				} else {
					// Add 'crash' action if AI is invalid
					if (getTurn() == 1) {
						((LeekLog) current.getLogs()).addSystemLog(LeekLog.SERROR, Error.INVALID_AI, new String[] { "Invalid AI" });
					}
					log(new ActionAIError(current));
					state.statistics.error(current);
				}
			} else {
				// Pas d'IA équipée : juste un warning
				((LeekLog) current.getLogs()).addSystemLog(LeekLog.SWARNING, Error.NO_AI_EQUIPPED);
			}
			current.endTurn();
			state.getActions().log(new ActionEndTurn(current));
		}
		state.endTurn();
	}

	public int useChip(Entity caster, Cell target, Chip template) {

		// Invocation mais sans IA
		if (template.getAttack().getEffectParametersByType(Effect.TYPE_SUMMON) != null) {
			((EntityAI) caster.getAI()).addSystemLog(LeekLog.WARNING, FarmerLog.BULB_WITHOUT_AI);
			((EntityAI) caster.getAI()).addSystemLog(LeekLog.STANDARD, Error.HELP_PAGE_LINK, new String[] { "summons" });
			return summonEntity(caster, target, template, null);
		}

		return state.useChip(caster, target, template);
	}

	public int summonEntity(Entity caster, Cell target, Chip template, FunctionLeekValue value) {
		return summonEntity(caster, target, template, value, null);
	}

	public int summonEntity(Entity caster, Cell target, Chip template, FunctionLeekValue value, String name) {

		int result = state.summonEntity(caster, target, template, name);

		// On assigne l'ia de l'invocation
		if (result > 0) {
			var summon = state.getLastEntity();
			summon.setFight(this);
			summon.setBirthTurn(getTurn());
			summon.setAI(new BulbAI(summon, (EntityAI) caster.getAI(), value));
		}

		return result;
	}

	public boolean generateCritical(Entity caster) {
		return state.getRandom().getDouble() < ((double) caster.getAgility() / 1000);
	}

	public int getTurn() {
		return state.getOrder().getTurn();
	}

	public Order getOrder() {
		return state.getOrder();
	}

	public void setId(int f) {
		mId = f;
	}

	public void setBoss(int b) {
		mBoss = b;
	}

	public static int getFightContext(int type) {
		if (type == TYPE_SOLO_GARDEN || type == TYPE_TEAM_GARDEN || type == TYPE_FARMER_GARDEN) {
			return CONTEXT_GARDEN;
		} else if (type == TYPE_SOLO_TEST || type == TYPE_TEAM_TEST || type == TYPE_FARMER_TEST) {
			return CONTEXT_TEST;
		} else if (type == TYPE_TEAM_TOURNAMENT || type == TYPE_SOLO_TOURNAMENT || type == TYPE_FARMER_TOURNAMENT) {
			return CONTEXT_TOURNAMENT;
		} else if (type == FULL_TYPE_BATTLE_ROYALE) {
			return CONTEXT_BATTLE_ROYALE;
		}
		return CONTEXT_CHALLENGE;
	}

	public static int getFightType(int type) {
		if (type == TYPE_SOLO_GARDEN || type == TYPE_SOLO_CHALLENGE || type == TYPE_SOLO_TOURNAMENT || type == TYPE_SOLO_TEST) {
			return Fight.TYPE_SOLO;
		} else if (type == TYPE_FARMER_GARDEN || type == TYPE_FARMER_TOURNAMENT || type == TYPE_FARMER_CHALLENGE || type == TYPE_FARMER_TEST) {
			return Fight.TYPE_FARMER;
		} else if (type == FULL_TYPE_BATTLE_ROYALE) {
			return Fight.TYPE_BATTLE_ROYALE;
		}
		return Fight.TYPE_TEAM;
	}

	public void setMaxTurns(int max_turns) {
		this.max_turns = max_turns;
	}

	public int getDuration() {
		return state.getOrder().getTurn();
	}

	public State getState() {
		return this.state;
	}

	public void setStatisticsManager(StatisticsManager statisticsManager) {
		this.state.setStatisticsManager(statisticsManager);
		statisticsManager.setGeneratorFight(this);
	}
}
