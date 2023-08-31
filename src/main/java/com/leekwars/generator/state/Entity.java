package com.leekwars.generator.state;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.leekwars.generator.action.ActionRemoveEffect;
import com.leekwars.generator.action.ActionUpdateEffect;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.effect.EffectParameters;
import com.leekwars.generator.effect.EffectPoison;
import com.leekwars.generator.effect.EffectShackleAgility;
import com.leekwars.generator.effect.EffectShackleMP;
import com.leekwars.generator.effect.EffectShackleMagic;
import com.leekwars.generator.effect.EffectShackleStrength;
import com.leekwars.generator.effect.EffectShackleTP;
import com.leekwars.generator.effect.EffectShackleWisdom;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.Registers;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;
import com.leekwars.generator.weapons.Weapon;

public abstract class Entity {

	public static final int SAY_LIMIT_TURN = 2;
	public static final int SHOW_LIMIT_TURN = 5;

	public static final int TYPE_LEEK = 0;
	public static final int TYPE_BULB = 1;
	public static final int TYPE_TURRET = 2;
	public static final int TYPE_CHEST = 3;
	public static final int TYPE_MOB = 4;

	// Characteristics constants
	public final static int CHARAC_LIFE = 0;
	public final static int CHARAC_TP = 1;
	public final static int CHARAC_MP = 2;
	public final static int CHARAC_STRENGTH = 3;
	public final static int CHARAC_AGILITY = 4;
	public final static int CHARAC_FREQUENCY = 5;
	public final static int CHARAC_WISDOM = 6;
	public final static int CHARAC_ABSOLUTE_SHIELD = 9;
	public final static int CHARAC_RELATIVE_SHIELD = 10;
	public final static int CHARAC_RESISTANCE = 11;
	public final static int CHARAC_SCIENCE = 12;
	public final static int CHARAC_MAGIC = 13;
	public final static int CHARAC_DAMAGE_RETURN = 14;
	public final static int CHARAC_POWER = 15;

	// Characteristics
	protected Cell cell;
	protected final Stats mBaseStats;
	protected final Stats mBuffStats;
	protected String name;
	protected int mId;
	protected int mFarmer;
	protected int mLevel;
	protected int mSkin;
	protected int mHat;
	protected boolean mMetal;
	protected int mFace;
	protected String mFarmerName = "";
	protected String mFarmerCountry;
	protected String mTeamName = "";
	protected String mAIName;
	protected int mTeamId;
	protected int mAIId;
	protected int mTotalLife;
	protected int mInitialLife;
	protected boolean mStatic;
	protected int resurrected = 0;
	protected long totalOperations = 0;
	public int saysTurn = 0;
	public int showsTurn = 0;
	protected int mBirthTurn = 1;

	// Current effects on the entity
	protected final ArrayList<Effect> effects = new ArrayList<Effect>();

	// Effects created by the entity
	private final ArrayList<Effect> launchedEffects = new ArrayList<Effect>();

	// Passive effects
	private ArrayList<EffectParameters> passiveEffects = new ArrayList<EffectParameters>();

	// Current cooldowns of the entity
	protected Map<Integer, Integer> mCooldown = new TreeMap<Integer, Integer>();

	private Set<EntityState> states = new HashSet<EntityState>();

	protected int team;

	public State state;

	private TreeMap<Integer, Chip> mChips = new TreeMap<Integer, Chip>();

	private List<Weapon> mWeapons = null;
	private Weapon weapon = null;

	private int usedTP;
	private int usedMP;
	private int life;

	private Registers mRegister = null;

	private boolean mHasMoved = false;
	private int fight_id;

	private Object ai;
	private Object logs;
	private Object fight;
	private Object aiFile;

	public Entity() {
		this(0, "");
	}

