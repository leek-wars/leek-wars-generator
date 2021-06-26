package test;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.Team;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.maps.Pathfinding;

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

		Assert.assertEquals(Pathfinding.getDistance2(start, end), 5);
		Assert.assertEquals(Pathfinding.getCaseDistance(start, end), 3);
		Assert.assertEquals(Pathfinding.getAStarPath(null, start, new Cell[] { end }).size(), 3);
	}

	@Test
	public void pathAwayFromLineTest() throws Exception {
		// On génère une map sans obstacles
		Map map = new Map(18, 18);

		Cell c = map.getCell(306);
		Cell start = map.getCell(5, 0);
		Cell end = map.getCell(7, 0);

		List<Cell> cells = Pathfinding.getPathAwayFromLine(null, c, start, end, 3);
//		map.drawMap(cells);
		Assert.assertNotNull(cells);
		Assert.assertEquals(cells.size(), 3);
		Assert.assertEquals(cells.get(0).getId(), 323);
		Assert.assertEquals(cells.get(1).getId(), 340);
		Assert.assertEquals(cells.get(2).getId(), 357);
//		map.drawMap(cells);
	}

	@Test
	public void astarTest() throws Exception {
		// On génère une map sans obstacles
		Map map = new Map(18, 18);
		// On test un paquet de path au hasard
		for (int i = 1; i < map.getNbCell(); i += 10) {
			for (int j = 0; j < map.getNbCell(); j += 9) {
				List<Cell> path = Pathfinding.getAStarPath(null, map.getCell(i), new Cell[] { map.getCell(j) });
				if (i == j)
					Assert.assertTrue(path == null);
				else
					Assert.assertTrue(path != null);
			}
		}
		// On teste différents chemins
		map = Map.generateMap(fight, 0, 18, 18, 50, new ArrayList<Team>(), null);

		for (int i = 1; i < map.getNbCell(); i += 10) {
			for (int j = 600; j >= 0; j -= 10) {
				Cell c1 = map.getCell(i);
				Cell c2 = map.getCell(j);
				List<Cell> path = Pathfinding.getAStarPath(null, c1, new Cell[] { c2 });
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
		Leek l1 = new Leek(1, "Bob");
		Leek l2 = new Leek(2, "Martin");
		List<Entity> t1 = new ArrayList<Entity>();
		List<Entity> t2 = new ArrayList<Entity>();
		t1.add(l1);
		t2.add(l2);

		Map map = Map.generateMap(fight, 0, 18, 18, 50, new ArrayList<Team>(), null);
		// On vérifie le nombre de cases
		Assert.assertEquals(map.getNbCell(), 613);
		// On vérifie que la carte a bien des obstacles
		int obst = 0;
		for (int i = 0; i < map.getNbCell(); i++) {
			if (map.getCell(i).getObstacle() > 0)
				obst++;
		}
		if (obst == 0)
			fail("Pas d'obstacles sur la map");
		// On vérifie que les deux joueurs sont sur la meme composante connexe
		List<Cell> patj = Pathfinding.getAStarPath(null, l1.getCell(), new Cell[] { l2.getCell() });
//		map.drawMap();
		Assert.assertFalse(patj == null);
	}

	@Test
	public void astar2Test() throws Exception {

		// On génère une map sans obstacles
		Map map = Map.generateMap(fight, 0, 18, 18, 100, new ArrayList<Team>(), null);

		long start = System.nanoTime();

		for (int i = 0; i < 1000; ++i) {

			ArrayList<Cell> ends = new ArrayList<Cell>();
			ends.add(map.getCell((int) Math.floor(Math.random() * 613)));
			Cell c = map.getCell((int) Math.floor(Math.random() * 613));

			List<Cell> path = Pathfinding.getAStarPath(null, c, ends, null);
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
}
