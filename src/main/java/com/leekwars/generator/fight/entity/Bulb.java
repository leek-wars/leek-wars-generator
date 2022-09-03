package com.leekwars.generator.fight.entity;

import leekscript.runner.values.FunctionLeekValue;

import com.leekwars.generator.Log;
import com.leekwars.generator.fight.bulbs.BulbTemplate;
import com.leekwars.generator.fight.bulbs.Bulbs;

public class Bulb extends Entity {

	private static final String TAG = Bulb.class.getSimpleName();

	protected Entity mOwner;

	public Bulb(Entity owner, FunctionLeekValue ai, Integer id, String name, int level, int life, int strength, int wisdom, int agility, int resistance, int science, int magic, int tp, int mp, int skin, int hat) {
		super(id, name, owner.getFarmer(), level, life, tp, mp, strength, agility, 0, wisdom, resistance, science, magic, skin, false, 0, owner.getTeamId(), owner.getTeamName(), owner.getAI().getId(), owner.getAIName(), owner.getFarmerName(), owner.getFarmerCountry(), hat);

		mOwner = owner;
		fight = mOwner.fight;

		// On détermine l'ia de l'invocation
		mEntityAI = new BulbAI(this, owner.mEntityAI, ai);
	}

	@Override
	public boolean isSummon() {
		return true;
	}

	@Override
	public Entity getSummoner() {
		return mOwner;
	}

	@Override
	public int getType() {
		return Entity.TYPE_BULB;
	}

	@Override
	public void resurrect(Entity entity, double factor) {
		super.resurrect(entity, factor);
		mOwner = entity;
	}

	public static int base(int base, int bonus, double coeff) {
		return (int) (base + Math.floor(bonus * coeff));
	}

	public static Bulb create(Entity owner, FunctionLeekValue ai, int id, int type, int level, boolean critical) {

		BulbTemplate bulb_template = Bulbs.getInvocationTemplate(type);
		if (bulb_template != null) {
			return bulb_template.createInvocation(owner, ai, id, level, critical);
		} else {
			Log.e(TAG, "Invocation " + type + " inexistante");
		}
		return null;
	}
}
