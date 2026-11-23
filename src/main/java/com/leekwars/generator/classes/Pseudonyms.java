package com.leekwars.generator.classes;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

/**
 * Noms de remplacement d'un combat anonyme (tournoi d'IA, Scenario#anonymous) : getName, getFarmerName
 * et getAIName rendent un pseudonyme pour l'entité d'un autre éleveur, au lieu de null qui faisait
 * planter les IA qui manipulent ces noms comme des chaînes.
 *
 * Un pseudonyme est stable pendant tout le combat, change d'un combat à l'autre (il dépend de la
 * graine) et ne consomme aucun tirage du générateur aléatoire du combat : un rejeu reste identique.
 * Deux poireaux du combat n'ont jamais le même nom (décalage par leur numéro dans le combat), et un
 * éleveur garde le même pseudonyme pour tous ses poireaux.
 */
public class Pseudonyms {

	private static final String[] LEEKS = {
		"Ciboulette", "Échalote", "Radis", "Navet", "Panais", "Rutabaga", "Topinambour", "Salsifis",
		"Céleri", "Fenouil", "Blette", "Cardon", "Bette", "Chou", "Brocoli", "Courgette",
		"Potiron", "Citrouille", "Butternut", "Pâtisson", "Artichaut", "Asperge", "Endive", "Cresson",
		"Roquette", "Mâche", "Laitue", "Scarole", "Frisée", "Épinard", "Oseille", "Persil",
		"Cerfeuil", "Estragon", "Aneth", "Basilic", "Thym", "Romarin", "Sauge", "Origan",
		"Ail", "Oignon", "Carotte", "Betterave", "Patate", "Igname", "Manioc", "Taro",
		"Haricot", "Petit-pois", "Lentille", "Fève", "Pois-chiche", "Soja", "Gingembre", "Raifort",
		"Wasabi", "Kale", "Pak-choï", "Okra", "Chayotte", "Aubergine", "Poivron", "Piment",
	};

	private static final String[] FARMERS = {
		"Jardinier", "Maraîcher", "Botaniste", "Cultivateur", "Semeur", "Planteur", "Arroseur", "Bêcheur",
		"Sarcleur", "Bouturier", "Greffeur", "Moissonneur", "Glaneur", "Herboriste", "Pépiniériste", "Horticulteur",
		"Laboureur", "Vigneron", "Cueilleur", "Agronome", "Potager", "Serriste", "Composteur", "Récolteur",
		"Défricheur", "Fermier", "Paysan", "Grainetier", "Rempoteur", "Tailleur", "Butteur", "Arboriste",
	};

	private static final String[] AIS = {
		"Cerveau", "Neurone", "Synapse", "Cortex", "Réflexe", "Instinct", "Stratège", "Tacticien",
		"Calculateur", "Oracle", "Prophète", "Penseur", "Logicien", "Automate", "Robot", "Golem",
		"Algorithme", "Heuristique", "Minimax", "Alpha-Bêta", "Monte-Carlo", "Glouton", "Récursif", "Itératif",
		"Boucle", "Fonction", "Variable", "Pointeur", "Octet", "Registre", "Processeur", "Compilateur",
	};

	public static String leekName(State state, Entity entity) {
		var name = pick(LEEKS, state, 0x4c45454bL, entity.getFId());
		// Jamais le vrai nom d'une entité du combat (un poireau « Radis », un mob « Potiron ») : une
		// IA qui reconnaît une entité à son nom la confondrait avec une autre.
		for (var e : state.getAllEntities(true)) {
			if (name.equals(e.getName())) return name + " ?";
		}
		return name;
	}

	public static String farmerName(State state, Entity entity) {
		return pick(FARMERS, state, 0x4641524dL, farmerIndex(state, entity.getFarmer()));
	}

	public static String aiName(State state, Entity entity) {
		return pick(AIS, state, 0x41494e4dL, entity.getFId());
	}

	/** Le plus petit numéro de combat des entités de l'éleveur : distinct d'un éleveur à l'autre. */
	private static int farmerIndex(State state, int farmer) {
		int min = Integer.MAX_VALUE;
		for (var e : state.getAllEntities(true)) {
			if (e.getFarmer() == farmer && e.getFId() < min) min = e.getFId();
		}
		return min == Integer.MAX_VALUE ? 0 : min;
	}

	/** Le nom d'indice `index` décalé par la graine ; au-delà de la liste, un numéro le rend unique. */
	private static String pick(String[] names, State state, long salt, int index) {
		int offset = (int) Long.remainderUnsigned(mix(state.getSeed() ^ salt), names.length);
		var name = names[Math.floorMod(offset + index, names.length)];
		int round = Math.floorDiv(index, names.length);
		return round > 0 ? name + " " + (round + 1) : name;
	}

	/** SplitMix64 : la graine bien mélangée, sans toucher au générateur aléatoire du combat. */
	private static long mix(long z) {
		z += 0x9e3779b97f4a7c15L;
		z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
		z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
		return z ^ (z >>> 31);
	}
}
