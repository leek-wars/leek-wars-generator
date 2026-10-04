package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.test.LocalTrophyManager;

/**
 * Ce que le générateur remonte aux trophées calculés en combat (TrophyManager du worker) :
 * les crochets doivent porter les bonnes entités et les bonnes valeurs au bon moment.
 */
public class TestTrophyHooks extends FightTestBase {

	private static final int CHIP_VAMPIRIZATION = FightConstants.CHIP_VAMPIRIZATION.getIntValue();

	private Leek leek1, leek2;
	private int maxTotalMP = 0;
	private final List<Entity[]> heals = new ArrayList<>();

	@Override
	protected void createLeeks() {
		leek1 = new Leek(1, "A", 0, 150, 1200, 20, 14, 300, 100, 1000, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "B", 0, 150, 1200, 20, 6, 300, 100, 1, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek1.addChip(Chips.getChip(CHIP_VAMPIRIZATION));
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
		fight.setStatisticsManager(new LocalTrophyManager() {
			@Override public void characteristics(Entity entity) {
				if (entity == leek1) maxTotalMP = Math.max(maxTotalMP, entity.getTotalMP());
			}
			@Override public void heal(Entity healer, Entity entity, int pv) {
				if (pv > 0) heals.add(new Entity[] { healer, entity });
			}
		});
	}

	/**
	 * Sprinteur (« Dépasser les 20 PM ») se vérifie dans characteristics() : chaque boost de PM
	 * doit le rappeler avec le total à jour (5pilow/leek-wars#2896 : 14 PM + 3 + 3 + 2).
	 */
	@Test
	public void mpBuffsReportTheUpdatedTotal() throws Exception {
		initFightOnly();
		applyEffect(Effect.TYPE_BUFF_MP, 3, 3, leek1, leek1, false);
		applyEffect(Effect.TYPE_RAW_BUFF_MP, 1, 3, leek1, leek1, true);
		applyEffect(Effect.TYPE_RAW_BUFF_MP, 2, 2, leek1, leek1, true);
		Assert.assertEquals(22, leek1.getTotalMP());
		Assert.assertEquals(22, maxTotalMP);
	}

	/**
	 * Traître (« Soigner un ennemi ») se juge sur heal(healer, entity) : le soin de Vampirisation
	 * revient au lanceur, il ne doit jamais apparaître comme un soin de l'ennemi visé
	 * (5pilow/leek-wars#1421).
	 */
	@Test
	public void vampirizationHealsTheCasterOnly() throws Exception {
		initFightOnly();
		Assert.assertEquals("le lanceur joue en premier (fréquence)", leek1, fight.getState().getOrder().current());
		Map map = fight.getState().getMap();
		map.clear();
		Cell next = map.getCell(leek1.getCell().getX() + 1, leek1.getCell().getY());
		if (next.getPlayer(map) != leek2) map.moveEntity(leek2, next);
		leek1.removeLife(500, 0, leek2, DamageType.DIRECT, null, null);

		int result = fight.useChip(leek1, leek2.getCell(), Chips.getChip(CHIP_VAMPIRIZATION));
		Assert.assertTrue("puce lancée : " + result, result > 0);

		Assert.assertFalse("la Vampirisation a soigné", heals.isEmpty());
		for (Entity[] heal : heals) {
			Assert.assertEquals("soin du lanceur sur lui-même", leek1, heal[0]);
			Assert.assertEquals(leek1, heal[1]);
		}
	}
}
