package com.leekwars.generator.maps;

import com.leekwars.generator.state.State;

/**
 * L'échiquier (carte de type TYPE) : la position d'une partie célèbre, tirée au hasard. Chaque
 * pièce est un obstacle 2x2 posé sur son carré, l'échiquier occupant les 16x16 cases du centre
 * (chaque carré du damier du client fait 2x2 cases). Quelques blocs 1x1 s'y ajoutent, de la
 * couleur du carré où ils tombent.
 *
 * Ids d'obstacles (table OBSTACLES du client, ObstacleInfo) : OBSTACLE_PIECES + 2 × rang de la
 * pièce dans PIECES, + 1 pour une pièce noire ; BLOCK_WHITE et BLOCK_BLACK pour les blocs.
 * L'id de la carte (ID_BASE + rang de la position) dit au client quelle partie est jouée.
 */
public class ChessMap {

	public static final int TYPE = 9;
	public static final int ID_BASE = 1000;
	private static final String PIECES = "PRNBQK";
	private static final int OBSTACLE_PIECES = 68;
	private static final int BLOCK_WHITE = 83;
	private static final int BLOCK_BLACK = 84;

	/** Positions (placement FEN, 8e rangée d'abord), vérifiées en rejouant les coups des parties. */
	public static final String[] POSITIONS = {
		"1n1Rkb1r/p4ppp/4q3/4p1B1/4P3/8/PPP2PPP/2K5", // Morphy – duc de Brunswick et comte Isouard, « partie de l'Opéra », Paris 1858
		"r1bk3r/p2pBpNp/n4n2/1p1NP2P/6P1/3P4/P1P1K3/q5b1", // Anderssen – Kieseritzky, « l'Immortelle », Londres 1851
		"1r3kr1/pbpBBp1p/1b3P2/8/8/2P2q2/P4PPP/3R2K1", // Anderssen – Dufresne, « la Toujours Jeune », Berlin 1852
		"1Q6/5pk1/2p3p1/1p2N2p/1b5P/1bn5/2r3P1/2K5", // D. Byrne – Fischer, « la partie du siècle », New York 1956
		"r1k4r/p2nb1p1/2b4p/1p1n1p2/2PP4/3Q1NB1/1P3PPP/R5K1", // Deep Blue – Kasparov, 6e partie, New York 1997
		"rn3r2/pbppq1p1/1p2pN2/8/3P2NP/6P1/PPPKBP1R/R5k1", // Ed. Lasker – Thomas, la chasse au roi, Londres 1912
		"rn1q1bnr/ppp1kB1p/3p2p1/3NN3/4P3/8/PPPP1PPP/R1BbK2R", // Legall de Kermeur – Saint-Brie, le mat de Légal, Paris 1750
		"r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR", // Le coup du berger
		"rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR", // Le mat du sot
		"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", // Position initiale
	};

	/** Une position au hasard, tirée une fois par combat. */
	public static int pick(State state) {
		return state.getRandom().getInt(0, POSITIONS.length - 1);
	}

	/**
	 * Pose les pièces de la position `position`, puis jusqu'à `blocks` blocs 1x1 sur des cases
	 * libres. Coordonnées du client : x = getX() − (largeur − 1), y = getY() ; le carré (colonne
	 * f, rangée r) couvre x ∈ [2f − 8, 2f − 7], y ∈ [6 − 2r, 7 − 2r] (les blancs en bas).
	 */
	public static void place(Map map, int position, State state, int blocks) {
		String[] ranks = POSITIONS[position].split("/");
		for (int row = 0; row < 8; row++) {
			int r = 7 - row, f = 0;
			for (char ch : ranks[row].toCharArray()) {
				if (Character.isDigit(ch)) {
					f += ch - '0';
					continue;
				}
				int id = OBSTACLE_PIECES + 2 * PIECES.indexOf(Character.toUpperCase(ch)) + (Character.isLowerCase(ch) ? 1 : 0);
				Cell anchor = map.getCell(2 * f - 8 + map.getWidth() - 1, 6 - 2 * r);
				Cell[] covered = anchor == null ? null : map.coveredCells(anchor, 2);
				if (covered != null) {
					anchor.setObstacle(id, 2);
					for (int i = 0; i < covered.length; i++) covered[i].setObstacle(0, -1 - i);
				}
				f++;
			}
		}
		for (int i = 0; i < blocks; i++) {
			Cell c = map.getCell(state.getRandom().getInt(0, map.getNbCell()));
			if (c != null && c.available(map)) {
				c.setObstacle(dark(map, c) ? BLOCK_BLACK : BLOCK_WHITE, 1);
			}
		}
	}

	/** Carré noir du damier du client (carrés de 2x2 cases, a1 noir). */
	static boolean dark(Map map, Cell c) {
		int x = c.getX() - (map.getWidth() - 1), y = c.getY();
		return Math.floorMod(Math.floorDiv(x, 2) + Math.floorDiv(y, 2), 2) != 0;
	}
}
