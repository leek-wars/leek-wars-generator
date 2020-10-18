package com.leekwars.generator.fight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.leekwars.generator.fight.entity.Entity;

/*
 * Handle the computation of entities starting order
 */
public class StartOrder {

	// List of list of entities (teams)
	private final List<List<Entity>> teams = new ArrayList<List<Entity>>();
	private int totalEntities = 0;

	// Add an entity
	public void addEntity(Entity entity) {

		while (teams.size() < entity.getTeam() + 1) {
			teams.add(new ArrayList<Entity>());
		}
		teams.get(entity.getTeam()).add(entity);
		totalEntities++;
	}

	public List<Entity> compute(Fight fight) {

		// Sort entities inside team on their frequency
		for (List<Entity> team : teams) {
			Collections.sort(team, new Comparator<Entity>() {
				@Override
				public int compare(Entity e1, Entity e2) {
					return Integer.signum(e2.getFrequency() - e1.getFrequency());
				}
			});
		}

		// Compute probability for each team, example : [0.15, 0.35, 0.5]
		List<Double> probas = new ArrayList<Double>();
		List<Integer> frequencies = new ArrayList<Integer>();

		double sum = 0;
		for (int i = 0; i < teams.size(); ++i) {
			int frequency = teams.get(i).get(0).getFrequency();
			frequencies.add(frequency);
			sum += frequency;
		}

		double psum = 0;
		for (int i = 0; i < teams.size(); ++i) {

			double f = frequencies.get(i);
			double p = 1d / (1d + Math.pow(10, (sum - f) / 100d));

			probas.add(p);
			psum += p;
		}

		for (int i = 0; i < teams.size(); ++i) {
			probas.set(i, probas.get(i) / psum);
		}
		psum = 1;

		// Logger.log("Frequencies : " +
		// Arrays.toString(frequencies.toArray()));

		// Compute team order, example : [team3, team1, team2]
		List<Integer> teamOrder = new ArrayList<Integer>();
		List<Integer> remaining = new ArrayList<Integer>();
		for (int i = 0; i < teams.size(); ++i) {
			remaining.add(i);
		}

		for (int t = 0; t < teams.size(); ++t) {

			double v = fight.getRandom().getDouble();
			// Logger.log("Remaining : " +
			// Arrays.toString(remaining.toArray()));
			// Logger.log("Probabilities : " +
			// Arrays.toString(probas.toArray()));
			// Logger.log("psum : " + psum);
			// Logger.log("v : " + v);

			for (int i = 0; i < remaining.size(); ++i) {

				int team = remaining.get(i);
				double p = probas.get(team);

				if (v <= p) {
					teamOrder.add(team);
					remaining.remove(i);
					psum -= p;
					break;
				}
				v -= p;
			}

			for (int i = 0; i < teams.size(); ++i) {
				probas.set(i, probas.get(i) / psum);
			}
			psum = 1;
		}

		// Logger.log("Team order : " + Arrays.toString(teamOrder.toArray()));

		// Compute entity order : [entity5, entity1, entity2, entity4, ...]
		List<Entity> order = new ArrayList<Entity>();

		int currentTeamI = 0;
		while (order.size() != totalEntities) {

			int team = teamOrder.get(currentTeamI);
			if (teams.get(team).size() > 0) {
				order.add(teams.get(team).remove(0));
			}

			currentTeamI = (currentTeamI + 1) % teams.size();
		}

		// Logger.log("Order : " + Arrays.toString(order.toArray()));

		return order;
	}
}
