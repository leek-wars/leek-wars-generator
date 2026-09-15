package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

/**
 * Fin d'un réveil de plante : les actions qui suivent sont de nouveau celles de l'entité
 * dont c'est le tour. Referme la parenthèse ouverte par {@link ActionPlantAwake} — un
 * SAY ou un USE_CHIP ne porte pas l'entité qui agit, le client la déduit du dernier
 * LEEK_TURN, donc sans borne de fin la plante parlerait et tirerait au nom du passant.
 */
public class ActionPlantAsleep implements Action {

	private final int plant;

	public ActionPlantAsleep(Entity plant) {
		this.plant = plant.getFId();
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.PLANT_ASLEEP);
		retour.add(plant);
		return retour;
	}
}