	public Entity(Integer id, String name, int farmer, int level, int life, int turn_point, int move_point, int force, int agility, int frequency, int wisdom, int resistance, int science, int magic, int skin, boolean metal, int face, int team_id, String team_name, int ai_id, String ai_name, String farmer_name, String farmer_country, int hat) {

		mId = id;
		this.name = name;
		mLevel = level;
		mFarmer = farmer;
		mSkin = skin;
		mHat = hat;
		mMetal = metal;
		mFace = face;

		mBuffStats = new Stats();
		mBaseStats = new Stats();
		mBaseStats.setStat(CHARAC_LIFE, life);
		mBaseStats.setStat(CHARAC_TP, turn_point);
		mBaseStats.setStat(CHARAC_MP, move_point);
		mBaseStats.setStat(CHARAC_STRENGTH, force);
		mBaseStats.setStat(CHARAC_AGILITY, agility);
		mBaseStats.setStat(CHARAC_FREQUENCY, frequency);
		mBaseStats.setStat(CHARAC_WISDOM, wisdom);
		mBaseStats.setStat(CHARAC_RESISTANCE, resistance);
		mBaseStats.setStat(CHARAC_SCIENCE, science);
		mBaseStats.setStat(CHARAC_MAGIC, magic);

		mTotalLife = mBaseStats.getStat(CHARAC_LIFE);
		mInitialLife = mTotalLife;
		this.life = mTotalLife;

		mWeapons = new ArrayList<Weapon>();

		mTeamName = team_name;
		mTeamId = team_id;
		mFarmerName = farmer_name;
		mFarmerCountry = farmer_country;
		mAIName = ai_name;
		mAIId = ai_id;

		endTurn();
	}

	public Entity(Integer id, String name) {
		mId = id;
		this.name = name;
		mLevel = 1;
		mFarmer = 0;
		mSkin = 0;
		mHat = -1;

		mBuffStats = new Stats();
		mBaseStats = new Stats();
		mBaseStats.setStat(CHARAC_LIFE, 0);
		mBaseStats.setStat(CHARAC_TP, 0);
		mBaseStats.setStat(CHARAC_MP, 0);
		mBaseStats.setStat(CHARAC_STRENGTH, 0);
		mBaseStats.setStat(CHARAC_AGILITY, 0);
		mBaseStats.setStat(CHARAC_FREQUENCY, 0);
		mBaseStats.setStat(CHARAC_WISDOM, 0);
		mBaseStats.setStat(CHARAC_RESISTANCE, 0);
		mBaseStats.setStat(CHARAC_SCIENCE, 0);
		mBaseStats.setStat(CHARAC_MAGIC, 0);

		mTotalLife = mBaseStats.getStat(CHARAC_LIFE);
		this.life = mTotalLife;

		mWeapons = new ArrayList<Weapon>();

		endTurn();
	}

	public Entity(Entity entity) {

		mId = entity.getId();
		this.fight_id = entity.fight_id;
		name = entity.getName();
		team = entity.getTeam();
		mLevel = entity.mLevel;
		mFarmer = entity.getFarmer();
		mBuffStats = new Stats(entity.mBuffStats);
		mBaseStats = new Stats(entity.mBaseStats);
		mInitialLife = entity.mInitialLife;
		mTotalLife = entity.mTotalLife;
		mStatic = entity.mStatic;
		saysTurn = entity.saysTurn;
		showsTurn = entity.showsTurn;
		resurrected = entity.resurrected;
		this.cell = entity.cell;
		this.life = entity.getLife();
		mWeapons = entity.mWeapons; // immutable
		mChips = entity.mChips; // immutable
		this.weapon = entity.weapon;
		this.mCooldown = new TreeMap<Integer, Integer>(entity.mCooldown);
		this.usedTP = entity.usedTP;
		this.usedMP = entity.usedMP;
		this.passiveEffects = entity.passiveEffects; // immutable

		// protected final ArrayList<Effect> effects = new ArrayList<Effect>();
		// private final ArrayList<Effect> launchedEffects = new ArrayList<Effect>();
	}

	public Leek getLeek() {
		return null;
	}

	public abstract int getType();

	public void setCell(Cell cell) {
		this.cell = cell;
	}

	public void setRegisters(Registers registre) {
		mRegister = registre;
	}

