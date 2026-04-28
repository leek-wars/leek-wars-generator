package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.Order;

/**
 * Direct unit tests on {@link com.leekwars.generator.state.Order}. Probes the
 * mutator semantics (next, addSummon, removeEntity position adjustment, turn
 * rollback when the last entity is removed).
 *
 * Frequency-based starting order is tested through Fight runs in
 * TestFightConfig#sameSeedProducesSameResult.
 */
public class TestFightOrder extends FightTestBase {

	private Leek a, b, c, d;

	@Override
	protected void createLeeks() {
		a = defaultLeek(1, "A");
		b = defaultLeek(2, "B");
		c = defaultLeek(3, "C");
		d = defaultLeek(4, "D");
		fight.getState().addEntity(0, a);
		fight.getState().addEntity(1, b);
		fight.getState().addEntity(0, c);
		fight.getState().addEntity(1, d);
	}

	// ---------- Empty Order ----------

	@Test
	public void emptyOrderHasNoCurrent() {
		Order o = new Order();
		Assert.assertNull(o.current());
		Assert.assertEquals(1, o.getTurn());
		Assert.assertEquals(0, o.getPosition());
	}

	@Test
	public void getEntityTurnOrderForUnknownReturnsZero() {
		// indexOf returns -1 → +1 → 0
		Order o = new Order();
		Assert.assertEquals(0, o.getEntityTurnOrder(a));
	}

	// ---------- Basic flow ----------

