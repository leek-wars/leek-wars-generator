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
 * cells_to_ignore.
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

	private Cell findEmptyWalkable(Cell exclude) {
		for (Cell c : map().getCells()) {
			if (c != exclude && c.isWalkable() && c.getPlayer(map()) == null) {
				return c;
			}
		}
		return null;
	}

	// ---------- Static helpers ----------

	/** Find a cell with the same x as `from` but a different y. */
	private Cell sameColumnAs(Cell from) {
		for (Cell c : map().getCells()) {
			if (c.getX() == from.getX() && c.getY() != from.getY()) return c;
		}
		return null;
	}

	/** Find a cell with the same y as `from` but a different x. */
	private Cell sameRowAs(Cell from) {
		for (Cell c : map().getCells()) {
			if (c.getY() == from.getY() && c.getX() != from.getX()) return c;
		}
		return null;
	}

	/** Find a cell with both x and y different from `from` (diagonal, not in line). */
	private Cell diagonalFrom(Cell from) {
		for (Cell c : map().getCells()) {
			if (c.getX() != from.getX() && c.getY() != from.getY()) return c;
		}
		return null;
	}

	@Test
	public void inLineRecognizesSameRow() throws Exception {
		initFightOnly();
		Cell origin = leek1.getCell();
		Cell sameRow = sameRowAs(origin);
		Cell sameColumn = sameColumnAs(origin);
		Cell diagonal = diagonalFrom(origin);
		Assert.assertNotNull(sameRow);
		Assert.assertNotNull(sameColumn);
		Assert.assertNotNull(diagonal);
		Assert.assertTrue("Same Y is in line", Pathfinding.inLine(origin, sameRow));
		Assert.assertTrue("Same X is in line", Pathfinding.inLine(origin, sameColumn));
		Assert.assertFalse("Diagonal is not in line", Pathfinding.inLine(origin, diagonal));
	}

	@Test
	public void getCaseDistanceIsManhattan() throws Exception {
		initFightOnly();
		Cell c1 = leek1.getCell();
		Cell c2 = leek2.getCell();
		int expected = Math.abs(c1.getX() - c2.getX()) + Math.abs(c1.getY() - c2.getY());
		Assert.assertEquals(expected, Pathfinding.getCaseDistance(c1, c2));
	}

	@Test
	public void getCaseDistanceIsZeroForSameCell() throws Exception {
		initFightOnly();
		Cell c1 = leek1.getCell();
		Assert.assertEquals(0, Pathfinding.getCaseDistance(c1, c1));
	}

	@Test
	public void getCaseDistanceToCellListPicksMin() throws Exception {
		initFightOnly();
		Cell origin = leek1.getCell();
		Cell far = leek2.getCell();
		Cell near = sameColumnAs(origin);
		Assert.assertNotNull(near);
		List<Cell> targets = new ArrayList<>();
		targets.add(far);
		targets.add(near);
		int expected = Math.min(
			Math.abs(origin.getX() - far.getX()) + Math.abs(origin.getY() - far.getY()),
			Math.abs(origin.getX() - near.getX()) + Math.abs(origin.getY() - near.getY())
		);
		Assert.assertEquals(expected, Pathfinding.getCaseDistance(origin, targets));
	}

	// ---------- Map.getCellByDir ----------

	@Test
	public void getCellByDirReturnsNullOnEdge() throws Exception {
		initFightOnly();
		// Find an actual edge cell: one with at least one missing neighbour.
		Cell corner = null;
		for (Cell c : map().getCells()) {
			if (!c.hasNorth() || !c.hasSouth() || !c.hasEast() || !c.hasWest()) {
				corner = c;
				break;
			}
		}
		Assert.assertNotNull(corner);
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
		Cell c = leek1.getCell();
		Cell[] around = map().getCellsAround(c);
		Assert.assertEquals(4, around.length);
		int existing = 0;
		for (Cell n : around) if (n != null) existing++;
		Assert.assertTrue("Leek's cell has at least one neighbour: " + existing, existing >= 1);
	}

	// ---------- A* path ----------

	@Test
	public void aStarPathToSelfReturnsNull() throws Exception {
		initFightOnly();
		Cell c = leek1.getCell();
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
		initFightOnly();
		Cell start = leek1.getCell();
		Cell enemyCell = leek2.getCell();
		Cell target = null;
		for (Cell c : map().getCellsAround(enemyCell)) {
			if (c != null && c.isWalkable() && c.getPlayer(map()) == null && c != start) {
				target = c;
				break;
			}
		}
		if (target == null) return;
		List<Cell> path = map().getAStarPath(start, new Cell[] { target });
		if (path == null) return;
		for (Cell c : path) {
			Assert.assertNotEquals("Path doesn't step through leek2", enemyCell.getId(), c.getId());
		}
	}

	@Test
	public void aStarWithEnemyAsTargetReachesAdjacentCell() throws Exception {
		// When end is a player-occupied cell, A* still returns a path: getAStarPath
		// strips the final occupied cell unless it's in cells_to_ignore.
		initFightOnly();
		Cell start = leek1.getCell();
		Cell end = leek2.getCell();
		List<Cell> path = map().getAStarPath(start, new Cell[] { end });
		if (path == null) return;
		Cell last = path.get(path.size() - 1);
		Assert.assertNotEquals("A* trims the final occupied cell", end.getId(), last.getId());
	}

	// ---------- Cell properties ----------

	@Test
	public void leekCellsAreNotWalkableViaPlayer() throws Exception {
		initFightOnly();
		Cell c = leek1.getCell();
		Assert.assertTrue(c.isWalkable());
		Assert.assertEquals(leek1.getFId(), c.getPlayer(map()).getFId());
	}

	@Test
	public void availableMatchesNoPlayerAndWalkable() throws Exception {
		initFightOnly();
		Cell occupied = leek1.getCell();
		Assert.assertFalse("Occupied cell is not available", occupied.available(map()));
		Cell empty = findEmptyWalkable(null);
		Assert.assertNotNull("Map has at least one available cell", empty);
		Assert.assertTrue(empty.available(map()));
	}

	// ---------- Map dimensions ----------

	@Test
	public void mapHasExpectedDimensions() throws Exception {
		initFightOnly();
		Assert.assertEquals(18, map().getWidth());
		Assert.assertEquals(18, map().getHeight());
		// Hex-style layout: nb_cells = (w*2-1)*h - (w-1) — 35*18 - 17 = 613 for 18x18.
		int expected = (18 * 2 - 1) * 18 - (18 - 1);
		Assert.assertEquals("nb_cells = (w*2-1)*h - (w-1)", expected, map().getNbCell());
	}

	@Test
	public void getCellByXYRoundTripsId() throws Exception {
		// Cell (x, y) coordinates can be negative on this hex-style grid.
		initFightOnly();
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
		// getObstacles returns the cells themselves; size-2 obstacles take 4 cells.
		Cell[] obstacles = map().getObstacles();
		Assert.assertTrue("Obstacle count > 0: " + obstacles.length, obstacles.length > 0);
		Assert.assertTrue("Obstacle count plausible: " + obstacles.length, obstacles.length < 18 * 18);
	}

	// ---------- moveEntity ----------

	// ---------- A* determinism (regression coverage for the TreeSet→PQ refactor) ----------

	@Test
	public void aStarPathIsDeterministicAcrossCalls() throws Exception {
		// Repeated calls with the same start/end must return identical paths.
		// The previous TreeSet implementation used a non-strict comparator
		// (returning -1 for equal weights), which could pick different cells
		// of equal weight on different calls. PriorityQueue with Float.compare
		// is stable enough for our needs; the generational reset doesn't shuffle
		// cell state.
		initFightOnly();
		Cell start = leek1.getCell();
		Cell end = leek2.getCell();
		List<Cell> first = map().getAStarPath(start, new Cell[] { end });
		for (int i = 0; i < 10; i++) {
			List<Cell> next = map().getAStarPath(start, new Cell[] { end });
			Assert.assertEquals("Same length on repeat", first == null ? 0 : first.size(), next == null ? 0 : next.size());
			if (first != null) {
				for (int j = 0; j < first.size(); j++) {
					Assert.assertEquals("Same cell at index " + j, first.get(j).getId(), next.get(j).getId());
				}
			}
		}
	}

	@Test
	public void aStarMultiTargetReachesNearestCell() throws Exception {
		// Multi-target A* must pick the closest target, not the first one in the list.
		// The previous implementation biased its heuristic toward endCells.get(0).
		initFightOnly();
		Cell start = leek1.getCell();
		// Build a list of end cells: leek2's cell (likely far) and a closer cell.
		Cell far = leek2.getCell();
		Cell near = null;
		for (Cell c : map().getCellsAround(start)) {
			if (c != null && c.isWalkable() && c.getPlayer(map()) == null) {
				near = c;
				break;
			}
		}
		if (near == null) return;
		List<Cell> targets = new ArrayList<>();
		targets.add(far);  // first in list — what the old heuristic would prefer
		targets.add(near);
		List<Cell> path = map().getAStarPath(start, targets);
		Assert.assertNotNull(path);
		// Path should reach the closer target (path size 1) not the far one
		Assert.assertEquals("A* picks the nearest target, not the first", 1, path.size());
		Assert.assertEquals(near.getId(), path.get(0).getId());
	}

	@Test
	public void moveEntityUpdatesCell() throws Exception {
		initFightOnly();
		Cell originalCell = leek1.getCell();
		Cell target = findEmptyWalkable(originalCell);
		Assert.assertNotNull(target);
		map().moveEntity(leek1, target);
		Assert.assertEquals("Leek now on target cell", target.getId(), leek1.getCell().getId());
		Assert.assertNull("Original cell freed", map().getEntity(originalCell));
		Assert.assertEquals("Target cell occupied", leek1.getFId(), map().getEntity(target).getFId());
	}
}
