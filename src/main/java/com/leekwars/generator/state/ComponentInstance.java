package com.leekwars.generator.state;

import java.util.Map;

import tools.jackson.databind.node.ObjectNode;

/**
 * Un exemplaire AMÉLIORÉ d'un composant, avec le delta d'altération qu'il porte.
 *
 * <p>Une pièce améliorée est un objet unique : elle ne peut valoir que pour un poireau à la
 * fois. Le temps d'un combat, les exemplaires d'un éleveur forment un pot commun ({@link
 * State#takeComponent}) dans lequel chaque {@code setLoadout()} rend ce qu'il tenait puis
 * prend ce que son ensemble demande — premier arrivé, premier servi. Un poireau qui n'appelle
 * pas {@code setLoadout()} ne rend rien : il garde tout son équipement d'avant combat.
 *
 * <p>Les pièces de BASE ne passent pas par là : elles n'ont pas de delta, donc rien à disputer.
 * Posséder le composant suffit, comme pour les armes et les puces d'un ensemble.
 */
public class ComponentInstance {

	/** Poids des caracs en puissance d'altération, copie d'AlterationRegistry::$weights (PHP).
	 * Ils ne servent qu'à départager les exemplaires candidats : une dérive entre les deux
	 * tables ne changerait le choix qu'à distance quasi égale. */
	private static final Map<Integer, Integer> WEIGHTS = Map.ofEntries(
		Map.entry(Entity.STAT_LIFE, 1),
		Map.entry(Entity.STAT_STRENGTH, 2), Map.entry(Entity.STAT_AGILITY, 2),
		Map.entry(Entity.STAT_WISDOM, 2), Map.entry(Entity.STAT_RESISTANCE, 2),
		Map.entry(Entity.STAT_SCIENCE, 2), Map.entry(Entity.STAT_MAGIC, 2),
		Map.entry(Entity.STAT_FREQUENCY, 2),
		Map.entry(Entity.STAT_TP, 80), Map.entry(Entity.STAT_MP, 100),
		Map.entry(Entity.STAT_CORES, 40), Map.entry(Entity.STAT_RAM, 60)
	);

	private final int id;
	private final int template;
	private final Map<Integer, Integer> stats;

	public ComponentInstance(int id, int template, Map<Integer, Integer> stats) {
		this.id = id;
		this.template = template;
		this.stats = stats == null ? Map.of() : stats;
	}

	public int getId() {
		return id;
	}

	public int getTemplate() {
		return template;
	}

	/** Entity.STAT_* → delta apporté par l'altération, par rapport aux stats du modèle. */
	public Map<Integer, Integer> getStats() {
		return stats;
	}

	/** L'altération en objet par nom de caractéristique, sans les zéros. */
	public ObjectNode statsJSON() {
		return Entity.statsJSON(name -> stats.get(Entity.STAT_NAMES.get(name)));
	}

	/** Distance pondérée entre deux deltas d'altération (null ou vide = pièce de base). */
	public static int distance(Map<Integer, Integer> a, Map<Integer, Integer> b) {
		int total = 0;
		java.util.Set<Integer> caracs = new java.util.HashSet<>();
		if (a != null) caracs.addAll(a.keySet());
		if (b != null) caracs.addAll(b.keySet());
		for (Integer carac : caracs) {
			int va = a == null ? 0 : a.getOrDefault(carac, 0);
			int vb = b == null ? 0 : b.getOrDefault(carac, 0);
			total += Math.abs(va - vb) * WEIGHTS.getOrDefault(carac, 0);
		}
		return total;
	}
}
