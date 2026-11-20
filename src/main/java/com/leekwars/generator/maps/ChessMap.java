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

	/**
	 * Positions (placement FEN, 8e rangée d'abord), dans l'ordre du client (chess-positions.ts) : fins de
	 * parties célèbres, vérifiées en rejouant leurs coups, et quelques études.
	 */
	public static final String[] POSITIONS = {
		"1n1Rkb1r/p4ppp/4q3/4p1B1/4P3/8/PPP2PPP/2K5", // Morphy – Brunswick & Isouard, Paris 1858 (Opera Game)
		"r1bk3r/p2pBpNp/n4n2/1p1NP2P/6P1/3P4/P1P1K3/q5b1", // Anderssen – Kieseritzky, London 1851 (Immortal Game)
		"1r3kr1/pbpBBp1p/1b3P2/8/8/2P2q2/P4PPP/3R2K1", // Anderssen – Dufresne, Berlin 1852 (Evergreen Game)
		"1Q6/5pk1/2p3p1/1p2N2p/1b5P/1bn5/2r3P1/2K5", // D. Byrne – Fischer, New York 1956 (Game of the Century)
		"r1k4r/p2nb1p1/2b4p/1p1n1p2/2PP4/3Q1NB1/1P3PPP/R5K1", // Deep Blue – Kasparov, New York 1997 (game 6)
		"rn3r2/pbppq1p1/1p2pN2/8/3P2NP/6P1/PPPKBP1R/R5k1", // Ed. Lasker – Thomas, London 1912
		"rn1q1bnr/ppp1kB1p/3p2p1/3NN3/4P3/8/PPPP1PPP/R1BbK2R", // Legall – Saint-Brie, Paris 1750 (Légal's mate)
		"r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR", // Scholar's mate
		"rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR", // Fool's mate
		"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", // Starting position
		"8/Q6p/6p1/5p2/5P2/2p3P1/3r3P/2K1k3", // Kasparov – Topalov, Wijk aan Zee 1999 (Kasparov's Immortal)
		"6k1/5ppp/pb2p3/1p2P3/1P2bPnP/P6r/1B4QP/R4R1K", // Rotlewi – Rubinstein, Łódź 1907 (Rubinstein's Immortal)
		"5rk1/pp4pp/4p3/2R3Q1/3n4/6qr/P1P2PPP/5RK1", // Levitsky – Marshall, Breslau 1912 (Gold Coins Game)
		"6k1/3q2p1/p2bp2p/3p1r2/1p1Pp3/3bQ1PP/PP1B1rB1/1N2R1RK", // Sämisch – Nimzowitsch, Copenhagen 1923 (Immortal Zugzwang Game)
		"8/7R/5q1k/3Q2N1/3p4/PP3pPP/5n1K/4r3", // Deep Blue – Kasparov, Philadelphia 1996 (game 1)
		"2kr2nr/p2n1ppp/B1p1p3/8/1P1P1B2/2N4P/1PPK1PP1/7q", // Canal – NN, Budapest 1934 (Peruvian Immortal)
		"r4rk1/pp4p1/2p5/3pP1Np/2P3K1/3B2P1/PP5q/R1BQ1RN1", // Glucksberg – Najdorf, Warsaw (Polish Immortal)
		"2k1r2r/ppp3pp/2n5/3B1b2/5P2/b1P1BQ2/P2N1P1P/2KR3R", // Schulder – Boden, London 1853 (Boden's mate)
		"8/5pk1/7p/8/1p4P1/1P1R2P1/3N1qBP/3Nr2K", // Karpov – Kasparov, Moscow 1985 (game 16)
		"8/3R3p/6p1/8/3k1P2/2p3KP/8/4r3", // Botvinnik – Tal, Moscow 1960 (game 6)
		"6q1/p1k5/1pb3n1/5pBP/3R4/P3P3/6P1/3QK3", // Tal – Botvinnik, Moscow 1960 (game 1)
		"1k1r4/2q4p/p2b1p1Q/4pp2/P2Rb3/5N1P/2p2PP1/4R2K", // Ding Liren – Nepomniachtchi, Astana 2023 (game 2)
		"8/3kb3/3Nn1p1/4Q3/1Pp1N1P1/2P5/5PK1/4q3", // Fischer – Spassky, Sveti Stefan 1992 (game 1)
		"3rn3/8/1P3pk1/R6P/p2N1B2/3K1b2/P7/8", // Spassky – Fischer, Sveti Stefan 1992 (game 4)
		"6k1/6r1/p2pq3/2p4R/1n3rPN/1P5P/6QK/8", // Fischer – Spassky, Sveti Stefan 1992 (game 5)
		"8/3P1kP1/5b2/pp3N2/8/3K4/P7/8", // Fischer – Spassky, Sveti Stefan 1992 (game 7)
		"7r/5pk1/2Rp2p1/3Pp1Pp/1p2P2P/4qP2/1PKR4/5B2", // Spassky – Fischer, Sveti Stefan 1992 (game 8)
		"8/2k5/8/1B6/3p1P1P/4b1P1/8/5K2", // Kasparov – Deep Blue, Philadelphia 1996 (game 2)
		"8/5pk1/R4p2/7p/1p1r4/P4KP1/5P1P/8", // Deep Blue – Kasparov, Philadelphia 1996 (game 3)
		"6rk/2Q3p1/5qBp/5P2/8/7P/6PK/8", // Kasparov – Deep Blue, Philadelphia 1996 (game 4)
		"8/p7/8/3KPkpp/1P6/P4P2/2b4P/8", // Deep Blue – Kasparov, Philadelphia 1996 (game 5)
		"rbr5/1q3p2/pPp1pBpk/P1QpP2p/1R5P/6P1/5P2/2R3K1", // Kasparov – Deep Blue, Philadelphia 1996 (game 6)
		"4r3/6P1/2p2P1k/1p6/pP2p1R1/P1B5/2P2K2/3r4", // Kasparov – Deep Blue, New York 1997 (game 1)
		"1r6/5kp1/RqQb1p1p/1p1PpP2/1Pp1B3/2P4P/6P1/5K2", // Deep Blue – Kasparov, New York 1997 (game 2)
		"8/8/1KP5/3r4/8/8/8/k7", // Saavedra position (1895)
		"7K/8/k1P5/7p/8/8/8/8", // Réti study (1921)
		"1K1k4/1P6/8/8/8/8/r7/2R5", // Lucena position
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
