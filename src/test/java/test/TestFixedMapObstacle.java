package test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.maps.Pathfinding;
import com.leekwars.generator.util.Json;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Regression coverage for 5pilow/leek-wars#2713 : sur les maps fixes (arenes de
 * boss), les obstacles 2x2 doivent bloquer leurs 4 cases dans le moteur, comme
 * ils sont dessines cote client. ObstacleInfo etait desynchronisee : l'obstacle
 * 31 (pebble) etait declare taille 1 -> seule la case d'ancrage etait bloquee,
 * les 3 autres restaient traversables alors qu'un mur y etait affiche.
 */
public class TestFixedMapObstacle extends FightTestBase {

	/** Obstacles de taille 2 (pebble) à 5. */
	private static final int PEBBLE = 31, SIZE3 = 51, SIZE4 = 39, SIZE5 = 60;
	/** Emprise des tailles 3 à 5 autour de l'ancrage (getNextCell), dans l'ordre de marquage. */
	private static final int[][] SIZE3_OFFSETS = { {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1} };
	private static final int[][] SIZE4_OFFSETS = { {-3, 0} };
	private static final int[][] SIZE5_OFFSETS = { {0, -1}, {0, 3}, {2, -1}, {2, 0}, {2, 3} };

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** Construit une map fixe (id 1) avec un seul obstacle d'id donne a la case d'ancrage. */
	private ObjectNode customMapWithObstacle(int anchorCell, int obstacleId) {
		ObjectNode map = Json.createObject();
		map.put("id", 1);
		ObjectNode obstacles = Json.createObject();
		obstacles.put(String.valueOf(anchorCell), obstacleId);
		map.set("obstacles", obstacles);
		map.set("pattern", Json.createArray());
		// Place les poireaux dans deux coins opposes, loin de l'ancrage central.
		ArrayNode team1 = Json.createArray(); team1.add(0);
		ArrayNode team2 = Json.createArray(); team2.add(612);
		map.set("team1", team1);
		map.set("team2", team2);
		return map;
	}

	@Test
	public void pebble2x2BlocksFourCells() throws Exception {
		final int anchor = 306; // centre (0, 0)
		fight.getState().setCustomMap(customMapWithObstacle(anchor, PEBBLE));
		initFightOnly();

		Map map = fight.getState().getMap();
		Cell a = map.getCell(anchor);
		Cell east = map.getCellByDir(a, Pathfinding.EAST);
		Cell south = map.getCellByDir(a, Pathfinding.SOUTH);
		Cell se = map.getCellByDir(south, Pathfinding.EAST);

		// L'ancrage porte bien l'obstacle d'id 31, taille 2.
		Assert.assertEquals("obstacle id", PEBBLE, a.getObstacle());
		Assert.assertEquals("obstacle size", 2, a.getObstacleSize());

		// Les 4 cases de l'empreinte 2x2 sont bloquees.
		Assert.assertFalse("ancrage non franchissable", a.isWalkable());
		Assert.assertFalse("est non franchissable", east.isWalkable());
		Assert.assertFalse("sud non franchissable", south.isWalkable());
		Assert.assertFalse("sud-est non franchissable", se.isWalkable());

		// Controle : une case voisine hors empreinte (ouest) reste franchissable.
		Cell west = map.getCellByDir(a, Pathfinding.WEST);
		Assert.assertTrue("ouest (hors empreinte) franchissable", west.isWalkable());
	}

	/**
	 * 5pilow/leek-wars#4343 : une map de test qui porte un obstacle hors de la grille
	 * (case inexistante) faisait lever un NullPointerException, remonté en erreur serveur
	 * à chaque combat. L'obstacle est simplement ignoré, les autres sont posés.
	 */
	@Test
	public void obstacleOutsideGridIsIgnored() throws Exception {
		ObjectNode customMap = customMapWithObstacle(306, 42);
		customMap.put("id", 0); // map de test (éditeur), pas une arène fixe
		((ObjectNode) customMap.get("obstacles")).put("9999", 1);
		((ObjectNode) customMap.get("obstacles")).put("-3", 1);
		fight.getState().setCustomMap(customMap);

		var reported = captureReportedErrors(this::initFightOnly);
		Assert.assertTrue("aucune erreur remontée : " + reported, reported.isEmpty());
		Assert.assertFalse("l'obstacle valide est posé", fight.getState().getMap().getCell(306).isWalkable());
	}

