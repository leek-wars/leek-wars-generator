package test;

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
		final int PEBBLE = 31;
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
