package com.leekwars.generator.fight.entity;

import leekscript.runner.LeekRunException;
import leekscript.runner.values.FunctionLeekValue;

public class BulbAI extends EntityAI {

	// private static final String TAG = SummonAI.class.getSimpleName();

	private FunctionLeekValue mAIFunction;
	private EntityAI mOwnerAI;

	public BulbAI(Entity entity, EntityAI owner_ai, FunctionLeekValue ai) {
		super(entity, owner_ai.logs);
		valid = true;
		mAIFunction = ai;
		setFight(owner_ai.fight);
		mBirthTurn = fight.getTurn();
		mOwnerAI = owner_ai;
	}

	@Override
	public Object runIA() throws LeekRunException {
		if (mAIFunction != null) {
			mOwnerAI.mEntity = mEntity;
			var argCount = mAIFunction.getArgumentsCount() == -1 ? 0 : mAIFunction.getArgumentsCount();
			var args = new Object[argCount];
			return mAIFunction.run(mOwnerAI, null, args);
		}
		return null;
	}
}
