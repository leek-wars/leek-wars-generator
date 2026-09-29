package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.action.Action;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.Entity;

/** Durée des effets périodiques, comptée en coups chez leur cible : cf. PeriodicEffect. */
public class TestPeriodicEffects extends FightTestBase {

	private static final int CHILLI_PEPPER = FightConstants.CHIP_CHILLI_PEPPER.getIntValue();
	private static final int CAPSAICIN = FightConstants.CHIP_CAPSAICIN.getIntValue();
	private static final int ARSENIC = FightConstants.CHIP_ARSENIC.getIntValue();

	private Leek leek1, leek2;

	@Override
	protected void createLeeks() {
		// Niveau 300 : le Piment a alors ses 7 PT, de quoi lancer la Capsaïcine (6 PT).
		// Magie à 0 : un poison de valeur v retire exactement v de vie par coup.
		leek1 = new Leek(1, "A", 0, 300, 3000, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "B", 0, 300, 3000, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek1.addChip(Chips.getChip(CHILLI_PEPPER));
		leek1.addChip(Chips.getChip(ARSENIC));
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private static int poisonTurns(int chip) {
		return Chips.getChip(chip).getAttack().getEffectParametersByType(Effect.TYPE_POISON).getTurns();
	}

	/** Poisons posés sur `target` par une puce ou une arme. */
	private int poisonsOn(Entity target) {
		int count = 0;
		for (var action : fight.getState().getActions().toJSON().get("actions")) {
			int type = action.get(0).asInt();
			// [type, item, id, lanceur, cible, effet, valeur, tours, …]
			if ((type == Action.ADD_CHIP_EFFECT || type == Action.ADD_WEAPON_EFFECT)
					&& action.get(4).asInt() == target.getFId() && action.get(5).asInt() == Effect.TYPE_POISON) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Le cas qui a tout déclenché : la Capsaïcine d'un Piment est lancée pendant le tour de
	 * celui qui entre dans sa zone. Décomptée au tour du Piment, qui passe avant le tour
	 * suivant de sa cible, elle perdait un tour avant d'avoir frappé : un coup sur deux.
	 */
	@Test
	public void laCapsaicineFrappeAutantDeFoisQueSaDuree() throws Exception {
		attachAI(leek1, summonChilliNextToMe("", "if (isEnemy(e)) { useChip(CHIP_CAPSAICIN, getEntity()); }"));
		// B marche sur A : il entre dans la zone du Piment pendant son propre tour, et y reste.
		attachAI(leek2, "moveToward(getNearestEnemy());");
		runFight();

		Assert.assertEquals("une seule Capsaïcine, au réveil du Piment", 1, poisonsOn(leek2));
		Assert.assertEquals("autant de coups que de tours", poisonTurns(CAPSAICIN), countActions(Action.POISON_DAMAGE, leek2));
	}

	/** Lancé au tour de son lanceur, un poison frappe autant de fois qu'avant. */
	@Test
	public void unPoisonLanceASonTourFrappeAutantDeFoisQueSaDuree() throws Exception {
		attachAI(leek1, ""
			+ "global done = false;"
			+ "var enemy = getNearestEnemy();"
			+ "if (!done) {"
			+ "  var c = getCellToUseChip(CHIP_ARSENIC, enemy);"
			+ "  if (c != null) moveTowardCell(c);"
			+ "  if (useChip(CHIP_ARSENIC, enemy) == USE_SUCCESS) done = true;"
			+ "}");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals(1, poisonsOn(leek2));
		Assert.assertEquals(poisonTurns(ARSENIC), countActions(Action.POISON_DAMAGE, leek2));
	}

	/**
	 * Le mécanisme, sans IA : le tour du lanceur ne touche plus au poison, chaque coup subi
	 * par la cible lui retire un tour, et le dernier coup le retire aussitôt.
	 */
	@Test
	public void unPoisonSeDecompteAuTourDeSaCiblePasDeSonLanceur() throws Exception {
		initFightOnly();
		applyEffect(Effect.TYPE_POISON, 2, 30, leek2, leek1, false);
		var poison = leek2.getEffects().get(0);
		int life = leek2.getLife();

		// Le tour du lanceur passe avant le premier coup, comme pour un réveil de plante.
		leek1.startTurn();
		Assert.assertEquals("le tour du lanceur ne décompte pas un poison", 2, poison.getTurns());

		leek2.startTurn();
		Assert.assertEquals(life - 30, leek2.getLife());
		Assert.assertEquals(1, poison.getTurns());

		leek1.startTurn();
		Assert.assertEquals(1, poison.getTurns());

		leek2.startTurn();
		Assert.assertEquals("deux tours, deux coups", life - 60, leek2.getLife());
		Assert.assertTrue("retiré dès son dernier coup", leek2.getEffects().isEmpty());
		Assert.assertTrue("et de la liste du lanceur", leek1.getLaunchedEffects().isEmpty());
	}

	@Test
	public void unPoisonSurSoiFrappeAutantDeFoisQueSaDuree() throws Exception {
		initFightOnly();
		applyEffect(Effect.TYPE_POISON, 2, 30, leek1, leek1, false);
		int life = leek1.getLife();

		leek1.startTurn();
		leek1.startTurn();

		Assert.assertEquals(life - 60, leek1.getLife());
		Assert.assertTrue(leek1.getEffects().isEmpty());
		Assert.assertTrue(leek1.getLaunchedEffects().isEmpty());
	}

	@Test
	public void laSequelleEtLeSoinSurLaDureeSeDecomptentAussiAuCoup() throws Exception {
		initFightOnly();
		applyEffect(Effect.TYPE_AFTEREFFECT, 2, 30, leek2, leek1, false);
		applyEffect(Effect.TYPE_HEAL, 2, 30, leek2, leek1, false);
		Assert.assertEquals(2, leek2.getEffects().size());

		leek1.startTurn();
		for (var effect : leek2.getEffects()) {
			Assert.assertEquals("effet " + effect.getID() + " : le tour du lanceur ne le décompte pas", 2, effect.getTurns());
		}
		leek2.startTurn();
		for (var effect : leek2.getEffects()) {
			Assert.assertEquals("effet " + effect.getID(), 1, effect.getTurns());
		}
		leek2.startTurn();
		Assert.assertTrue(leek2.getEffects().isEmpty());
		Assert.assertTrue(leek1.getLaunchedEffects().isEmpty());
	}

	@Test
	public void unBuffSeDecompteToujoursAuTourDeSonLanceur() throws Exception {
		initFightOnly();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 1, 50, leek2, leek1, false);
		Assert.assertEquals(1, leek2.getEffects().size());

		leek2.startTurn();
		Assert.assertEquals("le tour de la cible ne décompte pas un buff", 1, leek2.getEffects().size());

		leek1.startTurn();
		Assert.assertTrue(leek2.getEffects().isEmpty());
	}

	/** Le lecteur du client s'en sert pour rejouer les anciens combats avec l'ancienne règle. */
	@Test
	public void leCombatAnnonceLaVersionDeSesRegles() throws Exception {
		initFightOnly();
		var version = fight.getState().getActions().toJSON().get("version");
		Assert.assertNotNull(version);
		Assert.assertTrue(version.asInt() >= 1);
	}
}
