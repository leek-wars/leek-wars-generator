package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

/**
 * Éveil des plantes (release/300/eveil_plantes_puces.md).
 *
 * Le Maïs et le Piment ne jouent pas de tour : ils se réveillent quand une entité entre
 * dans leur losange de rayon 3. Ces tests portent sur la mécanique elle-même — qui
 * réveille, quand, combien de fois — et sur ce que le réveil rend à la plante (PT pleins,
 * cooldowns d'un cran). L'IA n'est pas lancée : le callback d'exécution est remplacé par
 * un enregistreur, ce qui laisse voir les réveils un par un.
 */
public class TestPlantAwakening extends FightTestBase {

	private static final int CORN = 9;
	private static final int CHILLI_PEPPER = 10;
	private static final int PROTOTAXITE = 13;
	private static final int PUNY_BULB = 1;

	private static final int PIQUANT = 444;
	private static final int CAPSAICIN = 445;
	private static final int SUGAR = 446;
	private static final int POPCORN = 447;

	private Leek leek1, leek2;

	/** Réveils observés, dans l'ordre : « idPlante:idDéclencheur ». */
	private final List<String> awakenings = new ArrayList<>();

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "A");
		leek2 = defaultLeek(2, "B");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private State start() throws Exception {
		initFightOnly();
		State state = fight.getState();
		state.setPlantAwakening((plant, trigger) -> awakenings.add(plant.getFId() + ":" + trigger.getFId()));
		return state;
	}

	/** Première case libre à exactement `distance` cases de `from`. */
	private Cell freeCellAt(State state, Cell from, int distance) {
		for (Cell cell : state.getMap().getCells()) {
			if (cell != from && cell.available(state.getMap())
				&& Pathfinding.getCaseDistance(from, cell) == distance) {
				return cell;
			}
		}
		throw new IllegalStateException("aucune case libre à " + distance + " de " + from.getId());
	}

	private Cell freeCell(State state) {
		for (Cell cell : state.getMap().getCells()) {
			if (cell.available(state.getMap())) return cell;
		}
		throw new IllegalStateException("aucune case libre");
	}

	/** Plante posée sur une case libre, loin des poireaux. */
	private Entity plant(State state, int template) {
		Cell cell = freeCell(state);
		Entity plant = state.createSummon(leek1, template, cell, 100, false);
		Assert.assertNotNull(plant);
		return plant;
	}

	/** Pose une entité sur une case sans passer par les déclencheurs (mise en place). */
	private void put(State state, Entity entity, Cell cell) {
		state.getMap().setEntity(entity, cell);
	}

	private boolean hasChip(Entity entity, int chipId) {
		return entity.getChips().stream().anyMatch(c -> c.getId() == chipId);
	}

	// ----------------- Qui réveille, et quand -----------------

	@Test
	public void arriverDansLaZoneReveilleLaPlante() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Cell outside = freeCellAt(state, plant.getCell(), 5);
		Cell inside = freeCellAt(state, plant.getCell(), 3);

		put(state, leek2, outside);
		state.moveEntity(leek2, inside);

		Assert.assertEquals(List.of(plant.getFId() + ":" + leek2.getFId()), awakenings);
	}

	@Test
	public void arriverJusteHorsDeLaZoneNeReveillePas() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		put(state, leek2, freeCellAt(state, plant.getCell(), 6));
		state.moveEntity(leek2, freeCellAt(state, plant.getCell(), 4));

		Assert.assertEquals(List.of(), awakenings);
	}

	@Test
	public void traverserLaZoneSansSyArreterNeReveillePas() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Cell start = freeCellAt(state, plant.getCell(), 5);
		Cell end = freeCellAt(state, plant.getCell(), 4);
		put(state, leek2, start);

		// Seule la case d'arrivée compte : le moteur ne découpe pas le chemin. Départ et
		// arrivée hors zone, quel que soit le trajet entre les deux.
		state.checkPlantTriggers(leek2, start, end);

		Assert.assertEquals(List.of(), awakenings);
	}

	@Test
	public void resterDansLaZoneNeReveillePasUneSecondeFois() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		put(state, leek2, freeCellAt(state, plant.getCell(), 5));

		state.moveEntity(leek2, freeCellAt(state, plant.getCell(), 3));
		state.moveEntity(leek2, freeCellAt(state, plant.getCell(), 2));

		Assert.assertEquals(1, awakenings.size());
	}

	@Test
	public void sortirEtRentrerDansLeMemeTourNeVautQuUnReveil() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Cell outside = freeCellAt(state, plant.getCell(), 5);
		Cell inside = freeCellAt(state, plant.getCell(), 3);
		put(state, leek2, outside);

		state.moveEntity(leek2, inside);
		state.moveEntity(leek2, outside);
		state.moveEntity(leek2, inside);

		Assert.assertEquals(1, awakenings.size());
	}

	@Test
	public void auTourSuivantLEntiteReveilleDeNouveau() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Cell outside = freeCellAt(state, plant.getCell(), 5);
		Cell inside = freeCellAt(state, plant.getCell(), 3);
		put(state, leek2, outside);

		state.moveEntity(leek2, inside);
		state.moveEntity(leek2, outside);
		// Son tour recommence : le compteur est celui de l'entité, pas celui du combat.
		state.clearPlantTriggers(leek2);
		state.moveEntity(leek2, inside);

		Assert.assertEquals(2, awakenings.size());
	}

	@Test
	public void chaqueEntiteApporteSonPropreReveil() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Cell inside1 = freeCellAt(state, plant.getCell(), 3);
		Cell inside2 = freeCellAt(state, plant.getCell(), 2);
		put(state, leek1, freeCellAt(state, plant.getCell(), 5));
		put(state, leek2, freeCellAt(state, plant.getCell(), 6));

		state.moveEntity(leek1, inside1);
		state.moveEntity(leek2, inside2);

		Assert.assertEquals(2, awakenings.size());
	}

	@Test
	public void laPousseeReveille() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		put(state, leek2, freeCellAt(state, plant.getCell(), 5));

		state.slideEntity(leek2, freeCellAt(state, plant.getCell(), 3), leek1);

		Assert.assertEquals(1, awakenings.size());
	}

	@Test
	public void laTeleportationReveille() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		put(state, leek2, freeCellAt(state, plant.getCell(), 5));

		state.teleportEntity(leek2, freeCellAt(state, plant.getCell(), 3), leek1, 0);

		Assert.assertEquals(1, awakenings.size());
	}

	@Test
	public void linversionReveille() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		put(state, leek1, freeCellAt(state, plant.getCell(), 3));
		put(state, leek2, freeCellAt(state, plant.getCell(), 6));

		// leek2 prend la place de leek1, à l'intérieur de la zone : il entre.
		state.invertEntities(leek2, leek1);

		Assert.assertEquals(1, awakenings.size());
		Assert.assertEquals(plant.getFId() + ":" + leek2.getFId(), awakenings.get(0));
	}

	@Test
	public void uneInvocationQuiSortDeTerreDansLaZoneReveille() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Cell inside = freeCellAt(state, plant.getCell(), 2);

		Entity bulb = state.createSummon(leek2, PUNY_BULB, inside, 100, false);
		state.checkPlantTriggers(bulb, null, bulb.getCell());

		Assert.assertEquals(List.of(plant.getFId() + ":" + bulb.getFId()), awakenings);
	}

	@Test
	public void unePlanteEstReveilleeParCeQuiLEntourALaPlantation() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		put(state, leek2, freeCellAt(state, plant.getCell(), 2));
		put(state, leek1, freeCellAt(state, plant.getCell(), 8));

		state.checkPlantPlanted(plant);

		// Seul leek2 est dans la zone ; leek1 est trop loin.
		Assert.assertEquals(List.of(plant.getFId() + ":" + leek2.getFId()), awakenings);
	}

	@Test
	public void lePrototaxiteNeSeReveilleJamais() throws Exception {
		State state = start();
		Entity proto = plant(state, PROTOTAXITE);
		put(state, leek2, freeCellAt(state, proto.getCell(), 5));

		state.moveEntity(leek2, freeCellAt(state, proto.getCell(), 1));
		state.checkPlantPlanted(proto);

		Assert.assertFalse(proto.hasAwakening());
		Assert.assertEquals(List.of(), awakenings);
	}

	@Test
	public void unBulbeOrdinaireNestPasUnePlanteAZone() throws Exception {
		State state = start();
		Entity bulb = state.createSummon(leek1, PUNY_BULB, freeCell(state), 100, false);
		put(state, leek2, freeCellAt(state, bulb.getCell(), 5));

		state.moveEntity(leek2, freeCellAt(state, bulb.getCell(), 1));

		Assert.assertFalse(bulb.hasAwakening());
		Assert.assertEquals(List.of(), awakenings);
	}

	// ----------------- Ce que le réveil rend à la plante -----------------

	@Test
	public void leReveilRendLesPtEtBaisseLesCooldowns() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Chip capsaicin = Chips.getChip(CAPSAICIN);

		plant.useTP(plant.getTP());
		plant.addCooldown(capsaicin, 3);
		Assert.assertEquals(0, plant.getTP());

		Cell outside = freeCellAt(state, plant.getCell(), 5);
		put(state, leek2, outside);
		state.moveEntity(leek2, freeCellAt(state, plant.getCell(), 3));

		Assert.assertEquals(plant.getTotalTP(), plant.getTP());
		Assert.assertEquals(2, plant.getCooldown(capsaicin.getId()));
	}

	@Test
	public void laGrossePuceRevientUnReveilSurTrois() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Chip capsaicin = Chips.getChip(CAPSAICIN);
		Cell outside = freeCellAt(state, plant.getCell(), 5);
		Cell inside = freeCellAt(state, plant.getCell(), 3);
		put(state, leek2, outside);

		// Réveil 1 : la puce est libre, on la lance (cooldown 3 posé à la main, comme le
		// ferait useChip).
		state.moveEntity(leek2, inside);
		Assert.assertFalse(plant.hasCooldown(capsaicin.getId()));
		plant.addCooldown(capsaicin, capsaicin.getCooldown());

		// Réveils 2 et 3 : encore en recharge.
		for (int reveil = 2; reveil <= 3; reveil++) {
			state.moveEntity(leek2, outside);
			state.clearPlantTriggers(leek2);
			state.moveEntity(leek2, inside);
			Assert.assertTrue("réveil " + reveil, plant.hasCooldown(capsaicin.getId()));
		}

		// Réveil 4 : de nouveau disponible.
		state.moveEntity(leek2, outside);
		state.clearPlantTriggers(leek2);
		state.moveEntity(leek2, inside);
		Assert.assertFalse(plant.hasCooldown(capsaicin.getId()));
	}

	@Test
	public void lesCooldownsDUnePlanteNeBaissentPasAuTour() throws Exception {
		State state = start();
		Entity plant = plant(state, CHILLI_PEPPER);
		Chip capsaicin = Chips.getChip(CAPSAICIN);
		plant.addCooldown(capsaicin, 3);

		// Le tour de la plante ne lui rend rien : son temps se compte en réveils.
		plant.startTurn();

		Assert.assertEquals(3, plant.getCooldown(capsaicin.getId()));
	}

	@Test
	public void lesCooldownsDUnBulbeBaissentToujoursAuTour() throws Exception {
		State state = start();
		Entity bulb = state.createSummon(leek1, PUNY_BULB, freeCell(state), 100, false);
		Chip bandage = Chips.getChip(3);
		bulb.addCooldown(bandage, 3);

		bulb.startTurn();

		Assert.assertEquals(2, bulb.getCooldown(bandage.getId()));
	}

	// ----------------- Les 4 puces -----------------

	@Test
	public void lesQuatrePucesSontChargeesEtDediees() {
		Chip piquant = Chips.getChip(PIQUANT);
		Chip capsaicin = Chips.getChip(CAPSAICIN);
		Chip sugar = Chips.getChip(SUGAR);
		Chip popcorn = Chips.getChip(POPCORN);

		for (Chip chip : new Chip[] { piquant, capsaicin, sugar, popcorn }) {
			Assert.assertNotNull(chip);
			// Une tourelle voit sa zone : pas de ligne de vue à respecter.
			Assert.assertFalse(chip.getName(), chip.getAttack().needLos());
		}

		// Les petites : 3 PT, portée 1 à 3 (la zone), pas de cooldown.
		for (Chip chip : new Chip[] { piquant, sugar }) {
			Assert.assertEquals(chip.getName(), 3, chip.getCost());
			Assert.assertEquals(chip.getName(), 1, chip.getAttack().getMinRange());
			Assert.assertEquals(chip.getName(), 3, chip.getAttack().getMaxRange());
			Assert.assertEquals(chip.getName(), 0, chip.getCooldown());
		}

		// Les grosses : 6 PT, lancées sur soi, zone CIRCLE3, cooldown 3.
		for (Chip chip : new Chip[] { capsaicin, popcorn }) {
			Assert.assertEquals(chip.getName(), 6, chip.getCost());
			Assert.assertEquals(chip.getName(), 0, chip.getAttack().getMaxRange());
			Assert.assertEquals(chip.getName(), 5, chip.getAttack().getArea());
			Assert.assertEquals(chip.getName(), 3, chip.getCooldown());
			// Multipliées par le nombre de cibles, comme le Plasma.
			Assert.assertEquals(chip.getName(), 2, chip.getAttack().getEffects().get(0).getModifiers());
		}
	}

	@Test
	public void lesPlantesPortentLeursPucesEtLeurZone() throws Exception {
		State state = start();
		Entity corn = plant(state, CORN);
		Entity chilli = plant(state, CHILLI_PEPPER);

		Assert.assertTrue(corn.hasAwakening());
		Assert.assertTrue(chilli.hasAwakening());
		Assert.assertEquals(3, corn.getAwakeningZone());
		Assert.assertEquals(3, chilli.getAwakeningZone());

		Assert.assertTrue(hasChip(corn, SUGAR));
		Assert.assertTrue(hasChip(corn, POPCORN));
		Assert.assertTrue(hasChip(chilli, PIQUANT));
		Assert.assertTrue(hasChip(chilli, CAPSAICIN));
		// Les puces de bulbe ordinaire ont laissé la place.
		Assert.assertFalse(hasChip(corn, 3));
		Assert.assertFalse(hasChip(chilli, 5));
	}

	@Test
	public void laCapsaicineNeBruleQueLesEnnemisEtLePopcornNeSoigneQueLesAllies() {
		// Les cibles croisent le CAMP et la NATURE. Sans bit de nature (8 + 16) le moteur
		// rejette tout ; avec le camp en trop, la plante retourne sa puce contre son camp.
		final int ENEMIES = 1, ALLIES = 2, CASTER = 4, NON_SUMMONS = 8, SUMMONS = 16;

		for (var effect : Chips.getChip(CAPSAICIN).getAttack().getEffects()) {
			int targets = effect.getTargets();
			Assert.assertTrue("Capsaïcine touche les ennemis", (targets & ENEMIES) != 0);
			Assert.assertEquals("Capsaïcine épargne les alliés", 0, targets & ALLIES);
			Assert.assertEquals("le Piment ne se brûle pas", 0, targets & CASTER);
			Assert.assertEquals("les deux natures", NON_SUMMONS | SUMMONS, targets & (NON_SUMMONS | SUMMONS));
		}

		for (var effect : Chips.getChip(POPCORN).getAttack().getEffects()) {
			int targets = effect.getTargets();
			Assert.assertTrue("Pop-corn soigne les alliés", (targets & ALLIES) != 0);
			Assert.assertEquals("Pop-corn ne soigne pas l'ennemi", 0, targets & ENEMIES);
			Assert.assertEquals("le Maïs ne se soigne pas", 0, targets & CASTER);
			Assert.assertEquals("les deux natures", NON_SUMMONS | SUMMONS, targets & (NON_SUMMONS | SUMMONS));
		}
	}

	@Test
	public void lePimentAUneMagiePourSaSequelle() throws Exception {
		State state = start();
		Entity chilli = plant(state, CHILLI_PEPPER);
		// La séquelle de Capsaïcine est un poison : sans magie elle ne vaudrait rien.
		Assert.assertTrue(chilli.getMagic() > 0);
	}
}
