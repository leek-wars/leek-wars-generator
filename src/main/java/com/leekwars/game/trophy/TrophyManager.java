package com.leekwars.game.trophy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.leekwars.game.Util;
import com.leekwars.game.attack.Attack;
import com.leekwars.game.attack.chips.ChipTemplate;
import com.leekwars.game.attack.effect.Effect;
import com.leekwars.game.attack.weapons.Weapon;
import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionLama;
import com.leekwars.game.fight.entity.Entity;
import com.leekwars.game.leek.Leek;
import com.leekwars.game.maps.Cell;

public class TrophyManager {

	private final Map<Integer, TrophyVariables> mFarmers;
	private final Fight mFight;

	private final List<Integer> mFarmers1 = new ArrayList<Integer>();
	private final List<Integer> mFarmers2 = new ArrayList<Integer>();

	public static void winTrophy(int farmer, int trophy) {}

	public TrophyManager(Fight f) {
		mFarmers = new TreeMap<Integer, TrophyVariables>();
		mFight = f;
	}

	public void addFarmer(TrophyVariables vars) {
		mFarmers.put(vars.getFarmer(), vars);
	}

	public void startFight() {
		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE)
			return;
		for (Entity l : mFight.getEntities().values()) {
			if (l.getFarmer() <= 0)
				continue;
			if (l.getTeam() == 0) {
				if (!mFarmers1.contains(l.getFarmer()))
					mFarmers1.add(l.getFarmer());
			} else if (!mFarmers2.contains(l.getFarmer()))
				mFarmers2.add(l.getFarmer());
			checkStats(l);
		}
	}

	public boolean hasFarmer(int farmer) {
		return mFarmers.containsKey(farmer);
	}

	public void deplacement(int farmer, List<Cell> path) {

		mFight.statistics.addDistance(path.size());

		if (Fight.isTestFight(mFight.getFullType()) || mFight.getContext() == Fight.CONTEXT_CHALLENGE) {
			return;
		}
		TrophyVariables tv = mFarmers.get(farmer);
		if (tv == null) {
			return;
		}
		tv.addCellMovement(path.size());
		tv.addFightCellMovement(path.size());
		
		for (Cell c : path) {

			int id = c.getId();

			// Explorator trophy
			tv.walked_cells[id] = true;

			// Cardinal cell
			if (!tv.hasTrophee(Trophy.CARDINAL)) {
				if (id == 0 || id == 17 || id == 595 || id == 612) {
					if (!tv.cardinal_cells.contains(id)) {
						tv.cardinal_cells.add(id);
						if (tv.cardinal_cells.size() == 4) {
							tv.winTrophy(Trophy.CARDINAL);
						}
					}
				}
			}
			// Prime cell
			if (!tv.hasTrophee(Trophy.MATHEMATICIAN)) {
				if (Util.isPrime(id)) {
					tv.addPrimeCell(id);
					if (tv.getPrimeCell() >= 50) {
						tv.winTrophy(Trophy.MATHEMATICIAN);
					}
				}
			}
		}
	}

	private void checkStats(Entity l) {
		if (l.getFarmer() < 0)
			return;
		TrophyVariables tv = mFarmers.get(l.getFarmer());
		// Vérification pour le nombre de PM/PT/PV max
		if (l.getLife() >= 1000)
			tv.winTrophy(Trophy.CARAPACE);
		if (l.getLife() >= 2000)
			tv.winTrophy(Trophy.MASTODON);
		if (l.getLife() >= 3000)
			tv.winTrophy(Trophy.TANK);
		if (l.getStat(Entity.CHARAC_MP) >= 12)
			tv.winTrophy(Trophy.RUNNER);
		if (l.getStat(Entity.CHARAC_MP) >= 20)
			tv.winTrophy(Trophy.SPRINTER);
		if (l.getStat(Entity.CHARAC_TP) >= 12)
			tv.winTrophy(Trophy.BOOSTED);
		if (l.getStat(Entity.CHARAC_TP) >= 20)
			tv.winTrophy(Trophy.BUFFED);
	}

	private void attackUsed(Entity caster, List<Entity> targets, Attack attack) {

		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE) {
			return;
		}

		if (caster instanceof Leek) {

			TrophyVariables tv = mFarmers.get(caster.getFarmer());

			// Le mec s'est suicidé avec son attaque ?
			if (caster.isDead()) {
				tv.setSuicide(true);
			}

			int hurt_enemies = 0;
			int healed_enemies = 0;
			int killed_allies = 0;
			int killed_enemies = 0;

			for (Entity target : targets) {

				if (target.getTeam() != caster.getTeam()) { // Ennemi

					if (target.isDead()) {
						killed_enemies++;
					}
					if (attack.isDamageAttack(Effect.TARGET_ENEMIES)) {
						hurt_enemies++;
					}
					if (attack.isHealAttack(Effect.TARGET_ENEMIES) && !attack.isDamageAttack(Effect.TARGET_ENEMIES)) {
						healed_enemies++;
					}
				} else if (target.getId() != caster.getId()) { // Allié

					if (target.isDead()) {
						killed_allies++;
					}
				}
			}
			// Cibles tuées
			tv.addKill(killed_allies + killed_enemies);

			// Kamikaze ?
			if (caster.isDead() && killed_enemies > 0) {
				tv.winTrophy(Trophy.KAMIKAZE);
			}
			// Tuer un allié
			if (killed_allies > 0) {
				tv.addKilledAllies(killed_allies);
			}
			// Soigner un ennemi
			if (healed_enemies > 0)
				tv.winTrophy(Trophy.TRAITOR);

			// Toucher plusieurs ennemis
			if (hurt_enemies >= 2)
				tv.winTrophy(Trophy.IMPALER);
			if (hurt_enemies >= 3)
				tv.winTrophy(Trophy.SKEWERING);
			if (hurt_enemies >= 4)
				tv.winTrophy(Trophy.STRATEGIST);
			if (hurt_enemies >= 5)
				tv.winTrophy(Trophy.BUTCHER);

			// Tuer plusieurs ennemis
			if (killed_enemies >= 2)
				tv.winTrophy(Trophy.DESTROYER);
		}

	}

	public void chipUsed(Entity caster, ChipTemplate chip, List<Entity> targets) {

		mFight.statistics.addUsedChips(1);

		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE)
			return;

		if (caster instanceof Leek) {

			// On vérifie les stats
			checkStats(caster);
			for (Entity l : targets)
				checkStats(l);
			if (caster.getFarmer() < 0)
				return;
			TrophyVariables tv = mFarmers.get(caster.getFarmer());

			tv.addUsedChips(1);

			tv.addUsedSpell(caster.getId(), chip.getId());
			tv.addSpellLaunch(1);
			attackUsed(caster, targets, chip.getAttack());
		}
	}

	public void weaponUsed(Entity caster, Weapon weapon, List<Entity> targets) {

		mFight.statistics.addBullets(1);
		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE)
			return;
		if (caster instanceof Leek) {
			// On vérifie les stats
			checkStats(caster);
			for (Entity l : targets)
				checkStats(l);
			if (caster.getFarmer() < 0)
				return;
			TrophyVariables tv = mFarmers.get(caster.getFarmer());
			tv.addUsedWeapon(caster.getId(), weapon.getId());
			tv.addWeaponShot(1);

			attackUsed(caster, targets, weapon.getAttack());
		}
	}

	public void endFight(int winteam) {

		int life = 0;
		int total_life = 0;
		List<Integer> farmers = new ArrayList<Integer>();

		// Pas les combats de test
		if (!Fight.isTestFight(mFight.getFullType())) {

			if (mFight.getFullType() == Fight.TYPE_SOLO_CHALLENGE) {
				List<Entity> l1 = mFight.getTeamEntities(0, true);
				List<Entity> l2 = mFight.getTeamEntities(1, true);
				for (Entity l : l1) {
					if (l.getFarmer() < 0)
						return;
					boolean d = false;
					for (Entity ll : l2) {
						if (ll.getId() == l.getId()) {
							d = true;
							break;
						}
					}
					if (d) {
						TrophyVariables tv = mFarmers.get(l.getFarmer());
						tv.winTrophy(Trophy.SCHIZOPHRENIC);
					}
				}
			}

			// Tous combats saufs test et challenge
			if (mFight.getFightContext() != Fight.CONTEXT_CHALLENGE) {

				if (winteam >= 0) {
					for (Entity l : mFight.getTeamEntities(winteam, true)) {
						life += l.getLife();
						total_life += l.getTotalLife();
						if (l.getFarmer() <= 0)
							continue;
						if (l.getOwnerId() != -1)
							continue;
						if (!farmers.contains(l.getFarmer()))
							farmers.add(l.getFarmer());

						// On regarde s'il a le trophé Chanceux
						if (l.getLife() == 1)
							mFarmers.get(l.getFarmer()).winTrophy(Trophy.FORTUNATE);

						// On regarde si le mec a bougé
						if (!l.hasMoved() && mFight.getFightType() == Fight.TYPE_SOLO) {
							mFight.addFlag(l.getTeam(), Fight.FLAG_STATIC);
							mFarmers.get(l.getFarmer()).winTrophy(Trophy.STATIC);
						}
						// On regarde si le mec est sur la case 42
						if (l.getCell() != null && l.getCell().getId() == 42)
							mFarmers.get(l.getFarmer()).winTrophy(Trophy.GEEK);

						if (mFight.getFightType() == Fight.TYPE_SOLO) {
							// On regarde si le mec avait qu'une seule
							// instruction
							if (l.getUsedLeekIA() != null && l.getUsedLeekIA().getInstructions() == 1)
								mFarmers.get(l.getFarmer()).winTrophy(Trophy.BRIEF);
						}
					}
				}

				for (Integer id : farmers) {

					TrophyVariables tv = mFarmers.get(id);

					// Perfect
					if (life >= total_life) {
						mFight.addFlag(winteam, Fight.FLAG_PERFECT);
						tv.addPerfect(1);
					}
				}

				// Trophée déserteur
				if (winteam >= 0 && mFight.getType() == Fight.TYPE_SOLO) {
					int looseteam = (winteam + 1) % 2;
					for (Entity l : mFight.getTeamEntities(looseteam, true)) {
						if (l.getFarmer() < 0) {
							continue;
						}
						if (mFarmers.get(l.getFarmer()).getSuicide()) {
							mFarmers.get(l.getFarmer()).winTrophy(Trophy.DESERTER);
						}
					}
				}

				// Trophée EXPLORER
				if (mFight.getFightType() == Fight.TYPE_SOLO) {

					for (Integer id : farmers) {
						TrophyVariables tv = mFarmers.get(id);
						if (!tv.hasTrophee(Trophy.EXPLORER)) {
							boolean all = true;
							for (int i = 0; i < 613; ++i) {
								if (mFight.getMap().getCell(i).isWalkable() && tv.walked_cells[i] == false) {
									all = false;
									break;
								}
							}
							if (all)
								tv.winTrophy(Trophy.EXPLORER);
						}
					}
				}
			}
		}
	}

	public void tooMuchInstructions(Entity l) {
		if (l.getFarmer() < 0)
			return;
		TrophyVariables tv = mFarmers.get(l.getFarmer());
		tv.winTrophy(Trophy.INFINITY);
	}

	public void stackOverflow(Entity l) {
		if (l.getFarmer() < 0)
			return;
		TrophyVariables tv = mFarmers.get(l.getFarmer());
		tv.winTrophy(Trophy.STACKOVERFLOW);
	}

	public void lama(Entity leek) {
		if (leek.getFarmer() < 0)
			return;
		mFight.log(new ActionLama(leek));
		mFarmers.get(leek.getFarmer()).winTrophy(Trophy.LAMA);
	}

	public void roxxor(Entity leek) {
		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE)
			return;
		if (leek.getFarmer() < 0)
			return;
		mFarmers.get(leek.getFarmer()).winTrophy(Trophy.ROXXOR);
	}

	public void summon(Entity caster, Entity summon) {
		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE)
			return;
		TrophyVariables tv = mFarmers.get(caster.getFarmer());
		tv.addSummoned(1);
	}

	public void addDamage(Entity entity, int damage) {
		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE) {
			return;
		}
		TrophyVariables tv = mFarmers.get(entity.getFarmer());
		if (tv != null) {
			tv.damage += damage;
		}
	}

	public void stashed(Entity entity) {
		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE) {
			return;
		}
		if (entity.getFarmer() < 0) {
			return;
		}
		mFarmers.get(entity.getFarmer()).winTrophy(Trophy.STASHED);
	}

	public void sniper(Entity entity) {
		if (mFight.getFightContext() == Fight.CONTEXT_TEST || mFight.getFightContext() == Fight.CONTEXT_CHALLENGE) {
			return;
		}
		if (entity.getFarmer() < 0) {
			return;
		}
		mFarmers.get(entity.getFarmer()).winTrophy(Trophy.SNIPER);
	}
}
