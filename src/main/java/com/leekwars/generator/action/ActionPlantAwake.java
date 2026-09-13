package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

/**
 * Éveil d'une plante : une entité vient d'entrer dans sa zone, la plante joue.
 * Émise juste avant les actions de la plante, dans le tour de l'entité qui a bougé —
 * le client s'en sert pour la faire rebondir et signaler qui l'a réveillée.
 */
public class ActionPlantAwake implements Action {

	private final int plant;
	private final int trigger;

	public ActionPlantAwake(Entity plant, Entity trigger) {
		this.plant = plant.getFId();
		this.trigger = trigger.getFId();
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.PLANT_AWAKE);
		retour.add(plant);
		retour.add(trigger);
		return retour;
	}
}
