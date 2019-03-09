package com.leekwars.game.fight.summons;

import java.util.ArrayList;

import leekscript.runner.values.FunctionLeekValue;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.game.attack.chips.Chip;
import com.leekwars.game.attack.chips.Chips;
import com.leekwars.game.fight.entity.Entity;
import com.leekwars.game.fight.entity.Summon;

public class SummonTemplate {

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

	public SummonTemplate(int id, String name, Integer[] chips, String caracteristics_string) {

		mId = id;
		mName = name;

		JSONObject c = JSONObject.parseObject(caracteristics_string);

		mMinLife = c.getJSONArray("life").getIntValue(0);
		mMaxLife = c.getJSONArray("life").getIntValue(1);

		mMinStrength = c.getJSONArray("strength").getIntValue(0);
		mMaxStrength = c.getJSONArray("strength").getIntValue(1);

		mMinWisdom = c.getJSONArray("wisdom").getIntValue(0);
		mMaxWisdom = c.getJSONArray("wisdom").getIntValue(1);

		mMinAgility = c.getJSONArray("agility").getIntValue(0);
		mMaxAgility = c.getJSONArray("agility").getIntValue(1);

		mMinResistance = c.getJSONArray("resistance").getIntValue(0);
		mMaxResistance = c.getJSONArray("resistance").getIntValue(1);

		mMinScience = c.getJSONArray("science").getIntValue(0);
		mMaxScience = c.getJSONArray("science").getIntValue(1);

		mMinMagic = c.getJSONArray("magic").getIntValue(0);
		mMaxMagic = c.getJSONArray("magic").getIntValue(1);

		mMinTp = c.getJSONArray("tp").getIntValue(0);
		mMaxTp = c.getJSONArray("tp").getIntValue(1);

		mMinMp = c.getJSONArray("mp").getIntValue(0);
		mMaxMp = c.getJSONArray("mp").getIntValue(1);

		mChips = new ArrayList<Chip>();
		if (chips != null) {
			for (Integer i : chips) {
				if (i != null) {
					Chip template = Chips.getChip(i);
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

	public static int base(int base, int bonus, double coeff) {
		return (int) (base + Math.floor((bonus - base) * coeff));
	}

	public Summon createInvocation(Entity owner, FunctionLeekValue ai, int id, int level) {
		double c = Math.min(300d, owner.getLevel()) / (300d);
		Summon inv = new Summon(owner, ai, id, 1, mName, level,
				base(mMinLife, mMaxLife, c),
				base(mMinStrength, mMaxStrength, c),
				base(mMinWisdom, mMaxWisdom, c),
				base(mMinAgility, mMaxAgility, c),
				base(mMinResistance, mMaxResistance, c),
				base(mMinScience, mMaxScience, c),
				base(mMinMagic, mMaxMagic, c),
				base(mMinTp, mMaxTp, c),
				base(mMinMp, mMaxMp, c),
				mId, 0);

		for (Chip chip : mChips) {
			inv.addChip(chip);
		}

		return inv;
	}
}