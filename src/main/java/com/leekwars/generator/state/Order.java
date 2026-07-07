package com.leekwars.generator.state;

import java.util.ArrayList;
import java.util.List;

public class Order {

	private final List<Entity> leeks;
	private int position = 0;
	private int turn = 1;

	public Order() {
		this.leeks = new ArrayList<Entity>();
		this.position = 0;
	}

	public Order(Order order, State state) {
		this.leeks = new ArrayList<Entity>();
		this.position = order.position;
		this.turn = order.turn;   // sans ça, toute COPIE de State repart au tour 1 (getTurn faux)
		for (var entity : order.leeks) {
			this.leeks.add(state.getEntity(entity.getFId()));
		}
	}

	public void addEntity(Entity leek) {
		leeks.add(leek);
	}

	public void addSummon(Entity owner, Entity invoc) {
		if (!leeks.contains(owner)) {
			return;
		}
		leeks.add(leeks.indexOf(owner) + 1, invoc);
	}

	public void addEntity(int index, Entity invoc) {

		leeks.add(index, invoc);

		if (index <= position) {
			position++;
		}
	}

	public void removeEntity(Entity leek) {
		int index = leeks.indexOf(leek);
		if (index == -1) {
			return;
		}
		if (index <= position) {
			position--;
		}
		leeks.remove(index);
		// Si on retire l'entité courante (index 0, position 0), position passe à -1 :
		// on le laisse tel quel. Le prochain next() repassera à 0 SANS wrap, donc sans
		// toucher au compteur de tours. L'ancien `turn--` (compensé par un wrap de
		// next()) faussait la durée si le combat se terminait sur cette mort, et
		// déclenchait un applyCoolDown() en trop (le wrap parasite). #11545
	}

	public Entity current() {
		if (position < 0 || leeks.size() <= position) {
			return null;
		}
		return leeks.get(position);
	}

	public int getTurn() {
		return turn;
	}

	public int getEntityTurnOrder(Entity e) {
		return leeks.indexOf(e) + 1;
	}

	public boolean next() {
		position++;
		if (position >= leeks.size()) {
			turn++;
			position = position % leeks.size();
			return true;
		}
		return false;
	}

	public Entity getNextPlayer() {
		return leeks.get((position + 1) % leeks.size());
	}

	public Entity getNextPlayer(Entity entity) {
		int index = leeks.indexOf(entity);
		if (index == -1) return null;
		return leeks.get((index + 1) % leeks.size());
	}

	public Entity getPreviousPlayer() {
		if (leeks.isEmpty()) return null;
		// Modulo robuste : position peut valoir -1 dans la fenêtre transitoire après le
		// retrait de l'entité courante (cf. removeEntity), et l'ancien `if (p < 0) p +=
		// size` ne corrigeait qu'une fois (-2 restait négatif quand size == 1 -> crash).
		int p = ((position - 1) % leeks.size() + leeks.size()) % leeks.size();
		return leeks.get(p);
	}

	public Entity getPreviousPlayer(Entity entity) {
		int index = leeks.indexOf(entity);
		if (index == -1) return null;
		int p = index - 1;
		if (p < 0)
			p += leeks.size();
		return leeks.get(p);
	}

	public List<Entity> getEntities() {
		return leeks;
	}

	public int getPosition() {
		return position;
	}
}
