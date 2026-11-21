package com.leekwars.generator.classes;

/**
 * Outils des variantes LeekScript 5 des fonctions de combat (méthodes `<nom>_v5`, déclarées par
 * `since(5, …)` dans FightFunctions).
 */
final class V5 {

	private V5() {}

	/** En v5, « aucun » (entité, case, arme…) vaut null et non plus -1. */
	static Long orNull(long value) {
		return value == -1 ? null : value;
	}

	static Long orNull(Long value) {
		return value == null || value == -1 ? null : value;
	}
}
