package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

/**
 * Éveil d'une plante : une entité vient d'entrer dans sa zone, la plante joue.
 * {@code [PLANT_AWAKE, plante, déclencheur, PT de la plante]}, émise juste avant les
 * actions de la plante, dans le tour de l'entité qui a bougé — le client s'en sert pour la
 * faire rebondir, signaler qui l'a réveillée, et savoir que ce qui suit est joué par la
 * plante et non par l'entité dont c'est le tour, jusqu'à {@link ActionPlantAsleep}.
 */
public class ActionPlantAwake implements Action {

	private final int plant;
	private final int trigger;
	/** PT de la plante une fois le réveil passé, c'est-à-dire ses PT pleins. */
	private final int tp;

	public ActionPlantAwake(Entity plant, Entity trigger, int tp) {
		this.plant = plant.getFId();
		this.trigger = trigger.getFId();
		this.tp = tp;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.PLANT_AWAKE);
		retour.add(plant);
		retour.add(trigger);
		retour.add(tp);
		return retour;
	}
}
