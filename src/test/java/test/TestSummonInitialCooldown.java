package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

/**
 * Cooldowns initiaux des invocations : ils se comptent depuis le début du combat,
 * pas depuis l'invocation. Un bulbe tacticien invoqué au tour 1 ne peut donc pas
 * lancer Inversion (cooldown initial 1) dans la foulée, alors qu'un bulbe invoqué
 * au tour 2 ou après le peut.
 */
public class TestSummonInitialCooldown extends FightTestBase {

	private static final int TACTICIAN_BULB = 11;
	private static final int INVERSION = 68;
	private static final int SHOCK = 6; // cooldown initial 0

	private Leek leek1, leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "A");
		leek2 = defaultLeek(2, "B");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private Cell freeCell(State state) {
		for (Cell cell : state.getMap().getCells()) {
			if (cell.available(state.getMap())) {
				return cell;
			}
		}
		throw new IllegalStateException("no free cell");
	}

	/** Avance l'ordre de jeu jusqu'au tour demandé, sans jouer les tours. */
	private void advanceToTurn(State state, int turn) {
		while (state.getOrder().getTurn() < turn) {
			state.getOrder().next();
		}
	}

	private Entity summonAtTurn(int turn) throws Exception {
		initFightOnly();
		State state = fight.getState();
		advanceToTurn(state, turn);
		return state.createSummon(leek1, TACTICIAN_BULB, freeCell(state), 1, false);
	}

	@Test
	public void summonedTurn1KeepsTheInitialCooldown() throws Exception {
		Entity bulb = summonAtTurn(1);
		Chip inversion = Chips.getChip(INVERSION);
		Assert.assertTrue(fight.getState().hasCooldown(bulb, inversion));
		// 2 = le bulbe joue dans le tour courant (décrémente une fois au début de son
		// tour) et doit rester bloqué pendant celui-ci : disponible au tour 2.
		Assert.assertEquals(2, fight.getState().getCooldown(bulb, inversion));
	}

	@Test
	public void summonedTurn2HasNoCooldown() throws Exception {
		Entity bulb = summonAtTurn(2);
		Assert.assertFalse(fight.getState().hasCooldown(bulb, Chips.getChip(INVERSION)));
	}

	@Test
	public void summonedTurn5HasNoCooldown() throws Exception {
		Entity bulb = summonAtTurn(5);
		Assert.assertFalse(fight.getState().hasCooldown(bulb, Chips.getChip(INVERSION)));
	}

	@Test
	public void chipsWithoutInitialCooldownAreUntouched() throws Exception {
		Entity bulb = summonAtTurn(1);
		Assert.assertFalse(fight.getState().hasCooldown(bulb, Chips.getChip(SHOCK)));
	}

	@Test
	public void entitiesPresentAtStartAreUnchanged() throws Exception {
		initFightOnly();
		// Régression : le calcul générique doit redonner exactement l'ancienne valeur
		// pour les entités présentes au lancement.
		Assert.assertEquals(2, fight.getState().getCooldown(leek1, Chips.getChip(INVERSION)));
	}
}
