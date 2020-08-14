package com.leekwars.generator.fight;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.fight.entity.Entity;

public class Order {

	private final List<Entity> leeks;
	private int position = 0;
	private int turn = 1;

	public Order() {
		this.leeks = new ArrayList<Entity>();
		this.position = 0;
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
		if (position == -1) {
			position = leeks.size() - 1;
			turn--; // On décrémente le tour car on va le réincrémenter tout de suite derrière
		}
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

	public Entity getPreviousPlayer() {
		int p = position - 1;
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
