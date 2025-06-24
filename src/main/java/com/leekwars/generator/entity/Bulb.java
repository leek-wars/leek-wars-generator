package com.leekwars.generator.entity;

import com.leekwars.generator.bulbs.BulbTemplate;
import com.leekwars.generator.bulbs.Bulbs;
import com.leekwars.generator.state.Entity;

public class Bulb extends Entity {

	private static final String TAG = Bulb.class.getSimpleName();

	protected Entity mOwner;

	public Bulb(Entity owner, Integer id, String name, int level, int life, int strength, int wisdom, int agility, int resistance, int science, int magic, int cores, int ram, int tp, int mp, int skin, int hat) {
		super(id, name, owner.getFarmer(), level, life, tp, mp, strength, agility, 0, wisdom, resistance, science, magic, cores, ram, skin, false, 0, owner.getTeamId(), owner.getTeamName(), owner.getAIId(), owner.getAIName(), owner.getFarmerName(), owner.getFarmerCountry(), hat);

		mOwner = owner;
		state = mOwner.state;
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

	public static int base(int base, int bonus, double coeff) {
		return (int) (base + Math.floor(bonus * coeff));
	}

	public static Bulb create(Entity owner, int id, int type, int level, boolean critical) {

		BulbTemplate bulb_template = Bulbs.getInvocationTemplate(type);
		if (bulb_template != null) {
			return bulb_template.createInvocation(owner, id, level, critical);
		} else {
			// Log.e(TAG, "Invocation " + type + " inexistante");
		}
		return null;
	}
}
