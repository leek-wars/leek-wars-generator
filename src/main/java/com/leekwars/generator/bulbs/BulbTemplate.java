package com.leekwars.generator.bulbs;

import java.util.ArrayList;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.entity.Bulb;
import com.leekwars.generator.state.Entity;

public class BulbTemplate {

	// private final static String TAG = SummonTemplate.class.getSimpleName();

	private final int mId;

	private final String mName;

	private final ArrayList<Chip> mChips;

	// États permanents de l'invocation (ex. ROOTED pour les plantes), appliqués
	// à l'apparition (et réappliqués à la résurrection).
	private final ArrayList<EntityState> mStates;

	private final int mMinLife;
	private final int mMaxLife;

	private final int mMinStrength;
	private final int mMaxStrength;

	private final int mMinWisdom;
	private final int mMaxWisdom;

	private final int mMinAgility;
	private final int mMaxAgility;

	private final int mMinResistance;
	private final int mMaxResistance;

	private final int mMinScience;
	private final int mMaxScience;

	private final int mMinMagic;
	private final int mMaxMagic;

	private final int mMinTp;
	private final int mMaxTp;

	private final int mMinMp;
	private final int mMaxMp;

	// Rayon de la zone d'Éveil, en cases (distance de Manhattan). 0 = pas de zone,
	// l'invocation joue son tour comme un bulbe ordinaire. Une plante à zone, elle, ne
	// joue plus son tour : elle se réveille quand une entité entre dans sa zone
	// (release/300/eveil_plantes_puces.md). Le Prototaxite est enraciné SANS zone.
	private final int mZone;

	public BulbTemplate(int id, String name, ArrayNode chips, ObjectNode characteristics) {
		this(id, name, chips, characteristics, null, 0);
	}

	public BulbTemplate(int id, String name, ArrayNode chips, ObjectNode characteristics, ArrayNode states) {
		this(id, name, chips, characteristics, states, 0);
	}

	public BulbTemplate(int id, String name, ArrayNode chips, ObjectNode characteristics, ArrayNode states, int zone) {

		mId = id;
		mName = name;
		mZone = zone;

		mMinLife = ((ArrayNode) characteristics.get("life")).get(0).intValue();
		mMaxLife = ((ArrayNode) characteristics.get("life")).get(1).intValue();

		mMinStrength = ((ArrayNode) characteristics.get("strength")).get(0).intValue();
		mMaxStrength = ((ArrayNode) characteristics.get("strength")).get(1).intValue();

		mMinWisdom = ((ArrayNode) characteristics.get("wisdom")).get(0).intValue();
		mMaxWisdom = ((ArrayNode) characteristics.get("wisdom")).get(1).intValue();

		mMinAgility = ((ArrayNode) characteristics.get("agility")).get(0).intValue();
		mMaxAgility = ((ArrayNode) characteristics.get("agility")).get(1).intValue();

		mMinResistance = ((ArrayNode) characteristics.get("resistance")).get(0).intValue();
		mMaxResistance = ((ArrayNode) characteristics.get("resistance")).get(1).intValue();

		mMinScience = ((ArrayNode) characteristics.get("science")).get(0).intValue();
		mMaxScience = ((ArrayNode) characteristics.get("science")).get(1).intValue();

		mMinMagic = ((ArrayNode) characteristics.get("magic")).get(0).intValue();
		mMaxMagic = ((ArrayNode) characteristics.get("magic")).get(1).intValue();

		mMinTp = ((ArrayNode) characteristics.get("tp")).get(0).intValue();
		mMaxTp = ((ArrayNode) characteristics.get("tp")).get(1).intValue();

		mMinMp = ((ArrayNode) characteristics.get("mp")).get(0).intValue();
		mMaxMp = ((ArrayNode) characteristics.get("mp")).get(1).intValue();

		mChips = new ArrayList<Chip>();
		if (chips != null) {
			for (var i : chips) {
				if (i != null) {
					Chip template = Chips.getChip(i.intValue());
					mChips.add(template);
				}
			}
		}

		mStates = new ArrayList<EntityState>();
		if (states != null) {
			for (var s : states) {
				if (s == null) continue;
				int ordinal = s.intValue();
				if (ordinal > 0 && ordinal < EntityState.values().length) {
					mStates.add(EntityState.values()[ordinal]);
				}
			}
		}
	}

	public int getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public static int base(int base, int bonus, double coeff, double multiplier) {
		return (int) ((base + Math.floor((bonus - base) * coeff)) * multiplier);
	}

	public Bulb createInvocation(Entity owner, int id, int level, boolean critical) {
		double c = Math.min(300d, owner.getLevel()) / (300d);
		double multiplier = critical ? 1.2 : 1.0;

		Bulb inv = new Bulb(owner, id, mName, level,
				base(mMinLife, mMaxLife, c, multiplier),
				base(mMinStrength, mMaxStrength, c, multiplier),
				base(mMinWisdom, mMaxWisdom, c, multiplier),
				base(mMinAgility, mMaxAgility, c, multiplier),
				base(mMinResistance, mMaxResistance, c, multiplier),
				base(mMinScience, mMaxScience, c, multiplier),
				base(mMinMagic, mMaxMagic, c, multiplier),
				1,
				6,
				base(mMinTp, mMaxTp, c, multiplier),
				base(mMinMp, mMaxMp, c, multiplier),
				mId, 0);

		for (Chip chip : mChips) {
			inv.addChip(chip);
		}

		inv.setTemplate(this);

		return inv;
	}

	public ArrayList<Chip> getChips() {
		return mChips;
	}

	public ArrayList<EntityState> getStates() {
		return mStates;
	}

	// Une plante est une invocation enracinée : c'est l'état ROOTED du template qui la définit,
	// pas une liste d'ids en dur (Maïs 9, Piment 10, Prototaxite 13 aujourd'hui). Détermine le
	// type d'entité vu par les IA (ENTITY_PLANT au lieu de ENTITY_BULB).
	public boolean isPlant() {
		return mStates.contains(EntityState.ROOTED);
	}

	// Une invocation qui ne peut rien faire (aucune puce, 0 PT max) n'a pas
	// besoin d'IA : pas d'avertissement BULB_WITHOUT_AI pour elle (ex. prototaxites).
	public boolean canAct() {
		return !mChips.isEmpty() || mMaxTp > 0;
	}

	public int getZone() {
		return mZone;
	}

	// Une plante à zone se joue à l'Éveil et nulle part ailleurs : elle sort de l'ordre
	// des tours (son IA n'y est jamais lancée) et ses cooldowns se comptent en réveils.
	public boolean hasAwakening() {
		return mZone > 0;
	}

	public int getMinLife() { return mMinLife; }
	public int getMaxLife() { return mMaxLife; }
	public int getMinStrength() { return mMinStrength; }
	public int getMaxStrength() { return mMaxStrength; }
	public int getMinWisdom() { return mMinWisdom; }
	public int getMaxWisdom() { return mMaxWisdom; }
	public int getMinAgility() { return mMinAgility; }
	public int getMaxAgility() { return mMaxAgility; }
	public int getMinResistance() { return mMinResistance; }
	public int getMaxResistance() { return mMaxResistance; }
	public int getMinScience() { return mMinScience; }
	public int getMaxScience() { return mMaxScience; }
	public int getMinMagic() { return mMinMagic; }
	public int getMaxMagic() { return mMaxMagic; }
	public int getMinTp() { return mMinTp; }
	public int getMaxTp() { return mMaxTp; }
	public int getMinMp() { return mMinMp; }
	public int getMaxMp() { return mMaxMp; }
}