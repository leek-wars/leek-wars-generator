package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.weapons.Weapons;

/**
 * Régression du combat prod 53252315 (action 757 = BUG) : la lance du soleil (item 440)
 * applique ses dégâts PUIS sa repoussée (EFFECT_REPEL). Si les dégâts tuent la cible,
 * elle n'a plus de cellule — la repoussée faisait NPE sur getCell() et le moteur
 * loggait une action BUG. La cible repoussée vivante, elle, doit continuer de reculer.
 */
public class TestRepelKill extends FightTestBase {

	private Leek caster;
	private Leek victim;

	@Override
	protected void createLeeks() {
		// Force 100 : dégâts de lance 150-180. La victime à 100 PV meurt, celle à 5000 survit.
		caster = new Leek(1, "Piqueur", 1, 150, 2500, 18, 6, 100, 0, 0, 0, 0, 0, 0, 8, 64,
			0, false, 0, 0, "", 0, "", "", "", 0);
		victim = new Leek(2, "Cible", 1, 150, 100, 18, 6, 0, 0, 0, 0, 0, 0, 0, 8, 64,
			0, false, 0, 0, "", 0, "", "", "", 0);
		fight.getState().addEntity(0, caster);
		fight.getState().addEntity(1, victim);
	}

	/** Map fixe sans obstacle, spawns alignés en diagonale à 2 cases (342 = 306 + 2×18). */
	private void setupAlignedMap() throws Exception {
		var map = Json.createObject();
		map.put("id", 1);
		map.set("obstacles", Json.createObject());
		map.set("pattern", Json.createArray());
		var team1 = Json.createArray(); team1.add(306);
		var team2 = Json.createArray(); team2.add(342);
		map.set("team1", team1);
		map.set("team2", team2);
		fight.getState().setCustomMap(map);
		fight.getState().seed(42);
		initFightOnly();
	}

	@Test
	public void repelSurCibleTueeNeCrashePas() throws Exception {
		var spear = Weapons.getWeapon(440);
		Assert.assertNotNull("lance du soleil absente du catalogue", spear);
		setupAlignedMap();
		caster.setWeapon(spear);

		// Dégâts (150-180) > 100 PV : la cible meurt, puis l'effet 53 tente la repoussée.
		var target = fight.getState().getMap().getCell(342);
		spear.getAttack().applyOnCell(fight.getState(), caster, target, false);

		Assert.assertTrue("la cible doit être morte", victim.isDead());
	}

	@Test
	public void repelSurCibleVivanteReculeDe4() throws Exception {
		var spear = Weapons.getWeapon(440);
		Assert.assertNotNull("lance du soleil absente du catalogue", spear);
		setupAlignedMap();
		caster.setWeapon(spear);
		victim.setLife(5000);
		victim.setTotalLife(5000);

		var target = fight.getState().getMap().getCell(342);
		spear.getAttack().applyOnCell(fight.getState(), caster, target, false);

		Assert.assertFalse("la cible doit survivre", victim.isDead());
		// Repoussée de 4 cases le long de la diagonale : 342 + 4×18 = 414.
		Assert.assertNotNull(victim.getCell());
		Assert.assertEquals("cellule après repoussée", 414, victim.getCell().getId());
	}

	/** Le critique porte aussi sur la distance de repoussée : round(4 × 1,3) = 5 cases. */
	@Test
	public void repelCritiqueReculeDe5() throws Exception {
		var spear = Weapons.getWeapon(440);
		Assert.assertNotNull("lance du soleil absente du catalogue", spear);
		setupAlignedMap();
		caster.setWeapon(spear);
		victim.setLife(5000);
		victim.setTotalLife(5000);

		var target = fight.getState().getMap().getCell(342);
		spear.getAttack().applyOnCell(fight.getState(), caster, target, true);

		Assert.assertFalse("la cible doit survivre", victim.isDead());
		// 342 + 5×18 = 432.
		Assert.assertNotNull(victim.getCell());
		Assert.assertEquals("cellule après repoussée critique", 432, victim.getCell().getId());
	}
}
