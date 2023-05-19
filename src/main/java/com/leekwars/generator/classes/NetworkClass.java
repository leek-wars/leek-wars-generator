package com.leekwars.generator.classes;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.fight.entity.EntityAI.LeekMessage;

import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;

public class NetworkClass {

	public static boolean sendTo(EntityAI ai, long target, long type, Object message) {
		if (target == ai.getEntity().getFId()) {
			return false;
		}
		var l = ai.getFight().getEntity(target);
		if (l == null) {
			return false;
		}
		if (l.getAI() != null)
			((EntityAI) l.getAI()).addMessage(new LeekMessage(ai.getEntity().getFId(), type, message));
		return true;
	}

	public static Object sendAll(EntityAI ai, long type, Object message) {
		for (var e : ai.getState().getTeamEntities(ai.getEntity().getTeam())) {
			if (e.getFId() == ai.getEntity().getFId())
				continue;
			if (e.getAI() != null)
				((EntityAI) e.getAI()).addMessage(new LeekMessage(ai.getEntity().getFId(), type, message));
		}
		return null;
	}

	public static LegacyArrayLeekValue getMessages_v1_3(EntityAI ai) throws LeekRunException {
		return getMessages_v1_3(ai, ai.getEntity().getFId());
	}

	public static LegacyArrayLeekValue getMessages_v1_3(EntityAI ai, long target_leek) throws LeekRunException {

		ai.ops(100);

		// On récupere le leek ciblé
		var l = ai.getEntity();
		if (target_leek != -1 && target_leek != l.getFId()) {
			l = ai.getFight().getEntity(target_leek);
			if (l == null) {
				return null;
			}
		}

		// On crée le tableau de retour
		EntityAI lia = (EntityAI) l.getAI();
		var messages = new LegacyArrayLeekValue(ai);

		// On y ajoute les messages
		if (lia != null) {

			ai.ops(lia.getMessages().size() * 100);

			for (var message : lia.getMessages()) {
				messages.push(ai, message.getArray(ai));
			}
		}
		return messages;
	}

	public static ArrayLeekValue getMessages(EntityAI ai) throws LeekRunException {
		return getMessages(ai, ai.getEntity().getFId());
	}

	public static ArrayLeekValue getMessages(EntityAI ai, long target_leek) throws LeekRunException {

		ai.ops(100);

		// On récupere le leek ciblé
		var l = ai.getEntity();
		if (target_leek != -1 && target_leek != l.getFId()) {
			l = ai.getFight().getEntity(target_leek);
			if (l == null) {
				return null;
			}
		}

		// On crée le tableau de retour
		EntityAI lia = (EntityAI) l.getAI();
		var messages = new ArrayLeekValue(ai);

		// On y ajoute les messages
		if (lia != null) {

			ai.ops(lia.getMessages().size() * 100);

			for (var message : lia.getMessages()) {
				messages.push(ai, message.getArray(ai));
			}
		}
		return messages;
	}

	public static long getMessageAuthor(EntityAI ai, GenericArrayLeekValue message) throws LeekRunException {
		if (message.size() == 3)
			return (Long) message.get(0);
		return 0;
	}

	public static long getMessageType(EntityAI ai, GenericArrayLeekValue message) throws LeekRunException {
		if (message.size() == 3)
			return (Long) message.get(1);
		return 0;
	}

	public static Object getMessageParams(EntityAI ai, GenericArrayLeekValue message) throws LeekRunException {
		if (message.size() == 3)
			return message.get(2);
		return null;
	}
}
