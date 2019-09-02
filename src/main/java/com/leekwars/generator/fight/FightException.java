package com.leekwars.generator.fight;

public class FightException extends Exception {

	private static final long serialVersionUID = -6971396527517233334L;

	public static final int NOT_ENOUGHT_PLAYERS = 1;
	public static final int CANT_START_FIGHT = 2;

	private final int type;

	public FightException(int type) {
		this.type = type;
	}

	@Override
	public String getMessage() {
		switch (type) {
		case NOT_ENOUGHT_PLAYERS:
			return "Pas assez de joueurs";
		case CANT_START_FIGHT:
			return "Toutes les conditions ne sont pas remplies pour démarrer le combat";
		}
		return "";
	}

	public int getType() {
		return type;
	}
}