	/**
	 * Un obstacle dont la case d'ancrage est dans la grille mais dont l'emprise en déborde
	 * (bord est/sud pour un 2x2, bords quelconques pour les tailles 3 à 5) n'est pas posé du
	 * tout : avant, l'ancrage était marqué puis setObstacle sur la case manquante levait un
	 * NullPointerException, remonté en erreur serveur, et l'obstacle restait à moitié posé.
	 */
	@Test
	public void obstacleOverflowingGridIsIgnoredWhole() throws Exception {
		// Emprises calculées sur une grille témoin de même taille que celle du combat
		Map grid = new Map(18, 18);
		java.util.Map<Integer, Integer> overflowing = new LinkedHashMap<>();
		java.util.Map<Integer, Cell[]> footprints = new HashMap<>();
		overflowing.put(17, PEBBLE); // pas de case à l'est
		footprints.put(17, footprint2x2(grid, grid.getCell(17)));
		overflowing.put(70, PEBBLE); // pas de case au sud
		footprints.put(70, footprint2x2(grid, grid.getCell(70)));
		overflowing.put(52, SIZE3);
		footprints.put(52, footprint(grid, grid.getCell(52), SIZE3_OFFSETS));
		overflowing.put(140, SIZE4);
		footprints.put(140, footprint(grid, grid.getCell(140), SIZE4_OFFSETS));
		overflowing.put(105, SIZE5);
		footprints.put(105, footprint(grid, grid.getCell(105), SIZE5_OFFSETS));
		for (var e : footprints.entrySet()) {
			Assert.assertTrue("l'emprise de " + e.getKey() + " déborde bien de la grille",
				Arrays.stream(e.getValue()).anyMatch(c -> c == null));
		}

		ObjectNode customMap = customMapWithObstacle(306, PEBBLE); // témoin entièrement dans la grille
		for (var e : overflowing.entrySet()) {
			((ObjectNode) customMap.get("obstacles")).put(String.valueOf(e.getKey()), e.getValue());
		}
		fight.getState().setCustomMap(customMap);
		var reported = captureReportedErrors(this::initFightOnly);
		Assert.assertTrue("aucune erreur remontée : " + reported, reported.isEmpty());

		Map map = fight.getState().getMap();
		for (var e : footprints.entrySet()) {
			Assert.assertTrue("ancrage " + e.getKey() + " libre", map.getCell(e.getKey()).isWalkable());
			for (Cell c : e.getValue()) {
				if (c != null) Assert.assertTrue("case " + c.getId() + " de l'emprise de " + e.getKey() + " libre", map.getCell(c.getId()).isWalkable());
			}
		}
		for (Cell c : footprint2x2(map, map.getCell(306))) {
			Assert.assertFalse("le témoin 2x2 est posé en entier (" + c.getId() + ")", c.isWalkable());
		}
	}

	/** Les obstacles de taille 2 à 5 entièrement dans la grille marquent toujours leur emprise, codes compris. */
	@Test
	public void fullFootprintObstaclesAreMarked() throws Exception {
		ObjectNode customMap = customMapWithObstacle(221, PEBBLE);
		ObjectNode obstacles = (ObjectNode) customMap.get("obstacles");
		obstacles.put("252", SIZE3);
		obstacles.put("396", SIZE4);
		obstacles.put("356", SIZE5);
		fight.getState().setCustomMap(customMap);
		initFightOnly();

		Map map = fight.getState().getMap();
		Cell[] pebble = footprint2x2(map, map.getCell(221));
		Assert.assertEquals(PEBBLE, map.getCell(221).getObstacle());
		Assert.assertEquals(2, map.getCell(221).getObstacleSize());
		for (int i = 0; i < pebble.length; i++) {
			Assert.assertFalse(pebble[i].isWalkable());
			Assert.assertEquals("code de la case " + i + " du 2x2", -1 - i, pebble[i].getObstacleSize());
		}
		assertMarked(map, 252, SIZE3, 3, SIZE3_OFFSETS);
		assertMarked(map, 396, SIZE4, 4, SIZE4_OFFSETS);
		assertMarked(map, 356, SIZE5, 5, SIZE5_OFFSETS);
	}

	/** Est, sud et sud-est d'un 2x2, dans l'ordre de marquage (null hors de la grille). */
	private static Cell[] footprint2x2(Map map, Cell anchor) {
		Cell south = map.getCellByDir(anchor, Pathfinding.SOUTH);
		return new Cell[] { map.getCellByDir(anchor, Pathfinding.EAST), south, map.getCellByDir(south, Pathfinding.EAST) };
	}

	private static Cell[] footprint(Map map, Cell anchor, int[][] offsets) {
		Cell[] cells = new Cell[offsets.length];
		for (int i = 0; i < offsets.length; i++) cells[i] = map.getNextCell(anchor, offsets[i][0], offsets[i][1]);
		return cells;
	}

	private static void assertMarked(Map map, int anchorId, int obstacleId, int size, int[][] offsets) {
		Cell anchor = map.getCell(anchorId);
		Assert.assertEquals(obstacleId, anchor.getObstacle());
		Assert.assertEquals(size, anchor.getObstacleSize());
		Assert.assertFalse(anchor.isWalkable());
		for (Cell c : footprint(map, anchor, offsets)) {
			Assert.assertFalse("case " + c.getId() + " de l'obstacle " + anchorId, c.isWalkable());
			Assert.assertEquals(-1, c.getObstacleSize());
		}
	}

	@Test
	public void size1ObstacleBlocksSingleCell() throws Exception {
		final int anchor = 306;
		final int BAMBOO = 42; // taille 1, a toujours fonctionne : sert de temoin
		fight.getState().setCustomMap(customMapWithObstacle(anchor, BAMBOO));
		initFightOnly();

		Map map = fight.getState().getMap();
		Cell a = map.getCell(anchor);
		Assert.assertFalse("ancrage bloque", a.isWalkable());
		Assert.assertTrue("est libre", map.getCellByDir(a, Pathfinding.EAST).isWalkable());
		Assert.assertTrue("sud libre", map.getCellByDir(a, Pathfinding.SOUTH).isWalkable());
	}
}
