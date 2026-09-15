package com.leekwars.generator.fight.entity;

import com.leekwars.generator.state.Entity;

import leekscript.runner.LeekRunException;
import leekscript.runner.Session;
import leekscript.runner.values.FunctionLeekValue;

public class BulbAI extends EntityAI {

	// private static final String TAG = SummonAI.class.getSimpleName();

	private FunctionLeekValue mAIFunction;
	private EntityAI mOwnerAI;

	public BulbAI(Entity entity, EntityAI owner_ai, FunctionLeekValue ai) {
		super(entity, owner_ai.getLogs());
		valid = true;
		mAIFunction = ai;
		setFight(owner_ai.fight);
		mOwnerAI = owner_ai;
	}

	@Override
	public Object runIA() throws LeekRunException {
		return runIA(null);
	}

	@Override
	public Object runIA(Session session) throws LeekRunException {
		if (mAIFunction == null) {
			return null;
		}
		// La fonction du bulbe est une fermeture de l'IA de l'invocateur : c'est cette
		// IA-là qui l'exécute, avec mEntity pointé sur le bulbe. Un réveil de plante
		// se produit AU MILIEU du tour de quelqu'un d'autre — potentiellement de
		// l'invocateur lui-même, qui vient d'entrer dans la zone de sa propre plante.
		// Sans cette sauvegarde, il reprendrait la main en croyant être la plante.
		Entity previous = mOwnerAI.mEntity;
		// Le compteur d'opérations est lui aussi celui de l'invocateur. Un réveil de plante
		// tombe dans le tour de quelqu'un d'autre : sans remise à zéro, la fermeture repart
		// du compteur laissé par le dernier tour de l'invocateur — une IA qui a consommé son
		// budget tuerait sa propre plante avec une erreur « trop d'opérations ». Le réveil a
		// donc son propre compteur (ce que runPlantAwakening annonce), et l'invocateur
		// retrouve ensuite le sien, augmenté de ce que la plante a dépensé : dans son propre
		// tour, il paye toujours ce que sa fermeture consomme.
		long previousOperations = mOwnerAI.getOperations();
		mOwnerAI.mEntity = mEntity;
		mOwnerAI.resetCounter();
		try {
			var argCount = mAIFunction.getArgumentsCount() == -1 ? 0 : mAIFunction.getArgumentsCount();
			var args = new Object[argCount];
			// Une fonction sans paramètre reste valide : l'entité déclenchante est
			// simplement ignorée. Comme partout dans l'API, une entité se passe par son
			// id, en long.
			// Éveil : l'entité entrante est le premier argument. Elle est portée par la
			// plante (setAwakeningTrigger) et pas par cette IA, qui n'est pas celle qui
			// exécute la fermeture.
			var trigger = mEntity.getAwakeningTrigger();
			if (argCount > 0 && trigger != null) {
				args[0] = (long) trigger.getFId();
			}
			return mAIFunction.run(mOwnerAI, null, args);
		} finally {
			long spent = mOwnerAI.getOperations();
			mOwnerAI.resetCounter();
			mOwnerAI.opsNoCheck((int) Math.min(Integer.MAX_VALUE, previousOperations + spent));
			mOwnerAI.mEntity = previous;
		}
	}

	@Override
	public String getErrorMessage(StackTraceElement[] elements) {
		return mOwnerAI.getErrorMessage(elements);
	}
}
