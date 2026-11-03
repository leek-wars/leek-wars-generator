package com.leekwars.generator.classes;

import java.util.List;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.fight.entity.EntityAI.LeekMessage;
import com.leekwars.generator.state.Entity;

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
		if (l.getAI() != null) {
			var sender = ai.getEntity();
			return ((EntityAI) l.getAI()).addMessage(new LeekMessage(sender.getFId(), type, message), l.getTeam() == sender.getTeam());
		}
		return true;
	}

	public static Object sendAll(EntityAI ai, long type, Object message) {
		for (var e : ai.getState().getTeamEntities(ai.getEntity().getTeam())) {
			if (e.getFId() == ai.getEntity().getFId())
				continue;
			if (e.getAI() != null)
				((EntityAI) e.getAI()).addMessage(new LeekMessage(ai.getEntity().getFId(), type, message), true);
		}
		return null;
	}

	public static LegacyArrayLeekValue getMessages_v1_3(EntityAI ai) throws LeekRunException {
		return getMessages_v1_3(ai, ai.getEntity().getFId());
	}

	public static LegacyArrayLeekValue getMessages_v1_3(EntityAI ai, long target_leek) throws LeekRunException {
		var mailbox = readableMessages(ai, target_leek);
		if (mailbox == null) {
			return null;
		}
		var messages = new LegacyArrayLeekValue(ai);
		for (var message : mailbox) {
			messages.push(ai, message.getArray(ai));
		}
		return messages;
	}

	public static ArrayLeekValue getMessages(EntityAI ai) throws LeekRunException {
		return getMessages(ai, ai.getEntity().getFId());
	}

	public static ArrayLeekValue getMessages(EntityAI ai, long target_leek) throws LeekRunException {
		var mailbox = readableMessages(ai, target_leek);
		if (mailbox == null) {
			return null;
		}
		var messages = new ArrayLeekValue(ai);
		for (var message : mailbox) {
			messages.push(ai, message.getArray(ai));
		}
		return messages;
	}

	/**
	 * Les messages de la boîte de target_leek (-1 : la sienne) que l'IA a le droit de lire, opérations
	 * comptées, ou null si l'entité n'existe pas. La boîte d'un mob n'est lisible que par lui-même.
	 */
	private static List<LeekMessage> readableMessages(EntityAI ai, long target_leek) throws LeekRunException {

		ai.ops(100);

		// On récupere le leek ciblé
		var l = ai.getEntity();
		if (target_leek != -1 && target_leek != l.getFId()) {
			l = ai.getFight().getEntity(target_leek);
			if (l == null) {
				return null;
			}
		}

		EntityAI lia = (EntityAI) l.getAI();
		if (lia == null || (l.getType() == Entity.TYPE_MOB && l != ai.getEntity())) {
			return List.of();
		}
		ai.ops(lia.getMessages().size() * 100);
		return lia.getMessages();
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
