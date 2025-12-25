package com.leekwars.generator.bulbs;

import java.util.ArrayList;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.entity.Bulb;
import com.leekwars.generator.state.Entity;

public class BulbTemplate {

	// private final static String TAG = SummonTemplate.class.getSimpleName();

	private final int mId;

	private final String mName;

	private final ArrayList<Chip> mChips;

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

	public BulbTemplate(int id, String name, ArrayNode chips, ObjectNode characteristics) {

		mId = id;
		mName = name;

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

		return inv;
	}

	public ArrayList<Chip> getChips() {
		return mChips;
	}
}