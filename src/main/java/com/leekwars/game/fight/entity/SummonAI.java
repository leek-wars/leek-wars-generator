package com.leekwars.game.fight.entity;

import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.FunctionLeekValue;

public class SummonAI extends EntityAI {

	private FunctionLeekValue mAIFunction;

	public SummonAI(Entity entity, EntityAI owner_ai, FunctionLeekValue ai) {
		super(entity, owner_ai.log);
		mAIFunction = ai;
		setFight(owner_ai.fight);
		mBirthTurn = fight.getTurn();
	}

	@Override
	public AbstractLeekValue runIA() throws Exception {
		if (mAIFunction != null) {
			return mAIFunction.executeFunction(this, new AbstractLeekValue[] {});
		}
		return null;
	}
}
