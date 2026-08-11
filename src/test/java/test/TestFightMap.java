package test;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.maps.Pathfinding;
import com.leekwars.generator.state.Team;
import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;

public class TestFightMap {

	private Generator generator;
	private Fight fight;

	@Before
	public void setUp() throws Exception {

		generator = new Generator();
		fight = new Fight(generator);
	}

	@Test
	public void getDistanceTest() throws Exception {
		// On génère une map sans obstacles
		Map map = new Map(18, 18);

		Cell start = map.getCell(5, 0);
		Cell end = map.getCell(7, 1);

		Assert.assertEquals(Map.getDistance2(start, end), 5);
		Assert.assertEquals(Pathfinding.getCaseDistance(start, end), 3);
		Assert.assertEquals(map.getAStarPath(start, new Cell[] { end }).size(), 3);
	}

	@Test
	public void pathAwayFromLineTest() throws Exception {
		// On génère une map sans obstacles
		Map map = new Map(18, 18);

		Cell c = map.getCell(306);
		Cell start = map.getCell(5, 0);
		Cell end = map.getCell(7, 0);

		List<Cell> cells = map.getPathAwayFromLine(c, start, end, 3);
//		map.drawMap(cells);
		Assert.assertNotNull(cells);
		Assert.assertEquals(3, cells.size());
		System.out.println("pathAwayFromLine cells: " + cells.get(0).getId() + ", " + cells.get(1).getId() + ", " + cells.get(2).getId());
		Assert.assertEquals(289, cells.get(0).getId());
		Assert.assertEquals(272, cells.get(1).getId());
		Assert.assertEquals(255, cells.get(2).getId());
//		map.drawMap(cells);
	}

	@Test
	public void astarTest() throws Exception {
		// On génère une map sans obstacles
		Map map = new Map(18, 18);
		// On test un paquet de path au hasard
		for (int i = 1; i < map.getNbCell(); i += 10) {
			for (int j = 0; j < map.getNbCell(); j += 9) {
				List<Cell> path = map.getAStarPath(map.getCell(i), new Cell[] { map.getCell(j) });
				if (i == j)
					Assert.assertTrue(path == null);
				else
					Assert.assertTrue(path != null);
			}
		}
		// On teste différents chemins
		map = Map.generateMap(fight.getState(), 0, 18, 18, 50, new ArrayList<Team>(), null);

		for (int i = 1; i < map.getNbCell(); i += 10) {
			for (int j = 600; j >= 0; j -= 10) {
				Cell c1 = map.getCell(i);
				Cell c2 = map.getCell(j);
				List<Cell> path = map.getAStarPath(c1, new Cell[] { c2 });
				if (!c2.isWalkable() || !c1.isWalkable())
					continue;
				if (c1 == c2 || c1.getComposante() != c2.getComposante()) {
					if (path != null) {
						System.out.println(i + "," + j + " -- " + c1.getComposante() + "," + c2.getComposante());
						System.out.println(c1.getX() + "," + c1.getY() + " -- " + c2.getX() + "," + c2.getY());
//						map.drawMap(path);
						for (Cell c : path) {
							System.out.print(c.getX() + "," + c.getY() + " - ");
						}
						System.out.println(";");
					}
					// Assert.assertTrue(path == null);
				} else
					Assert.assertTrue(path != null);

			}
		}

	}