	public Registers getRegisters() {
		return mRegister;
	}
	private void loadRegisters() {
		String v = state.getRegisterManager().getRegisters(getId());
		if (v == null) {
			mRegister = new Registers(true);
		} else {
			mRegister = Registers.fromJSONString(v);
		}
	}

	public String getRegister(String key) {
		if (mRegister == null) {
			loadRegisters();
		}
		return mRegister.get(key);
	}
	public Map<String, String> getAllRegisters() {
		if (mRegister == null) {
			loadRegisters();
		}
		return mRegister.getValues();
	}
	public boolean setRegister(String key, String value) {
		if (mRegister == null) {
			loadRegisters();
		}
		state.statistics.registerWrite(this, key, value);
		return mRegister.set(key, value);
	}
	public void deleteRegister(String key) {
		if (mRegister != null) {
			mRegister.delete(key);
		}
	}

	public int getHat() {
		return mHat;
	}

	public int getTeamId() {
		return mTeamId;
	}

	public String getTeamName() {
		return mTeamName;
	}

	public String getAIName() {
		return mAIName;
	}

	public int getAIId() {
		return mAIId;
	}

	public String getFarmerName() {
		return mFarmerName;
	}

	public String getFarmerCountry() {
		if (mFarmerCountry == null) {
			return "?";
		}
		return mFarmerCountry;
	}

	public void addWeapon(Weapon w) {
		mWeapons.add(w);
		passiveEffects.addAll(w.getPassiveEffects());
	}

	public Stats getBaseStats() {
		return mBaseStats;
	}

	public Cell getCell() {
		return cell;
	}

	/**
	 * Returns the id related to this fight
	 */
	public int getFId() {
		return fight_id;
	}
	/**
	 * Returns the real entity id
	 */
	public int getId() {
		return mId;
	}
	public void setId(int id) {
		mId = id;
	}
	public int getLevel() {
		return mLevel;
	}
	public int getLife() {
		return life;
	}

	public int getTotalLife() {
		return mTotalLife;
	}

	public void addTotalLife(int vitality, Entity caster) {
		mTotalLife += vitality;
		state.statistics.vitality(this, caster, vitality);
	}

	public void setTotalLife(int vitality) {
		mTotalLife = vitality;
		mInitialLife = life;
	}

