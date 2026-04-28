package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.maps.Pathfinding;

/**
 * Pathfinding edge cases via the Java Map API: A* result for trivial / blocked
 * / unreachable inputs, isLine / getCaseDistance helpers, getPathBeetween with
 * cells_to_ignore. Complements TestFightMapNatives which probes the same
 * surface from LeekScript.
 */
public class TestFightPathfinding extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private Map map() {
		return fight.getState().getMap();
	}

	// ---------- Static helpers ----------

	@Test
	public void inLineRecognizesSameRow() throws Exception {
		initFightOnly();
		Cell origin = map().getCell(5, 5);
		Cell sameY = map().getCell(8, 5);
		Cell sameX = map().getCell(5, 9);
		Cell diagonal = map().getCell(7, 7);
		if (origin == null || sameY == null || sameX == null || diagonal == null) {
			System.out.println("[SKIP] map cells not available");
			return;
		}
		Assert.assertTrue("Same Y → in line", Pathfinding.inLine(origin, sameY));
		Assert.assertTrue("Same X → in line", Pathfinding.inLine(origin, sameX));
		Assert.assertFalse("Diagonal → not in line", Pathfinding.inLine(origin, diagonal));
	}

	@Test
	public void getCaseDistanceIsManhattan() throws Exception {
		initFightOnly();
		Cell c1 = map().getCell(3, 3);
		Cell c2 = map().getCell(6, 7);
		if (c1 == null || c2 == null) return;
		Assert.assertEquals("Manhattan distance", 7, Pathfinding.getCaseDistance(c1, c2));
	}

	@Test
	public void getCaseDistanceIsZeroForSameCell() throws Exception {
		initFightOnly();
		Cell c1 = map().getCell(3, 3);
		if (c1 == null) return;
		Assert.assertEquals(0, Pathfinding.getCaseDistance(c1, c1));
	}

	@Test
	public void getCaseDistanceToCellListPicksMin() throws Exception {
		initFightOnly();
		Cell origin = map().getCell(0, 0);
		Cell far = map().getCell(10, 10);
		Cell near = map().getCell(2, 1);
		if (origin == null || far == null || near == null) return;
		List<Cell> targets = new ArrayList<>();
		targets.add(far);
		targets.add(near);
		Assert.assertEquals("Min of (20, 3)", 3, Pathfinding.getCaseDistance(origin, targets));
	}

	// ---------- Map.getCellByDir ----------

	@Test
	public void getCellByDirReturnsNullOnEdge() throws Exception {
		initFightOnly();
		// Find a cell on an edge: cell (0,0) — going north or west should fall off.
		Cell corner = map().getCell(0, 0);
		if (corner == null) return;
		// Without knowing which directions are available, just verify the method
		// returns null for at least one direction (the corner can't have all 4).
		int nullCount = 0;
		if (map().getCellByDir(corner, Pathfinding.NORTH) == null) nullCount++;
		if (map().getCellByDir(corner, Pathfinding.SOUTH) == null) nullCount++;
		if (map().getCellByDir(corner, Pathfinding.EAST) == null) nullCount++;
		if (map().getCellByDir(corner, Pathfinding.WEST) == null) nullCount++;
		Assert.assertTrue("Corner cell has at least 2 missing directions", nullCount >= 2);
	}

	@Test
	public void getCellByDirOnNullCellReturnsNull() throws Exception {
		initFightOnly();
		Assert.assertNull(map().getCellByDir(null, Pathfinding.NORTH));
	}

	@Test
	public void getCellsAroundReturnsFourElements() throws Exception {
		initFightOnly();
		// Pick a leek's cell — it's guaranteed to exist
		Cell c = leek1.getCell();
		Cell[] around = map().getCellsAround(c);
		Assert.assertEquals(4, around.length);
		// At least one neighbour must exist (leek can't be on a totally isolated cell)
		int existing = 0;
		for (Cell n : around) if (n != null) existing++;
		Assert.assertTrue("Leek's cell has at least one neighbour: " + existing, existing >= 1);
	}

	// ---------- A* path ----------

	@Test
	public void aStarPathToSelfReturnsNull() throws Exception {
		initFightOnly();
		Cell c = leek1.getCell();
		// endCells.contains(c1) → return null per Map.java:1048-1049
		List<Cell> path = map().getAStarPath(c, new Cell[] { c });
		Assert.assertNull("Path to self is null", path);
	}

	@Test
	public void aStarPathFromNullReturnsNull() throws Exception {
		initFightOnly();
		Cell c = leek1.getCell();
		Assert.assertNull(map().getAStarPath(null, new Cell[] { c }));
	}

	@Test
	public void aStarPathToEmptyListReturnsNull() throws Exception {
		initFightOnly();
		Cell c = leek1.getCell();
		Assert.assertNull(map().getAStarPath(c, new ArrayList<Cell>()));
	}

	@Test
	public void aStarPathBetweenLeeksHasReasonableLength() throws Exception {
		initFightOnly();
		Cell start = leek1.getCell();
		Cell end = leek2.getCell();
		List<Cell> path = map().getAStarPath(start, new Cell[] { end });
		// May be null if leek2 is unreachable, but on a normal random map we expect
		// a path. Just verify length is bounded.
		if (path != null) {
			Assert.assertTrue("Path length > 0", path.size() > 0);
			Assert.assertTrue("Path length <= total cells", path.size() <= map().getNbCell());
		}
	}

	@Test
	public void aStarPathDoesNotIncludeStart() throws Exception {
		initFightOnly();
		Cell start = leek1.getCell();
		Cell end = leek2.getCell();
		List<Cell> path = map().getAStarPath(start, new Cell[] { end });
		if (path != null && !path.isEmpty()) {
			Assert.assertNotEquals("Path's first cell is not the start", start.getId(), path.get(0).getId());
		}
	}

	@Test
	public void getPathBeetweenIsConsistentWithAStar() throws Exception {
		initFightOnly();
		Cell start = leek1.getCell();
		Cell end = leek2.getCell();
		List<Cell> p1 = map().getPathBeetween(start, end, null);
		List<Cell> p2 = map().getAStarPath(start, new Cell[] { end });
		if (p1 == null || p2 == null) {
			Assert.assertEquals(p1 == null, p2 == null);
		} else {
			Assert.assertEquals("Same length via either entry point", p2.size(), p1.size());
		}
	}

	@Test
	public void getPathBeetweenWithNullEndReturnsNull() throws Exception {
		initFightOnly();
		Cell start = leek1.getCell();
		Assert.assertNull(map().getPathBeetween(start, null, null));
		Assert.assertNull(map().getPathBeetween(null, start, null));
	}

	// ---------- A* with player blocking ----------

	@Test
	public void aStarRoutesAroundOccupiedCells() throws Exception {
		// leek2 occupies its own cell. A path from leek1 toward a cell adjacent to
		// leek2 should not step on leek2's cell.
		initFightOnly();
		Cell start = leek1.getCell();
		Cell enemyCell = leek2.getCell();
		Cell[] around = map().getCellsAround(enemyCell);
		Cell target = null;
		for (Cell c : around) {
			if (c != null && c.isWalkable() && c.getPlayer(map()) == null && c != start) {
				target = c;
				break;
			}
		}
		if (target == null) {
			System.out.println("[SKIP] no walkable cell adjacent to leek2");
			return;
		}
		List<Cell> path = map().getAStarPath(start, new Cell[] { target });
		if (path == null) return;
		for (Cell c : path) {
			Assert.assertNotEquals("Path doesn't step through leek2", enemyCell.getId(), c.getId());
		}
	}

	@Test
	public void aStarWithEnemyAsTargetReachesAdjacentCell() throws Exception {
		// Special-case: when end is a player-occupied cell, A* still returns a path
		// (last cell removed at line 1080-1083 if cells_to_ignore doesn't include it).
		initFightOnly();
		Cell start = leek1.getCell();
		Cell end = leek2.getCell();
		List<Cell> path = map().getAStarPath(start, new Cell[] { end });
		if (path == null) return;
		// Last cell in path should NOT be leek2's cell — code strips it.
		Cell last = path.get(path.size() - 1);
		Assert.assertNotEquals("A* trims the final occupied cell", end.getId(), last.getId());
	}

	// ---------- Cell properties ----------

	@Test
	public void leekCellsAreNotWalkableViaPlayer() throws Exception {
		initFightOnly();
		Cell c = leek1.getCell();
		// Cell is walkable (no obstacle), but a player is present
		Assert.assertTrue(c.isWalkable());
		Assert.assertEquals(leek1.getFId(), c.getPlayer(map()).getFId());
	}

	@Test
	public void availableMatchesNoPlayerAndWalkable() throws Exception {
		initFightOnly();
		Cell occupied = leek1.getCell();
		Assert.assertFalse("Occupied cell is not available", occupied.available(map()));

		// Find an unoccupied walkable cell
		Cell empty = null;
		for (Cell c : map().getCells()) {
			if (c.isWalkable() && c.getPlayer(map()) == null) {
				empty = c;
				break;
			}
		}
		Assert.assertNotNull("Map has at least one available cell", empty);
		Assert.assertTrue(empty.available(map()));
	}

	// ---------- Map dimensions ----------

	@Test
	public void mapHasExpectedDimensions() throws Exception {
		initFightOnly();
		// Default map is 18x18 per State.java:429
		Assert.assertEquals(18, map().getWidth());
		Assert.assertEquals(18, map().getHeight());
		// Map cells are laid out on a hexagonal-ish grid: nb_cells = (w*2-1)*h - (w-1)
		// per Map.java:274 — for 18x18 this is 35*18 - 17 = 613.
		int expected = (18 * 2 - 1) * 18 - (18 - 1);
		Assert.assertEquals("nb_cells = (w*2-1)*h - (w-1)", expected, map().getNbCell());
	}

	@Test
	public void getCellByXYRoundTripsId() throws Exception {
		initFightOnly();
		// Cell (x, y) coordinates can be negative on this hex-style grid.
		for (Cell c : map().getCells()) {
			Cell roundTripped = map().getCell(c.getX(), c.getY());
			Assert.assertNotNull("Cell roundtrip not null for id " + c.getId(), roundTripped);
			Assert.assertEquals("Cell round trip: id " + c.getId(), c.getId(), roundTripped.getId());
		}
	}

	// ---------- Obstacles ----------

	@Test
	public void obstacleCellsAreNotWalkable() throws Exception {
		initFightOnly();
		Cell[] obstacles = map().getObstacles();
		Assert.assertNotNull(obstacles);
		for (Cell o : obstacles) {
			Assert.assertFalse("Obstacle cell is not walkable: " + o.getId(), o.isWalkable());
		}
	}

	@Test
	public void obstacleCountInRange() throws Exception {
		initFightOnly();
		// State.java:427 → getRandom().getInt(30, 80) for normal maps
		Cell[] obstacles = map().getObstacles();
		// Note: getObstacles returns the cells themselves; size 2 obstacles take 4 cells
		Assert.assertTrue("Obstacle count > 0: " + obstacles.length, obstacles.length > 0);
		Assert.assertTrue("Obstacle count plausible: " + obstacles.length, obstacles.length < 18 * 18);
	}

	// ---------- moveEntity ----------

	@Test
	public void moveEntityUpdatesCell() throws Exception {
		initFightOnly();
		Cell originalCell = leek1.getCell();
		// Find a different walkable empty cell
		Cell target = null;
		for (Cell c : map().getCells()) {
			if (c.isWalkable() && c.getPlayer(map()) == null && c != originalCell) {
				target = c;
				break;
			}
		}
		Assert.assertNotNull(target);
		map().moveEntity(leek1, target);
		Assert.assertEquals("Leek now on target cell", target.getId(), leek1.getCell().getId());
		Assert.assertNull("Original cell freed", map().getEntity(originalCell));
		Assert.assertEquals("Target cell occupied", leek1.getFId(), map().getEntity(target).getFId());
	}
}
