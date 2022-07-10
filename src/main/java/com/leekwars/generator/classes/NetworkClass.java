package com.leekwars.generator.classes;

import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.fight.entity.EntityAI.LeekMessage;

import leekscript.runner.LeekRunException;
import leekscript.runner.values.GenericArrayLeekValue;

public class NetworkClass {

	public static boolean sendTo(EntityAI ai, long target, long type, Object message) {
		if (target == ai.getEntity().getFId()) {
			return false;
		}
		Entity l = ai.getFight().getEntity(target);
		if (l == null) {
			return false;
		}
		if (l.getAI() != null)
			l.getAI().addMessage(new LeekMessage(ai.getEntity().getFId(), type, message));
		return true;
	}

	public static Object sendAll(EntityAI ai, long type, Object message) {
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam())) {
			if (l.getFId() == ai.getEntity().getFId())
				continue;
			if (l.getAI() != null)
				l.getAI().addMessage(new LeekMessage(ai.getEntity().getFId(), type, message));
		}
		return null;
	}

	public static GenericArrayLeekValue getMessages(EntityAI ai) throws LeekRunException {
		return getMessages(ai, ai.getEntity().getFId());
	}

	public static GenericArrayLeekValue getMessages(EntityAI ai, long target_leek) throws LeekRunException {

		ai.ops(100);

		// On récupere le leek ciblé
		Entity l = ai.getEntity();
		if (target_leek != -1 && target_leek != l.getFId()) {
			l = ai.getFight().getEntity(target_leek);
			if (l == null) {
				return null;
			}
		}

		// On crée le tableau de retour
		EntityAI lia = l.getAI();
		var messages = ai.newArray();

		// On y ajoute les messages
		if (lia != null) {

			ai.ops(lia.getMessages().size() * 100);

			for (var message : lia.getMessages()) {
				messages.push(ai, message.getArray(ai));
			}
		}
		return messages;
	}

	public static Object getMessageAuthor(EntityAI ai, GenericArrayLeekValue message) throws LeekRunException {
		if (message.size() == 3)
			return message.get(((EntityAI) ai).getUAI(), 0);
		return null;
	}

	public static Object getMessageType(EntityAI ai, GenericArrayLeekValue message) throws LeekRunException {
		if (message.size() == 3)
			return message.get(((EntityAI) ai).getUAI(), 1);
		return null;
	}

	public static Object getMessageParams(EntityAI ai, GenericArrayLeekValue message) throws LeekRunException {
		if (message.size() == 3)
			return message.get(((EntityAI) ai).getUAI(), 2);
		return null;
	}
}
