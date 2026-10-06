package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.Order;
import com.leekwars.generator.state.State;

/**
 * Place dans l'ordre de jeu d'une entité ressuscitée. Un poireau reprend sa place de
 * départ ; une invocation, qui n'en a pas, revient juste après son invocateur s'il est en
 * vie (comme à l'invocation), et sinon en fin de tour. Topic forum 9992.
 */
public class TestResurrectOrder extends FightTestBase {

	private static final int PUNY_BULB = 1;

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

	private Cell freeCell(State state) {
		for (Cell cell : state.getMap().getCells()) {
			if (cell.available(state.getMap())) {
				return cell;
			}
		}
		throw new IllegalStateException("no free cell");
	}

	private void kill(Entity entity, Entity killer) {
		entity.removeLife(entity.getLife(), 0, killer, DamageType.DIRECT, null, null);
		Assert.assertTrue(entity.isDead());
	}

	private Entity enemyOf(Entity entity) {
		for (Entity e : fight.getState().getOrder().getEntities()) {
			if (e.getTeam() != entity.getTeam()) return e;
		}
		throw new IllegalStateException("no enemy");
	}

	/** Bulbe du premier à jouer, tué par un ennemi : sans le correctif, il repartirait en fin de tour. */
	private Entity deadBulbOf(Entity summoner) {
		State state = fight.getState();
		Entity bulb = state.createSummon(summoner, PUNY_BULB, freeCell(state), 1, false);
		kill(bulb, enemyOf(summoner));
		Assert.assertEquals(0, state.getOrder().getEntityTurnOrder(bulb));
		return bulb;
	}

	@Test
	public void resurrectedSummonPlaysRightAfterItsSummoner() throws Exception {
		initFightOnly();
		Order order = fight.getState().getOrder();
		Entity summoner = order.getEntities().get(0);
		Entity bulb = deadBulbOf(summoner);
		fight.getState().resurrect(enemyOf(summoner), bulb, freeCell(fight.getState()), false, false);
		Assert.assertEquals(2, order.getEntityTurnOrder(bulb));
		Assert.assertNotEquals(order.getEntities().size(), order.getEntityTurnOrder(bulb));
	}

	@Test
	public void resurrectedSummonOfADeadSummonerPlaysLast() throws Exception {
		initFightOnly();
		Order order = fight.getState().getOrder();
		Entity summoner = order.getEntities().get(0);
		Entity bulb = deadBulbOf(summoner);
		kill(summoner, enemyOf(summoner));
		fight.getState().resurrect(enemyOf(summoner), bulb, freeCell(fight.getState()), false, false);
		Assert.assertEquals(order.getEntities().size(), order.getEntityTurnOrder(bulb));
	}

	@Test
	public void resurrectedLeekGetsItsStartingPlaceBack() throws Exception {
		initFightOnly();
		State state = fight.getState();
		Order order = state.getOrder();
		Entity first = order.getEntities().get(0);
		Entity second = order.getEntities().get(1);
		Entity killer = first.getTeam() == second.getTeam() ? order.getEntities().get(2) : second;
		kill(first, killer);
		state.resurrect(second, first, freeCell(state), false, false);
		Assert.assertEquals(1, order.getEntityTurnOrder(first));
	}
}