	@Test
	public void nextWrapsAndIncrementsTurn() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		Assert.assertEquals(a, o.current());
		Assert.assertEquals(1, o.getTurn());
		Assert.assertFalse("next on first→second is not a turn boundary", o.next());
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(1, o.getTurn());
		Assert.assertTrue("next from last to first signals new turn", o.next());
		Assert.assertEquals(a, o.current());
		Assert.assertEquals(2, o.getTurn());
	}

	@Test
	public void getNextPlayerWrapsAround() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		Assert.assertEquals(b, o.getNextPlayer()); // pos=0 → next is index 1
		o.next();
		Assert.assertEquals(c, o.getNextPlayer()); // pos=1 → next is 2
		o.next();
		Assert.assertEquals(a, o.getNextPlayer()); // pos=2 → wraps to 0
	}

	@Test
	public void getPreviousPlayerWrapsAround() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		Assert.assertEquals(c, o.getPreviousPlayer()); // pos=0 → wraps to 2
		o.next();
		Assert.assertEquals(a, o.getPreviousPlayer());
	}

	@Test
	public void getNextPlayerByEntityWrapsAround() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		Assert.assertEquals(b, o.getNextPlayer(a));
		Assert.assertEquals(c, o.getNextPlayer(b));
		Assert.assertEquals(a, o.getNextPlayer(c));
	}

	@Test
	public void getNextPlayerForUnknownReturnsNull() {
		Order o = new Order();
		o.addEntity(a);
		Assert.assertNull(o.getNextPlayer(b));
		Assert.assertNull(o.getPreviousPlayer(b));
	}

	@Test
	public void getEntityTurnOrderIsOneIndexed() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		Assert.assertEquals(1, o.getEntityTurnOrder(a));
		Assert.assertEquals(2, o.getEntityTurnOrder(b));
		Assert.assertEquals(3, o.getEntityTurnOrder(c));
	}

	// ---------- addEntity(index, ...) ----------

	@Test
	public void addEntityAtIndexShiftsPositionWhenInserted_atOrBeforeCurrent() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		o.next(); // position = 1 (b)
		Assert.assertEquals(b, o.current());
		o.addEntity(0, d);
		// Inserting at 0 (≤ position=1) bumps position to 2 — current still b
		Assert.assertEquals("Insert before current preserves current entity", b, o.current());
		Assert.assertEquals(2, o.getPosition());
	}

	@Test
	public void addEntityAtIndexAfterCurrentDoesNotShift() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		o.next(); // position = 1 (b)
		o.addEntity(3, d); // append after c
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(1, o.getPosition());
		Assert.assertEquals(d, o.getEntities().get(3));
	}

	// ---------- addSummon ----------

	@Test
	public void addSummonInsertsAfterOwner() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		o.addSummon(b, d);
		Assert.assertEquals(a, o.getEntities().get(0));
		Assert.assertEquals(b, o.getEntities().get(1));
		Assert.assertEquals(d, o.getEntities().get(2));
		Assert.assertEquals(c, o.getEntities().get(3));
	}

	@Test
	public void addSummonForUnknownOwnerIsNoOp() {
		Order o = new Order();
		o.addEntity(a);
		o.addSummon(b, d); // b not in order
		Assert.assertEquals(1, o.getEntities().size());
	}

	@Test
	public void addSummonDoesNotShiftPositionWhenOwnerIsCurrent() {
		// Quirk: addSummon uses raw List.add(idx, ...) — doesn't bump `position` like
		// addEntity(int, Entity) does. When owner == current, summon is at idx+1
		// (after position), so `current()` is unchanged. This is the production behavior.
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c); // pos=0 (a)
		o.next(); // pos=1 (b)
		o.addSummon(b, d);
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(1, o.getPosition());
		Assert.assertEquals(d, o.getEntities().get(2));
	}

	// ---------- removeEntity ----------

	@Test
	public void removeEntityBeforePositionDecrementsPosition() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		o.next();
		o.next(); // pos=2 (c)
		o.removeEntity(a);
		Assert.assertEquals("Removing before current keeps current entity", c, o.current());
		Assert.assertEquals(1, o.getPosition());
	}

	@Test
	public void removeEntityAtPositionShiftsToNext() {
		// Removing the current entity: position is decremented (covering removed slot
		// in the < position case actually fires for ==), then list.remove() naturally
		// moves the next entity into the freed index.
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		o.next(); // pos=1 (b)
		o.removeEntity(b);
		// b removed: list = [a, c], position = 0 (decremented from 1 because <= 1)
		Assert.assertEquals(a, o.current());
		Assert.assertEquals(0, o.getPosition());
	}

	@Test
	public void removeEntityAfterPositionDoesNotMove() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.addEntity(c);
		o.next(); // pos=1 (b)
		o.removeEntity(c);
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(1, o.getPosition());
	}

	@Test
	public void removeFirstEntityOnFirstTurnRollsTurnBack() {
		// Edge case: remove the only remaining entity at position 0 → position becomes
		// -1, code resets to size-1 and decrements turn.
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		Assert.assertEquals(a, o.current()); // pos=0
		o.removeEntity(a); // pos becomes -1 → resets to 0 (size-1=0), turn-- to 0
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(0, o.getPosition());
		Assert.assertEquals("Turn was decremented (caller will increment via next())", 0, o.getTurn());
	}

	@Test
	public void removeUnknownEntityIsNoOp() {
		Order o = new Order();
		o.addEntity(a);
		o.addEntity(b);
		o.removeEntity(c);
		Assert.assertEquals(2, o.getEntities().size());
		Assert.assertEquals(0, o.getPosition());
	}

	// ---------- Order copy constructor ----------

	@Test
	public void copyConstructorPreservesState() throws Exception {
		// Use the Fight's State which has a real Order populated by initFight.
		initFightOnly();
		Order original = fight.getState().getOrder();
		original.next();
		Order copy = new Order(original, fight.getState());
		Assert.assertEquals(original.getPosition(), copy.getPosition());
		Assert.assertEquals(original.getEntities().size(), copy.getEntities().size());
		Assert.assertEquals(original.current().getFId(), copy.current().getFId());
	}

	// ---------- Through real fight ----------

	@Test
	public void getEntitiesContainsAllInitiallyAddedLeeks() throws Exception {
		initFightOnly();
		Assert.assertEquals(4, fight.getState().getOrder().getEntities().size());
	}

	@Test
	public void turnAdvancesAfterEachLeekActs() throws Exception {
		// Each leek records `getTurn()` once. After all four have acted, getTurn is 1
		// for the entire turn 1 — turn 2 starts only after entity #4 ends turn.
		String aiCode = "global t = ''; t = t + getTurn() + ','; setRegister('turns', t);";
		attachAI(a, aiCode);
		attachAI(b, aiCode);
		attachAI(c, aiCode);
		attachAI(d, aiCode);
		// Set max_turns=2 so we can see turns 1 and 2 only
		fight.setMaxTurns(2);
		runFight();
		// Each leek runs once per turn, so each should have "1,2,"
		String aTurns = a.getRegister("turns");
		Assert.assertNotNull(aTurns);
		Assert.assertTrue("a should see turns 1 then 2: " + aTurns, aTurns.startsWith("1,"));
		Assert.assertTrue("a should see turn 2: " + aTurns, aTurns.contains("2,"));
	}

	@Test
	public void deathRemovesEntityFromOrder() throws Exception {
		initFightOnly();
		int sizeBefore = fight.getState().getOrder().getEntities().size();
		// Kill `a` directly
		fight.getState().onPlayerDie(a, b, null);
		Assert.assertEquals(sizeBefore - 1, fight.getState().getOrder().getEntities().size());
		Assert.assertFalse("Dead entity removed from order",
			fight.getState().getOrder().getEntities().contains(a));
	}
}
