package com.leekwars.generator.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FightLoadout {

	private final String name;
	private final List<Integer> weapons;
	private final List<Integer> forgottenWeapons;
	private final List<Integer> chips;
	private final Map<Integer, Integer> stats;
	private final Map<Integer, Integer> capitalStats;
	private final boolean overCapital;

	public FightLoadout(String name, List<Integer> weapons, List<Integer> forgottenWeapons, List<Integer> chips, Map<Integer, Integer> stats) {
		this(name, weapons, forgottenWeapons, chips, stats, null, false);
	}

	public FightLoadout(String name, List<Integer> weapons, List<Integer> forgottenWeapons, List<Integer> chips, Map<Integer, Integer> stats, Map<Integer, Integer> capitalStats, boolean overCapital) {
		this.name = name;
		this.weapons = weapons;
		this.forgottenWeapons = forgottenWeapons == null ? java.util.Collections.emptyList() : forgottenWeapons;
		this.chips = chips;
		this.stats = stats == null ? new HashMap<>() : stats;
		this.capitalStats = capitalStats;
		this.overCapital = overCapital;
	}

	public String getName() {
		return name;
	}

	public List<Integer> getWeapons() {
		return weapons;
	}

	/** Ordered list of forgotten-weapon candidates: at apply time, the first one not
	 * already claimed by a teammate of the same farmer wins (with stickiness on the
	 * weapon currently equipped if it appears in this list). */
	public List<Integer> getForgottenWeapons() {
		return forgottenWeapons;
	}

	public List<Integer> getChips() {
		return chips;
	}

	public Map<Integer, Integer> getStats() {
		return stats;
	}

	/**
	 * Part des stats de {@link #getStats()} qui provient du capital investi (hors base de
	 * niveau et hors composants), par stat. C'est cette part — et elle seule — qui exige une
	 * potion de restat quand elle diminue ; les composants d'un loadout, eux, s'équipent
	 * librement, exactement comme côté UI (LoadoutController::statsRequireRestat).
	 *
	 * <p>{@code null} quand l'information n'est pas fournie (scénarios de test construits à
	 * la main) : on retombe alors sur l'ancien comportement, comparaison des stats finales.
	 */
	public Map<Integer, Integer> getCapitalStats() {
		return capitalStats;
	}

	/**
	 * True si l'allocation de capital de ce loadout dépasse le capital total du poireau à qui
	 * il est proposé — un loadout est partagé par tous les poireaux de l'éleveur, celui-ci a
	 * été conçu pour un plus gros. Les caractéristiques ne sont alors pas appliquées, comme
	 * côté UI (not_enough_capital).
	 */
	public boolean isOverCapital() {
		return overCapital;
	}
}
