package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.Order;

/**
 * Direct unit tests on {@link com.leekwars.generator.state.Order}: mutator
 * semantics (next, addSummon, removeEntity position adjustment, turn rollback
 * when the last entity is removed).
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

	private Order orderOf(Leek... entities) {
		Order o = new Order();
		for (Leek l : entities) o.addEntity(l);
		return o;
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
		Order o = new Order();
		Assert.assertEquals(0, o.getEntityTurnOrder(a));
	}

	// ---------- Basic flow ----------

	@Test
	public void nextWrapsAndIncrementsTurn() {
		Order o = orderOf(a, b);
		Assert.assertEquals(a, o.current());
		Assert.assertEquals(1, o.getTurn());
		Assert.assertFalse("next on first to second is not a turn boundary", o.next());
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(1, o.getTurn());
		Assert.assertTrue("next from last to first signals new turn", o.next());
		Assert.assertEquals(a, o.current());
		Assert.assertEquals(2, o.getTurn());
	}

	@Test
	public void getNextPlayerWrapsAround() {
		Order o = orderOf(a, b, c);
		Assert.assertEquals(b, o.getNextPlayer());
		o.next();
		Assert.assertEquals(c, o.getNextPlayer());
		o.next();
		Assert.assertEquals(a, o.getNextPlayer());
	}

	@Test
	public void getPreviousPlayerWrapsAround() {
		Order o = orderOf(a, b, c);
		Assert.assertEquals(c, o.getPreviousPlayer());
		o.next();
		Assert.assertEquals(a, o.getPreviousPlayer());
	}

	@Test
	public void getNextPlayerByEntityWrapsAround() {
		Order o = orderOf(a, b, c);
		Assert.assertEquals(b, o.getNextPlayer(a));
		Assert.assertEquals(c, o.getNextPlayer(b));
		Assert.assertEquals(a, o.getNextPlayer(c));
	}

	@Test
	public void getNextPlayerForUnknownReturnsNull() {
		Order o = orderOf(a);
		Assert.assertNull(o.getNextPlayer(b));
		Assert.assertNull(o.getPreviousPlayer(b));
	}

	@Test
	public void getEntityTurnOrderIsOneIndexed() {
		Order o = orderOf(a, b, c);
		Assert.assertEquals(1, o.getEntityTurnOrder(a));
		Assert.assertEquals(2, o.getEntityTurnOrder(b));
		Assert.assertEquals(3, o.getEntityTurnOrder(c));
	}

	// ---------- addEntity(index, ...) ----------

	@Test
	public void addEntityAtIndexShiftsPositionWhenInsertedAtOrBeforeCurrent() {
		Order o = orderOf(a, b, c);
		o.next();
		Assert.assertEquals(b, o.current());
		o.addEntity(0, d);
		Assert.assertEquals("Insert before current preserves current entity", b, o.current());
		Assert.assertEquals(2, o.getPosition());
	}

	@Test
	public void addEntityAtIndexAfterCurrentDoesNotShift() {
		Order o = orderOf(a, b, c);
		o.next();
		o.addEntity(3, d);
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(1, o.getPosition());
		Assert.assertEquals(d, o.getEntities().get(3));
	}

	// ---------- addSummon ----------

	@Test
	public void addSummonInsertsAfterOwner() {
		Order o = orderOf(a, b, c);
		o.addSummon(b, d);
		Assert.assertEquals(a, o.getEntities().get(0));
		Assert.assertEquals(b, o.getEntities().get(1));
		Assert.assertEquals(d, o.getEntities().get(2));
		Assert.assertEquals(c, o.getEntities().get(3));
	}

	@Test
	public void addSummonForUnknownOwnerIsNoOp() {
		Order o = orderOf(a);
		o.addSummon(b, d);
		Assert.assertEquals(1, o.getEntities().size());
	}

	@Test
	public void addSummonDoesNotShiftPositionWhenOwnerIsCurrent() {
		// Quirk: addSummon uses raw List.add(idx, ...), so position isn't bumped like
		// addEntity(int, Entity) does. Summon lands at idx+1 (after position),
		// leaving current() unchanged. Locks in the production behavior.
		Order o = orderOf(a, b, c);
		o.next();
		o.addSummon(b, d);
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(1, o.getPosition());
		Assert.assertEquals(d, o.getEntities().get(2));
	}

	// ---------- removeEntity ----------

	@Test
	public void removeEntityBeforePositionDecrementsPosition() {
		Order o = orderOf(a, b, c);
		o.next();
		o.next();
		o.removeEntity(a);
		Assert.assertEquals("Removing before current keeps current entity", c, o.current());
		Assert.assertEquals(1, o.getPosition());
	}

	@Test
	public void removeEntityAtPositionShiftsToNext() {
		Order o = orderOf(a, b, c);
		o.next();
		o.removeEntity(b);
		Assert.assertEquals(a, o.current());
		Assert.assertEquals(0, o.getPosition());
	}

	@Test
	public void removeEntityAfterPositionDoesNotMove() {
		Order o = orderOf(a, b, c);
		o.next();
		o.removeEntity(c);
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(1, o.getPosition());
	}

	@Test
	public void removeFirstEntityOnFirstTurnRollsTurnBack() {
		// Edge case: removing entity at pos=0 makes position -1; code resets to
		// size-1 and decrements turn so the caller's next() reaches turn 1 again.
		Order o = orderOf(a, b);
		Assert.assertEquals(a, o.current());
		o.removeEntity(a);
		Assert.assertEquals(b, o.current());
		Assert.assertEquals(0, o.getPosition());
		Assert.assertEquals(0, o.getTurn());
	}

	@Test
	public void removeUnknownEntityIsNoOp() {
		Order o = orderOf(a, b);
		o.removeEntity(c);
		Assert.assertEquals(2, o.getEntities().size());
		Assert.assertEquals(0, o.getPosition());
	}

	// ---------- Order copy constructor ----------

	@Test
	public void copyConstructorPreservesState() throws Exception {
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
		String aiCode = "global t = ''; t = t + getTurn() + ','; setRegister('turns', t);";
		attachAI(a, aiCode);
		attachAI(b, aiCode);
		attachAI(c, aiCode);
		attachAI(d, aiCode);
		fight.setMaxTurns(2);
		runFight();
		String aTurns = a.getRegister("turns");
		Assert.assertNotNull(aTurns);
		Assert.assertTrue("a should see turn 1: " + aTurns, aTurns.startsWith("1,"));
		Assert.assertTrue("a should see turn 2: " + aTurns, aTurns.contains("2,"));
	}

	@Test
	public void deathRemovesEntityFromOrder() throws Exception {
		initFightOnly();
		int sizeBefore = fight.getState().getOrder().getEntities().size();
		fight.getState().onPlayerDie(a, b, null);
		Assert.assertEquals(sizeBefore - 1, fight.getState().getOrder().getEntities().size());
		Assert.assertFalse("Dead entity removed from order",
			fight.getState().getOrder().getEntities().contains(a));
	}
}
