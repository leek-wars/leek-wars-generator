package com.leekwars.generator.fight.entity;

import leekscript.runner.values.FunctionLeekValue;

import com.leekwars.generator.Log;
import com.leekwars.generator.fight.summons.SummonTemplate;
import com.leekwars.generator.fight.summons.Summons;

public class Summon extends Entity {

	private static final String TAG = Summon.class.getSimpleName();

	protected Entity mOwner;
	protected int mType;

	public Summon(Entity owner, FunctionLeekValue ai, Integer id, int type, String name, int level, int life, int strength, int wisdom, int agility, int resistance, int science, int magic, int tp, int mp, int skin, int hat) {

		super(id, name, owner.getFarmer(), level, life, tp, mp, strength, agility, 0, wisdom, resistance, science, magic, skin, owner.getTeamId(), owner.getTeamName(), owner.getAIId(), owner.getAIName(), owner.getFarmerName(), owner.getFarmerCountry(), hat);

		mOwner = owner;
		mType = type;

		// On détermine l'ia de l'invocation
		mEntityAI = new SummonAI(this, owner.mEntityAI, ai);
	}

	@Override
	public int getOwnerId() {
		return mOwner.getFId();
	}

	@Override
	public int getType() {
		return mType;
	}

	@Override
	public void resurrect(Entity entity) {
		super.resurrect(entity);
		mOwner = entity;
	}

	public static int base(int base, int bonus, double coeff) {
		return (int) (base + Math.floor(bonus * coeff));
	}

	public static Summon create(Entity owner, FunctionLeekValue ai, int id, int type, int level) {

		SummonTemplate invocation_template = Summons.getInvocationTemplate(type);
		if (invocation_template != null) {
			return invocation_template.createInvocation(owner, ai, id, level);
		} else {
			Log.e(TAG, "Invocation " + type + " inexistante");
		}
		return null;
	}
}
