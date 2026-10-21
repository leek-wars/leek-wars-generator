package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.State;
import com.leekwars.generator.turret.Turret;

/**
 * Un camp peut tuer sa propre tourelle et la ressusciter dans le même tour : la fin du
 * combat ne se teste qu'en fin de tour. La tourelle ressuscitée reste Statique (sa mort
 * avait purgé l'état) : ni poussée, ni attirée, ni échangée. Topic forum 10227.
 */
public class TestTurretResurrect extends FightTestBase {

	private Leek ally;
	private Turret turret;
	private State state;

	@Override
	protected void createLeeks() {
		fight.getState().setType(State.TYPE_TEAM);
		ally = defaultLeek(1, "A");
		turret = new Turret();
		turret.setTotalLife(1000);
		turret.setLife(1000);
		fight.getState().addEntity(0, ally);
		fight.getState().addEntity(0, turret);
		fight.getState().addEntity(1, defaultLeek(2, "B"));
	}

	@Before
	public void initFight() throws Exception {
		initFightOnly();
		state = fight.getState();
		turret.startFight();
		Assert.assertTrue(turret.hasState(EntityState.STATIC));
	}

	private Cell freeCell() {
		for (Cell cell : state.getMap().getCells()) {
			if (cell.available(state.getMap())) {
				return cell;
			}
		}
		throw new IllegalStateException("no free cell");
	}

	@Test
	public void resurrectedTurretStaysStatic() {
		turret.removeLife(turret.getLife(), 0, ally, DamageType.DIRECT, null, null);
		Assert.assertTrue(turret.isDead());
		Assert.assertTrue("tourelle morte, camp mort : seule la fin du tour clôt le combat", state.getTeams().get(0).isDead());

		Cell cell = freeCell();
		state.resurrect(ally, turret, cell, false, false);
		Assert.assertFalse(turret.isDead());
		Assert.assertFalse("ressuscitée avant la fin du tour, le camp est sauf", state.isFinished());
		Assert.assertTrue(turret.hasState(EntityState.STATIC));
	}
}
