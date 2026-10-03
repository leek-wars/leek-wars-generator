package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.maps.Pathfinding;
import com.leekwars.generator.util.Json;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * moveTowardCell vers une case obstacle : le poireau s'en approche au plus près. Le but
 * était une case au bord du BLOC d'obstacles connexes : le poireau s'arrêtait au premier
 * obstacle du bloc, parfois loin de la cible, ou ne bougeait pas sur un îlot fermé.
 * Cartes reprises de combats de test d'un rapport du forum.
 */
public class TestMoveTowardObstacle extends FightTestBase {

	private static final String BLOCKS = "{\"10\":1,\"109\":1,\"130\":1,\"136\":1,\"149\":1,\"152\":2,\"154\":1,\"158\":2,\"160\":2,\"164\":2,\"166\":2,\"189\":2,\"200\":1,\"208\":1,\"229\":1,\"230\":1,\"236\":1,\"247\":1,\"248\":2,\"256\":2,\"257\":1,\"260\":2,\"267\":1,\"286\":2,\"3\":1,\"301\":1,\"314\":1,\"316\":1,\"326\":2,\"329\":1,\"332\":1,\"333\":2,\"363\":2,\"379\":1,\"383\":1,\"4\":2,\"403\":1,\"406\":2,\"414\":2,\"419\":1,\"447\":1,\"492\":2,\"493\":1,\"506\":1,\"51\":2,\"529\":1,\"549\":2,\"557\":2,\"558\":1,\"56\":2,\"57\":1,\"578\":1,\"581\":1,\"582\":1,\"588\":1,\"59\":2,\"593\":1,\"6\":2,\"60\":1,\"600\":1,\"601\":1,\"63\":1,\"65\":1,\"84\":1,\"88\":1,\"89\":2}";
	private static final String ISLAND = "{\"235\":1,\"252\":1,\"253\":1,\"269\":1,\"271\":1,\"286\":1,\"289\":1,\"303\":1,\"305\":1,\"307\":1,\"321\":1,\"324\":1,\"339\":1,\"341\":1,\"357\":1,\"358\":1,\"375\":1}";
	private static final String WALL = "{\"1\":1,\"106\":1,\"124\":1,\"141\":1,\"18\":1,\"19\":1,\"36\":1,\"54\":1,\"71\":1,\"72\":1,\"89\":1}";

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, defaultLeek(2, "L2"));
	}

	private void setup(String obstacles, int cell1, int cell2, int mp) throws Exception {
		leek1.setMP(mp);
		ObjectNode map = Json.createObject();
		map.put("id", 1);
		map.set("obstacles", Json.parse(obstacles));
		map.set("pattern", Json.createArray());
		ArrayNode team1 = Json.createArray(); team1.add(cell1);
		ArrayNode team2 = Json.createArray(); team2.add(cell2);
		map.set("team1", team1);
		map.set("team2", team2);
		fight.getState().setCustomMap(map);
		initFightOnly();
	}

	private Map map() {
		return fight.getState().getMap();
	}

	private int distanceTo(int target) {
		return Pathfinding.getCaseDistance(leek1.getCell(), map().getCell(target));
	}

	@Test
	public void obstacleInBlockUsesAllMovePoints() throws Exception {
		setup(BLOCKS, 144, 612, 6);
		Assert.assertEquals(8, distanceTo(3));
		var path = map().getPathToClosestReachableCell(leek1.getCell(), map().getCell(3));
		Assert.assertEquals(7, path.size());
		Assert.assertEquals(20, path.get(path.size() - 1).getId());
		// Avant : 3 PM, arrêt en 90 au bord du bloc, toujours à 8 cases de la cible
		Assert.assertEquals(6, fight.getState().moveTowardCell(leek1, 3, -1));
		Assert.assertTrue(distanceTo(3) < 8);
	}

	@Test
	public void obstacleInBlockReachesClosestCell() throws Exception {
		setup(BLOCKS, 144, 612, 30);
		Assert.assertEquals(7, fight.getState().moveTowardCell(leek1, 3, -1));
		Assert.assertEquals(20, leek1.getCell().getId());
		// Déjà au plus près : plus rien à gagner, pas de déplacement
		Assert.assertEquals(0, fight.getState().moveTowardCell(leek1, 3, -1));
	}

	@Test
	public void obstacleInClosedIsland() throws Exception {
		setup(ISLAND, 351, 577, 30);
		Assert.assertEquals(13, distanceTo(305));
		// Avant : aucun voisin du bloc atteignable, le poireau ne bougeait pas
		Assert.assertTrue(fight.getState().moveTowardCell(leek1, 305, -1) > 0);
		Assert.assertEquals(3, distanceTo(305));
	}

	@Test
	public void obstacleAtEndOfWall() throws Exception {
		setup(WALL, 478, 14, 30);
		// Avant : arrêt en 90, au pied d'un autre obstacle du mur
		fight.getState().moveTowardCell(leek1, 1, -1);
		Assert.assertEquals(2, distanceTo(1));
	}

	@Test
	public void isolatedObstacleGoalIsAFreeNeighbour() throws Exception {
		setup("{\"306\":1}", 0, 612, 30);
		int distance = distanceTo(306);
		// Carte ouverte : chemin le plus court jusqu'à une case collée à l'obstacle
		Assert.assertEquals(distance - 1, fight.getState().moveTowardCell(leek1, 306, -1));
		Assert.assertEquals(1, distanceTo(306));
	}
}
