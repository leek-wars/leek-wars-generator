package com.leekwars.generator.state;

import tools.jackson.databind.JsonNode;

/**
 * Règles d'un équipement construit en code (setLoadout(objet)) : niveau des objets, nombre
 * d'armes, barème du capital. Le serveur les tient (il connaît le niveau des composants) et les
 * fournit au générateur ; sans elles, setLoadout(objet) n'est pas disponible.
 */
public interface LoadoutRules {

	/**
	 * Équipement normalisé (LoadoutClass.readLoadouts : name, weapons, forgotten_weapons, chips,
	 * components [{slot, item, stats}], stats {carac: capital}) résolu au niveau d'un poireau :
	 * objets trop hauts retirés, stats finales, capital, dépassement. null s'il est inutilisable.
	 */
	FightLoadout resolve(JsonNode loadout, int level);

	/** Capital total d'un poireau à ce niveau. */
	int totalCapital(int level);
}