	@Test
	public void generationTest() throws Exception {
		Leek l1 = new Leek(1, "Bob", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		Leek l2 = new Leek(2, "Martin", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		var team1 = new Team();
		var team2 = new Team();
		team1.addEntity(l1);
		team2.addEntity(l2);
		var teams = new ArrayList<Team>();
		teams.add(team1);
		teams.add(team2);

		Map map = Map.generateMap(fight.getState(), 0, 18, 18, 50, teams, null);
		// On vérifie le nombre de cases
		Assert.assertEquals(613, map.getNbCell());
		// On vérifie que la carte a bien des obstacles
		int obst = 0;
		for (int i = 0; i < map.getNbCell(); i++) {
			if (map.getCell(i).getObstacle() > 0)
				obst++;
		}
		if (obst == 0)
			fail("Pas d'obstacles sur la map");
		// On vérifie que les deux joueurs sont sur la meme composante connexe
		Assert.assertNotNull(l1.getCell());
		Assert.assertNotNull(l2.getCell());
		List<Cell> patj = map.getAStarPath(l1.getCell(), new Cell[] { l2.getCell() });
		Assert.assertNotNull("Les deux joueurs doivent être sur la même composante connexe", patj);
	}

	@Test
	public void astar2Test() throws Exception {

		// On génère une map sans obstacles
		Map map = Map.generateMap(fight.getState(), 0, 18, 18, 100, new ArrayList<Team>(), null);

		long start = System.nanoTime();

		for (int i = 0; i < 1000; ++i) {

			ArrayList<Cell> ends = new ArrayList<Cell>();
			ends.add(map.getCell((int) Math.floor(Math.random() * 613)));
			Cell c = map.getCell((int) Math.floor(Math.random() * 613));

			List<Cell> path = map.getAStarPath(c, ends, null);
			System.out.println(path != null ? path.size() : 0);
		}

		System.out.println("astar time : " + ((System.nanoTime() - start) / 1000000) + " ms");

		/*
		 * start = System.nanoTime(); // List<Cell> path2 =
		 * Pathfinding.getAStarPath(c, ends, null); System.out.println(
		 * "Old A* : " + (System.nanoTime() - start));
		 *
		 * start = System.nanoTime(); // List<Cell> path3 =
		 * Pathfinding.getAStarPath(c, ends, null); System.out.println(
		 * "Old A* : " + (System.nanoTime() - start));
		 *
		 * /* if(path2 != null) System.out.println("Len : " + path2.size());
		 *
		 * if(path != null) map.drawMap(path); else map.drawMap(); if(path2 !=
		 * null){ try{ Thread.sleep(5000); map.drawMap(path2); } catch(Exception
		 * e){ // handle exception } }
		 */

	}

	/**
	 * BFS de référence : sur une grille à coût unitaire, il rend la longueur du
	 * plus court chemin garantie (oracle indépendant de l'A*). Renvoie -1 si
	 * aucun chemin. Longueur = nombre de pas (exclut la case de départ), comme
	 * getAStarPath().size().
	 */
	private int bfsShortestLength(Map map, Cell start, Cell end) {
		if (start == end)
			return 0;
		int n = map.getNbCell();
		int[] dist = new int[n];
		java.util.Arrays.fill(dist, -1);
		java.util.ArrayDeque<Cell> queue = new java.util.ArrayDeque<>();
		dist[start.getId()] = 0;
		queue.add(start);
		while (!queue.isEmpty()) {
			Cell u = queue.poll();
			if (u == end)
				return dist[u.getId()];
			for (Cell c : map.getCellsAround(u)) {
				if (c == null || !c.isWalkable() || dist[c.getId()] != -1)
					continue;
				dist[c.getId()] = dist[u.getId()] + 1;
				queue.add(c);
			}
		}
		return -1;
	}

	/**
	 * Régression du bug #4744 : moveTowardCell(300) depuis la 409 empruntait un
	 * détour de 12 pas au lieu du chemin optimal de 10, si bien que le poireau
	 * n'atteignait jamais sa cible. Cause : java.util.PriorityQueue n'a pas de
	 * decrease-key, donc l'A* fermait une case avec un coût sous-optimal.
	 * On rejoue le layout réel du combat 53208119 (obstacles de la carte +
	 * cases occupées par les entités) et on exige que l'A* rende bien la
	 * longueur minimale (celle du BFS).
	 */
	@Test
	public void astarShortestPathIssue4744() throws Exception {
		Map map = new Map(18, 18);
		// Cases bloquées au moment de l'action : obstacles de la carte + entités.
		int[] blocked = {
			3, 4, 12, 32, 39, 64, 65, 70, 72, 79, 83, 85, 94, 99, 128,
			136, 142, 147, 152, 154, 155, 180, 183, 186, 194, 195, 207, 222, 223, 234,
			237, 240, 248, 249, 251, 252, 274, 293, 295, 313, 319, 322, 327, 328, 337,
			339, 340, 342, 345, 353, 372, 380, 386, 391, 396, 405, 407, 431, 435, 442,
			443, 445, 447, 453, 455, 459, 472, 485, 509, 518, 521, 546, 582, 585, 610,
			612
		};
		for (int id : blocked)
			map.getCell(id).setWalkable(false);

		Cell start = map.getCell(409);
		Cell end = map.getCell(300);
		Assert.assertTrue("409 doit être franchissable", start.isWalkable());
		Assert.assertTrue("300 doit être franchissable", end.isWalkable());

		int optimal = bfsShortestLength(map, start, end);
		Assert.assertTrue("Un chemin 409->300 doit exister", optimal > 0);

		List<Cell> path = map.getAStarPath(start, new Cell[] { end });
		Assert.assertNotNull("L'A* doit trouver un chemin 409->300", path);
		Assert.assertEquals("409->300 arrive bien sur la 300", 300, path.get(path.size() - 1).getId());
		// Avant le correctif : 12 pas (détour), le BFS en donne 10.
		Assert.assertEquals("L'A* doit rendre le plus court chemin", optimal, path.size());
	}

	/**
	 * Propriété générale (fuzz déterministe façon rapport #4744) : sur des
	 * milliers de layouts d'obstacles aléatoires, getAStarPath() doit TOUJOURS
	 * rendre un chemin de longueur minimale, comparé au BFS de référence.
	 */
	@Test
	public void astarAlwaysReturnsShortestPathFuzz() throws Exception {
		java.util.Random rng = new java.util.Random(4744); // seed figée = déterministe
		int suboptimal = 0, tested = 0;
		for (int iter = 0; iter < 400; iter++) {
			Map map = new Map(18, 18);
			int n = map.getNbCell();
			// 15 % à 40 % d'obstacles, comme le fuzz du rapport.
			double density = 0.15 + rng.nextDouble() * 0.25;
			for (int i = 0; i < n; i++) {
				if (rng.nextDouble() < density)
					map.getCell(i).setWalkable(false);
			}
			for (int p = 0; p < 6; p++) {
				Cell a = map.getCell(rng.nextInt(n));
				Cell b = map.getCell(rng.nextInt(n));
				if (a == b || !a.isWalkable() || !b.isWalkable())
					continue;
				int bfs = bfsShortestLength(map, a, b);
				List<Cell> path = map.getAStarPath(a, new Cell[] { b });
				tested++;
				if (bfs < 0) {
					Assert.assertNull("Pas de chemin BFS => A* doit rendre null (" + a.getId() + "->" + b.getId() + ")", path);
				} else {
					Assert.assertNotNull("Chemin BFS existe => A* doit en trouver un (" + a.getId() + "->" + b.getId() + ")", path);
					if (path.size() != bfs) {
						suboptimal++;
						System.out.println("Sous-optimal " + a.getId() + "->" + b.getId() + " : A*=" + path.size() + " BFS=" + bfs);
					}
				}
			}
		}
		System.out.println("Fuzz A* : " + tested + " trajets testés, " + suboptimal + " sous-optimaux");
		Assert.assertEquals("Aucun chemin ne doit être sous-optimal", 0, suboptimal);
	}

	@Test
	public void repelDistanceTest() throws Exception {
		Map map = new Map(18, 18);

		Cell caster = map.getCell(5, 0);
		Cell entity = map.getCell(6, 0); // juste à côté, direction +x

		// Repoussée de 3 cases : l'entité s'éloigne du lanceur d'exactement 3 cases
		Cell destination = map.getRepelLastAvailableCell(entity, caster, 3);
		Assert.assertEquals(9, destination.getX());
		Assert.assertEquals(0, destination.getY());

		// Distance 0 : aucun déplacement
		Assert.assertEquals(entity, map.getRepelLastAvailableCell(entity, caster, 0));

		// Même case que le lanceur : pas de direction, aucun déplacement
		Assert.assertEquals(caster, map.getRepelLastAvailableCell(caster, caster, 3));
	}

	@Test
	public void repelStopsOnObstacleTest() throws Exception {
		Map map = new Map(18, 18);

		Cell caster = map.getCell(5, 0);
		Cell entity = map.getCell(6, 0);
		map.getCell(8, 0).setObstacle(1, 1); // barrage à 2 cases

		// La repoussée s'arrête devant l'obstacle au lieu de le traverser
		Cell destination = map.getRepelLastAvailableCell(entity, caster, 3);
		Assert.assertEquals(7, destination.getX());
	}
}
