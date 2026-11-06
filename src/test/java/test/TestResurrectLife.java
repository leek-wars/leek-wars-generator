package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.classes.ChipClass;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.State;

/**
 * resurrect(entity, cell, chip?) : sans puce précisée, Résurrection passe avant Réveil,
 * et la vie rendue dépend de la puce réellement lancée (moitié avec Résurrection, toute
 * avec Réveil), pas des puces équipées.
 */
public class TestResurrectLife extends FightTestBase {

	private static final int CHIP_RESURRECTION = FightConstants.CHIP_RESURRECTION.getIntValue();
	private static final int CHIP_AWAKENING = FightConstants.CHIP_AWAKENING.getIntValue();
	private static final int CHIP_SPARK = FightConstants.CHIP_SPARK.getIntValue();

	private Leek both, waker, ally;
	private State state;

	@Override
	protected void createLeeks() {
		both = caster(1, "A");
		both.addChip(Chips.getChip(CHIP_RESURRECTION));
		both.addChip(Chips.getChip(CHIP_AWAKENING));
		both.addChip(Chips.getChip(CHIP_SPARK));
		waker = caster(2, "W");
		waker.addChip(Chips.getChip(CHIP_AWAKENING));
		ally = defaultLeek(3, "C");
		fight.getState().addEntity(0, both);
		fight.getState().addEntity(0, waker);
		fight.getState().addEntity(0, ally);
		fight.getState().addEntity(1, defaultLeek(4, "B"));
	}

	private Leek caster(int id, String name) {
		return new Leek(id, name, 0, 300, 1000, 50, 6, 100, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
	}

	@Before
	public void initFight() throws Exception {
		initFightOnly();
		state = fight.getState();
	}

	/** Tue l'allié puis le fait ressusciter par `caster`, avec la puce `chip` si elle est donnée. */
	private long resurrectAlly(Leek caster, Integer chip) throws Exception {
		ally.removeLife(ally.getLife(), 0, state.getEnemiesEntities(0).get(0), DamageType.DIRECT, null, null);
		Assert.assertTrue(ally.isDead());
		while (state.getOrder().current() != caster) {
			state.getOrder().next();
		}
		caster.setLogs(new LeekLog(farmerLog, caster));
		// Pas de code à exécuter : une IA vide suffit à porter l'appel de resurrect()
		var ai = EntityAI.build(generator, null, caster);
		ai.setFight(fight);
		// Une case à portée des deux puces de résurrection
		Chip resurrection = Chips.getChip(CHIP_RESURRECTION), awakening = Chips.getChip(CHIP_AWAKENING);
		for (Cell cell : state.getMap().getCells()) {
			if (cell.available(state.getMap())
				&& state.getMap().canUseAttack(caster.getCell(), cell, resurrection.getAttack())
				&& state.getMap().canUseAttack(caster.getCell(), cell, awakening.getAttack())) {
				return chip == null
					? ChipClass.resurrect(ai, ally.getFId(), cell.getId())
					: ChipClass.resurrect(ai, ally.getFId(), cell.getId(), chip);
			}
		}
		throw new IllegalStateException("no cell in range");
	}

	/** Moitié de la vie max, elle-même réduite de moitié par la résurrection. */
	private void assertHalfLife(int totalLife, long result) {
		Assert.assertTrue("resurrect() = " + result, result > 0);
		Assert.assertFalse(ally.isDead());
		Assert.assertTrue("vie max réduite", ally.getTotalLife() < totalLife);
		Assert.assertEquals(ally.getTotalLife() / 2, ally.getLife());
	}

	private void assertFullLife(int totalLife, long result) {
		Assert.assertTrue("resurrect() = " + result, result > 0);
		Assert.assertEquals(totalLife, ally.getTotalLife());
		Assert.assertEquals(totalLife, ally.getLife());
	}

	@Test
	public void resurrectionWinsOverAwakeningAndGivesHalfLife() throws Exception {
		assertHalfLife(ally.getTotalLife(), resurrectAlly(both, null));
	}

	@Test
	public void awakeningAloneGivesFullLife() throws Exception {
		assertFullLife(ally.getTotalLife(), resurrectAlly(waker, null));
	}

	@Test
	public void awakeningChosenExplicitlyGivesFullLife() throws Exception {
		assertFullLife(ally.getTotalLife(), resurrectAlly(both, CHIP_AWAKENING));
	}

	@Test
	public void resurrectionChosenExplicitlyGivesHalfLife() throws Exception {
		assertHalfLife(ally.getTotalLife(), resurrectAlly(both, CHIP_RESURRECTION));
	}

	@Test
	public void aChipThatIsNotEquippedFails() throws Exception {
		Assert.assertEquals(-1, resurrectAlly(waker, CHIP_RESURRECTION));
		Assert.assertTrue(ally.isDead());
	}

	@Test
	public void aChipWithoutResurrectEffectFails() throws Exception {
		Assert.assertTrue(resurrectAlly(both, CHIP_SPARK) < 0);
		Assert.assertTrue(ally.isDead());
	}
}
