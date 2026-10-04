package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

/**
 * Fin d'une chasse aux coffres : quand tous les coffres sont ouverts, mais aussi quand
 * plus aucun poireau n'est en vie pour les ouvrir (5pilow/leek-wars#4598) — le combat
 * tournait sinon jusqu'au tour 64 avec les seuls coffres.
 */
public class TestChestHuntEnd extends FightTestBase {

	private Leek leek1;
	private Entity chest;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		chest = new Entity() {
			@Override public int getType() { return Entity.TYPE_CHEST; }
		};
		chest.setId(-100);
		chest.setName("chest");
		chest.setLevel(100);
		chest.setTotalLife(1000);
		chest.setLife(1000);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, chest);
	}

	private State chestHunt() throws Exception {
		initFightOnly();
		State state = fight.getState();
		state.setType(State.TYPE_CHEST_HUNT);
		return state;
	}

	@Test
	public void notFinishedWhilePlayersAndChestsAreAlive() throws Exception {
		Assert.assertFalse(chestHunt().isFinished());
	}

	@Test
	public void finishedWhenAllChestsAreOpened() throws Exception {
		State state = chestHunt();
		chest.removeLife(chest.getLife(), 0, leek1, DamageType.DIRECT, null, null);
		Assert.assertTrue(state.isFinished());
	}

	@Test
	public void finishedWhenNoPlayerIsLeft() throws Exception {
		State state = chestHunt();
		leek1.removeLife(leek1.getLife(), 0, chest, DamageType.RETURN, null, null);
		Assert.assertTrue(state.isFinished());
		// Les coffres restent fermés : pas de vainqueur, comme au bout des 64 tours
		fight.computeWinner(false);
		Assert.assertEquals(-1, fight.getWinner());
	}
}
