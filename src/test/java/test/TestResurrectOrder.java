package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.attack.DamageType;
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

	private State state;
	private Order order;

	@Override
	protected void createLeeks() {
		fight.getState().addEntity(0, defaultLeek(1, "A"));
		fight.getState().addEntity(1, defaultLeek(2, "B"));
		fight.getState().addEntity(0, defaultLeek(3, "C"));
		fight.getState().addEntity(1, defaultLeek(4, "D"));
	}

	@Before
	public void initFight() throws Exception {
		initFightOnly();
		state = fight.getState();
		order = state.getOrder();
	}

	private Cell freeCell() {
		for (Cell cell : state.getMap().getCells()) {
			if (cell.available(state.getMap())) {
				return cell;
			}
		}
		throw new IllegalStateException("no free cell");
	}

	private Entity enemyOf(Entity entity) {
		return state.getEnemiesEntities(entity.getTeam()).get(0);
	}

	private void kill(Entity entity) {
		entity.removeLife(entity.getLife(), 0, enemyOf(entity), DamageType.DIRECT, null, null);
		Assert.assertTrue(entity.isDead());
	}

	/** Bulbe du premier à jouer, tué : sans le correctif, ressuscité, il repartirait en fin de tour. */
	private Entity deadBulbOf(Entity summoner) {
		Entity bulb = state.createSummon(summoner, FightConstants.BULB_PUNY.getIntValue(), freeCell(), 1, false);
		kill(bulb);
		Assert.assertEquals(0, order.getEntityTurnOrder(bulb));
		return bulb;
	}

	@Test
	public void resurrectedSummonPlaysRightAfterItsSummoner() {
		Entity summoner = order.getEntities().get(0);
		Entity bulb = deadBulbOf(summoner);
		state.resurrect(enemyOf(summoner), bulb, freeCell(), false, false);
		Assert.assertEquals(2, order.getEntityTurnOrder(bulb));
	}

	@Test
	public void resurrectedSummonOfADeadSummonerPlaysLast() {
		Entity summoner = order.getEntities().get(0);
		Entity bulb = deadBulbOf(summoner);
		kill(summoner);
		state.resurrect(enemyOf(summoner), bulb, freeCell(), false, false);
		Assert.assertEquals(order.getEntities().size(), order.getEntityTurnOrder(bulb));
	}

	@Test
	public void resurrectedLeekGetsItsStartingPlaceBack() {
		Entity first = order.getEntities().get(0);
		kill(first);
		state.resurrect(enemyOf(first), first, freeCell(), false, false);
		Assert.assertEquals(1, order.getEntityTurnOrder(first));
	}
}