	public int getInitialLife() {
		return mInitialLife;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public int getStat(int id) {
		return mBaseStats.getStat(id) + mBuffStats.getStat(id);
	}

	public int getStrength() {
		return getStat(Entity.CHARAC_STRENGTH);
	}

	public int getAgility() {
		return getStat(Entity.CHARAC_AGILITY);
	}

	public int getResistance() {
		return getStat(Entity.CHARAC_RESISTANCE);
	}

	public int getScience() {
		return getStat(Entity.CHARAC_SCIENCE);
	}

	public int getMagic() {
		return getStat(Entity.CHARAC_MAGIC);
	}

	public int getWisdom() {
		return getStat(Entity.CHARAC_WISDOM);
	}

	public int getRelativeShield() {
		return getStat(Entity.CHARAC_RELATIVE_SHIELD);
	}

	public int getAbsoluteShield() {
		return getStat(Entity.CHARAC_ABSOLUTE_SHIELD);
	}

	public int getDamageReturn() {
		return getStat(Entity.CHARAC_DAMAGE_RETURN);
	}

	public int getFrequency() {
		return getStat(Entity.CHARAC_FREQUENCY);
	}

	public int getTotalTP() {
		return getStat(Entity.CHARAC_TP);
	}

	public int getTotalMP() {
		return getStat(Entity.CHARAC_MP);
	}

	public int getMP() {
		return getTotalMP() - usedMP;
	}

	public int getTP() {
		return getTotalTP() - usedTP;
	}

	public int getPower() {
		return getStat(Entity.CHARAC_POWER);
	}

	public int getTeam() {
		return team;
	}

	public Weapon getWeapon() {
		return weapon;
	}

	public boolean hasWeapon(int id_tmp) {
		for (Weapon w : mWeapons) {
			if (w.getId() == id_tmp)
				return true;
		}
		return false;
	}

	public List<Weapon> getWeapons() {
		return mWeapons;
	}

	public boolean isDead() {
		return life <= 0;
	}

	public void removeLife(int pv, int erosion, Entity attacker, DamageType type, Effect effect) {

		if (isDead()) return;

		if (pv > life) {
			pv = life;
		}
		life -= pv;
		if (pv > 0) {
			state.statistics.damage(this, attacker, pv, type, effect);
		}
		if (erosion > 0) {
			state.statistics.damage(this, attacker, erosion, DamageType.NOVA, effect);
		}

		// Add erosion
		mTotalLife -= erosion;
		if (mTotalLife < 1) mTotalLife = 1;

		if (life <= 0) {
			state.onPlayerDie(this, attacker);
			die();
		}
	}

	public void onDirectDamage(int damage) {
		if (isDead()) return;
		for (Weapon weapon : mWeapons) {
			for (EffectParameters effect : weapon.getPassiveEffects()) {
				activateOnDamagePassiveEffect(effect, weapon.getAttack(), damage);
			}
		}
	}
	public void onNovaDamage(int damage) {
		if (isDead()) return;
		for (Weapon weapon : mWeapons) {
			for (EffectParameters effect : weapon.getPassiveEffects()) {
				activateOnNovaDamagePassiveEffect(effect, weapon.getAttack(), damage);
			}
		}
	}
	public void onPoisonDamage(int damage) {
		if (isDead()) return;
		for (Weapon weapon : mWeapons) {
			for (EffectParameters effect : weapon.getPassiveEffects()) {
				activateOnPoisonDamagePassiveEffect(effect, weapon.getAttack(), damage);
			}
		}
	}

	public void onMoved(Entity by) {
		if (isDead()) return;
		if (by == this) return; // Déplacement subi uniquement
		for (Weapon weapon : mWeapons) {
			for (EffectParameters effect : weapon.getPassiveEffects()) {
				activateOnMovedPassiveEffect(effect, weapon.getAttack());
			}
		}
	}

	public void onAllyKilled() {
		if (isDead()) return;
		for (Weapon weapon : mWeapons) {
			for (EffectParameters effect : weapon.getPassiveEffects()) {
				activateOnAllyKilledPassiveEffect(effect, weapon.getAttack());
			}
		}
	}

	public void onCritical() {
		if (isDead()) return;
		for (Weapon weapon : mWeapons) {
			for (EffectParameters effect : weapon.getPassiveEffects()) {
				activateOnCriticalPassiveEffect(effect, weapon.getAttack());
			}
		}
	}

	public void onKill() {
		if (isDead()) return;
		for (Weapon weapon : mWeapons) {
			for (EffectParameters effect : weapon.getPassiveEffects()) {
				activateOnKillPassiveEffect(effect, weapon.getAttack());
			}
		}
	}

	public void activateOnMovedPassiveEffect(EffectParameters effect, Attack attack) {
		if (effect.getId() == Effect.TYPE_MOVED_TO_MP) {
			double value = effect.getValue1();
			boolean stackable = (effect.getModifiers() & Effect.MODIFIER_STACKABLE) != 0;
			Effect.createEffect(this.state, Effect.TYPE_RAW_BUFF_MP, effect.getTurns(), 1, value, 0, false, this, this, attack, 0, stackable, 0, 1, 0, effect.getModifiers());
		}
	}

	public void activateOnDamagePassiveEffect(EffectParameters effect, Attack attack, int inputValue) {
		if (effect.getId() == Effect.TYPE_DAMAGE_TO_ABSOLUTE_SHIELD) {
			double value = inputValue * (effect.getValue1() / 100);
			boolean stackable = (effect.getModifiers() & Effect.MODIFIER_STACKABLE) != 0;
			Effect.createEffect(this.state, Effect.TYPE_RAW_ABSOLUTE_SHIELD, effect.getTurns(), 1, value, 0, false, this, this, attack, 0, stackable, 0, 0, 0, effect.getModifiers());
		}
		else if (effect.getId() == Effect.TYPE_DAMAGE_TO_STRENGTH) {
			double value = inputValue * (effect.getValue1() / 100);
			boolean stackable = (effect.getModifiers() & Effect.MODIFIER_STACKABLE) != 0;
			Effect.createEffect(this.state, Effect.TYPE_RAW_BUFF_STRENGTH, effect.getTurns(), 1, value, 0, false, this, this, attack, 0, stackable, 0, 0, 0, effect.getModifiers());
		}
	}

	public void activateOnNovaDamagePassiveEffect(EffectParameters effect, Attack attack, int inputValue) {
		if (effect.getId() == Effect.TYPE_NOVA_DAMAGE_TO_MAGIC) {
			double value = inputValue * (effect.getValue1() / 100);
			boolean stackable = (effect.getModifiers() & Effect.MODIFIER_STACKABLE) != 0;
			Effect.createEffect(this.state, Effect.TYPE_RAW_BUFF_MAGIC, effect.getTurns(), 1, value, 0, false, this, this, attack, 0, stackable, 0, 0, 0, effect.getModifiers());
		}
	}

	public void activateOnPoisonDamagePassiveEffect(EffectParameters effect, Attack attack, int inputValue) {
		if (effect.getId() == Effect.TYPE_POISON_TO_SCIENCE) {
			double value = inputValue * (effect.getValue1() / 100);
			boolean stackable = (effect.getModifiers() & Effect.MODIFIER_STACKABLE) != 0;
			Effect.createEffect(this.state, Effect.TYPE_RAW_BUFF_SCIENCE, effect.getTurns(), 1, value, 0, false, this, this, attack, 0, stackable, 0, 0, 0, effect.getModifiers());
		}
	}

	public void activateOnAllyKilledPassiveEffect(EffectParameters effect, Attack attack) {
		if (effect.getId() == Effect.TYPE_ALLY_KILLED_TO_AGILITY) {
			double value = effect.getValue1();
			boolean stackable = (effect.getModifiers() & Effect.MODIFIER_STACKABLE) != 0;
			Effect.createEffect(this.state, Effect.TYPE_RAW_BUFF_AGILITY, effect.getTurns(), 1, value, 0, false, this, this, attack, 0, stackable, 0, 0, 0, effect.getModifiers());
		}
	}

	public void activateOnCriticalPassiveEffect(EffectParameters effect, Attack attack) {
		if (effect.getId() == Effect.TYPE_CRITICAL_TO_HEAL) {
			if (this.getLife() < this.getTotalLife()) {
				double value1 = effect.getValue1();
				double value2 = effect.getValue2();
				double jet = state.getRandom().getDouble();
				Effect.createEffect(this.state, Effect.TYPE_RAW_HEAL, 0, 1, value1, value2, false, this, this, attack, jet, false, 0, 1, 0, effect.getModifiers());
			}
		}
	}

	public void activateOnKillPassiveEffect(EffectParameters effect, Attack attack) {
		if (effect.getId() == Effect.TYPE_KILL_TO_TP) {
			double value = effect.getValue1();
			Effect.createEffect(this.state, Effect.TYPE_RAW_BUFF_TP, effect.getTurns(), 1, value, value, false, this, this, attack, 0, true, 0, 1, 0, effect.getModifiers());
		}
	}

	public void addLife(Entity healer, int pv) {
		if (pv > getTotalLife() - life) {
			pv = getTotalLife() - life;
		}
		life += pv;
		state.statistics.heal(healer, this, pv);
		state.statistics.characteristics(this);
	}

	public void setTeam(int team) {
		this.team = team;
	}

	public void setWeapon(Weapon weapon) {
		this.weapon = weapon;
	}

	// At the start of his turn, decrease duration of his launched effects
	// and apply effects that affects the entity at the beginning of his turn
	// (poisons, ...)
	public void startTurn() {

		applyCoolDown();

		state.statistics.entityTurn(this);

		ArrayList<Effect> effectsCopy = new ArrayList<Effect>(this.effects);
		for (Effect effect : effectsCopy) {
			effect.applyStartTurn(state);
			if (isDead()) {
				return;
			}
		}

		for (int e = 0; e < launchedEffects.size(); ++e) {

			Effect effect = launchedEffects.get(e);

			if (effect.getTurns() != -1) { // Decrease duration
				effect.setTurns(effect.getTurns() - 1);
			}

			if (effect.getTurns() == 0) { // Effect done

				effect.getTarget().removeEffect(effect);
				launchedEffects.remove(e);
				e--;
			}
		}
	}

	// Restore TP and MP at the end of turn
	public void endTurn() {

		usedMP = 0;
		usedTP = 0;

		saysTurn = 0;
		showsTurn = 0;

		if (ai != null) {
			// totalOperations += ai.operations();
		}

		// Propagation des effets
		for (Effect effect : effects) {
			if (effect.propagate > 0) {
				Attack attack = effect.getAttack();
				EffectParameters propagation = attack.getEffects().get(0); // First effect is the propagation information
				EffectParameters original = attack.getEffects().get(1); // Second effect is the actual effect
				double jet = state.getRandom().getDouble();
				for (Entity target : getEntitiesAround(effect.propagate)) {
					if ((propagation.getModifiers() & Effect.MODIFIER_NOT_REPLACEABLE) != 0 && target.hasEffect(attack.getItemId())) {
						continue; // La cible a déjà l'effet et il n'est pas remplacable
					}
					Effect.createEffect(state, effect.getID(), original.getTurns(), 1, original.getValue1(), original.getValue2(), effect.isCritical(), target, effect.getCaster(), attack, jet, (propagation.getModifiers() & Effect.MODIFIER_STACKABLE) != 0, 0, 0, effect.propagate, effect.modifiers);
				}
			}
		}
	}

	public boolean hasEffect(int attackID) {
		for (Effect target_effect : effects) {
			if (target_effect.getAttack() != null && target_effect.getAttack().getItemId() == attackID) return true;
		}
		return false;
	}

	// When entity dies
	public void die() {

		life = 0;

		// Remove launched effects
		while (launchedEffects.size() > 0) {
			Effect effect = launchedEffects.get(0);
			effect.getTarget().removeEffect(effect);
			launchedEffects.remove(0);
		}

		// Remove effects
		while (effects.size() > 0) {
			var effect = effects.get(0);
			effect.getCaster().removeLaunchedEffect(effect);
			effects.remove(0); // Don't send remove effect action, the client will remove the effects itself
		}
		updateBuffStats(); // Update target stats

		// Kill summons
		List<Entity> entities = new ArrayList<Entity>(state.getTeamEntities(getTeam()));
		for (Entity e : entities) {
			if (e.isSummon() && e.getSummoner().getFId() == getFId()) {
				state.onPlayerDie(e, null);
				e.die();
			}
		}
	}

	public void updateBuffStats() {
		mBuffStats.clear();
		for (Effect effect : effects) {
			if (effect.getStats() != null)
				mBuffStats.addStats(effect.getStats());
		}
	}

	public void updateBuffStats(int id, int delta, Entity caster) {
		mBuffStats.updateStat(id, delta);
		state.statistics.characteristics(this);
		state.statistics.updateCharacteristic(this, id, delta, caster);
	}

	public void addEffect(Effect effect) {
		effects.add(effect);
	}

	/*
	 * Remove an active effect of the entity. Also add a fight log
	 */
	public void removeEffect(Effect effect) {

		state.log(new ActionRemoveEffect(effect.getLogID()));
		effects.remove(effect);

		updateBuffStats();
	}

	public void addLaunchedEffect(Effect effect) {
		launchedEffects.add(effect);
	}

	public void removeLaunchedEffect(Effect effect) {
		launchedEffects.remove(effect);
	}

	public void updateEffect(Effect effect) {
		state.log(new ActionUpdateEffect(effect.getLogID(), effect.value));
	}

	public void clearEffects() {

		for (int i = 0; i < effects.size(); ++i) {

			Effect effect = effects.get(i);

			effect.getCaster().removeLaunchedEffect(effect);
			removeEffect(effect);
			i--;
		}

		effects.clear();
	}

	public void reduceEffects(double percent, Entity caster) {
		for (int i = 0; i < effects.size(); ++i) {
			var effect = effects.get(i);
			// Irreductible effect? skip
			if ((effect.getModifiers() & Effect.MODIFIER_IRREDUCTIBLE) != 0) continue;

			effect.reduce(percent, caster);
			if (effect.value == 0) {
				removeEffect(effects.get(i));
				i--;
			} else {
				updateEffect(effects.get(i));
			}
		}
		updateBuffStats();
	}

	public void clearPoisons(Entity caster) {
		int poisonsRemoved = 0;
		for (int i = 0; i < effects.size(); ++i) {
			Effect effect = effects.get(i);
			if (effect instanceof EffectPoison) {
				effect.getCaster().removeLaunchedEffect(effect);
				removeEffect(effect);
				i--;
				poisonsRemoved += effect.getValue();
			}
		}
		state.statistics.antidote(this, caster, poisonsRemoved);
	}

	public void removeShackles() {
		for (int i = 0; i < effects.size(); ++i) {
			Effect effect = effects.get(i);
			if (effect instanceof EffectShackleTP || effect instanceof EffectShackleMP || effect instanceof EffectShackleAgility || effect instanceof EffectShackleMagic || effect instanceof EffectShackleStrength || effect instanceof EffectShackleWisdom) {
				effect.getCaster().removeLaunchedEffect(effect);
				removeEffect(effect);
				i--;
			}
		}
	}

	public void applyCoolDown() {
		Map<Integer, Integer> cooldown = new TreeMap<Integer, Integer>();
		cooldown.putAll(mCooldown);
		for (Entry<Integer, Integer> chip : cooldown.entrySet()) {
			if (chip.getValue() <= 1)
				mCooldown.remove(chip.getKey());
			else
				mCooldown.put(chip.getKey(), chip.getValue() - 1);
		}
	}

	public void addChip(Chip chip) {
		if (chip != null)
			mChips.put(chip.getId(), chip);
	}

	// Chip has just been used, we must store the cooldown (entity cooldown)
	public void addCooldown(Chip chip, int cooldown) {

		mCooldown.put(chip.getId(), cooldown == -1 ? State.MAX_TURNS + 2 : cooldown);
	}

	// Entity has cooldown for this chip?
	public boolean hasCooldown(int chipID) {
		return mCooldown.containsKey(chipID);
	}

	// Get current cooldown for a chip
	public int getCooldown(int chipID) {
		if (!hasCooldown(chipID)) {
			return 0;
		}
		return mCooldown.get(chipID);
	}

	public Map<Integer, Integer> getCooldowns() {
		return mCooldown;
	}

	public int getFarmer() {
		return mFarmer;
	}

	public Chip getChip(int id) {
		return mChips.get(id);
	}

	public List<Chip> getChips() {
		return new ArrayList<Chip>(mChips.values());
	}

	public int getSkin() {
		return mSkin;
	}

	public boolean getMetal() {
		return mMetal;
	}

	public int getFace() {
		return mFace;
	}

	public List<Effect> getEffects() {
		return effects;
	}

	public List<Effect> getLaunchedEffects() {
		return launchedEffects;
	}

	public List<EffectParameters> getPassiveEffects() {
		return passiveEffects;
	}

	public void setLevel(int level) {
		mLevel = level;
	}

	public void resurrect(Entity entity, double factor) {
		mTotalLife = Math.max(10, (int) Math.round(mTotalLife * 0.5 * factor));
		life = mTotalLife / 2;
		resurrected++;
		endTurn();
	}

	public void useTP(int tp) {
		usedTP += tp;
		state.statistics.useTP(tp);
	}

	public void useMP(int mp) {
		usedMP += mp;
		state.statistics.useTP(mp);
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean isAlive() {
		return !isDead();
	}

	public boolean isSummon() {
		return false;
	}

	public Entity getSummoner() {
		return null;
	}

	public void setLife(int life) {
		mBaseStats.setStat(CHARAC_LIFE, life);
		this.life = life;
	}
	public void setStrength(int strength) {
		mBaseStats.setStat(CHARAC_STRENGTH, strength);
	}
	public void setAgility(int agility) {
		mBaseStats.setStat(CHARAC_AGILITY, agility);
	}
	public void setWisdom(int wisdom) {
		mBaseStats.setStat(CHARAC_WISDOM, wisdom);
	}
	public void setResistance(int resistance) {
		mBaseStats.setStat(CHARAC_RESISTANCE, resistance);
	}
	public void setScience(int science) {
		mBaseStats.setStat(CHARAC_SCIENCE, science);
	}
	public void setMagic(int magic) {
		mBaseStats.setStat(CHARAC_MAGIC, magic);
	}
	public void setFrequency(int frequency) {
		mBaseStats.setStat(CHARAC_FREQUENCY, frequency);
	}
	public void setTP(int tp) {
		mBaseStats.setStat(CHARAC_TP, tp);
	}
	public void setMP(int mp) {
		mBaseStats.setStat(CHARAC_MP, mp);
	}
	public void setFarmer(int farmer) {
		this.mFarmer = farmer;
	}
	public void setFarmerName(String name) {
		this.mFarmerName = name;
	}
	public void setFarmerCountry(String country) {
		this.mFarmerCountry = country;
	}
	public void setAIName(String ai) {
		this.mAIName = ai;
	}
	public void setTeamID(int team) {
		this.mTeamId = team;
	}
	public void setTeamName(String name) {
		this.mTeamName = name;
	}
	public void setStatic(boolean isStatic) {
		this.mStatic = isStatic;
	}

	public boolean isStatic() {
		return mStatic;
	}

	public void setSkin(int skin) {
		mSkin = skin;
	}

	public void setHat(int hat) {
		mHat = hat;
	}

	public void setMetal(boolean metal) {
		mMetal = metal;
	}

	public void setFace(int face) {
		mFace = face;
	}

	public List<Entity> getSummons(boolean get_dead) {
		List<Entity> summons = new ArrayList<Entity>();
		for (Entity e : state.getTeamEntities(getTeam(), get_dead)) {
			if (e.isSummon() && e.getSummoner().getFId() == getFId()) {
				summons.add(e);
			}
		}
		return summons;
	}

	public List<Entity> getEntitiesAround(int distance) {
		List<Entity> entities = new ArrayList<Entity>();
		for (Entity entity : state.getEntities().values()) {
			if (entity != this && entity.getDistance(this) <= distance) {
				entities.add(entity);
			}
		}
		return entities;
	}

	public int getDistance(Entity entity) {
		if (isDead() || entity.isDead()) return 999;
		return Pathfinding.getCaseDistance(getCell(), entity.getCell());
	}

	public int getResurrected() {
		return this.resurrected;
	}

	public long getTotalOperations() {
		return this.totalOperations;
	}

	public Map<Integer, Integer> loot(State state) {
		return new TreeMap<>();
	}

	public void setState(State state, int fid) {
		this.state = state;
		this.fight_id = fid;
	}

	public void setAI(Object ai) {
		this.ai = ai;
	}

	public Object getAI() {
		return ai;
	}

	public Object getLogs() {
		return logs;
	}

	public void setLogs(Object logs) {
		this.logs = logs;
	}

	public Object getFight() {
		return this.fight;
	}

	public Object getAIFile() {
		return this.aiFile;
	}

	public void setFight(Object fight) {
		this.fight = fight;
	}

	public void setAIFile(Object aiFile) {
		this.aiFile = aiFile;
	}

	public void setRelativeShield(int shield) {
		this.mBaseStats.setStat(CHARAC_RELATIVE_SHIELD, shield);
	}

	public void setAbsoluteShield(int shield) {
		this.mBaseStats.setStat(CHARAC_ABSOLUTE_SHIELD, shield);
	}

	public boolean hasState(EntityState state) {
		return this.states.contains(state);
	}

    public void addState(EntityState state) {
		this.states.add(state);
    }

	public Set<EntityState> getStates() {
		return this.states;
	}

	public void setBirthTurn(int birthTurn) {
		mBirthTurn = birthTurn;
	}

	public int getBirthTurn() {
		return mBirthTurn;
	}
}
