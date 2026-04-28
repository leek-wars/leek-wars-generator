package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.State;

/**
 * Battle Royale fights: each entity gets its own team, all enemies of each
 * other. Probes the winner determination edge case where {@link
 * com.leekwars.generator.fight.Fight#computeWinner} only compares teams 0
 * and 1 for the draw-by-life tiebreaker — known limitation in BR.
 */
public class TestFightBR extends FightTestBase {

	private Leek a, b, c, d;

	@Override
	protected void createLeeks() {
		// Set BR type BEFORE adding entities so they get distinct teams.
		fight.getState().setType(State.TYPE_BATTLE_ROYALE);
		a = defaultLeek(1, "A");
		b = defaultLeek(2, "B");
		c = defaultLeek(3, "C");
		d = defaultLeek(4, "D");
		// All added with team=0; State.addEntity reassigns to next team in BR mode
		fight.getState().addEntity(0, a);
		fight.getState().addEntity(0, b);
		fight.getState().addEntity(0, c);
		fight.getState().addEntity(0, d);
	}

	// ---------- Team assignment ----------

	@Test
	public void brAssignsDistinctTeamsAutomatically() throws Exception {
		initFightOnly();
		Assert.assertEquals(4, fight.getState().getTeams().size());
		Assert.assertEquals(0, a.getTeam());
		Assert.assertEquals(1, b.getTeam());
		Assert.assertEquals(2, c.getTeam());
		Assert.assertEquals(3, d.getTeam());
	}

	@Test
	public void allLeeksAreMutualEnemies() throws Exception {
		attachAI(a, "setRegister('enemies', '' + count(getEnemies()));");
		attachAI(b, "");
		attachAI(c, "");
		attachAI(d, "");
		runFight();
		Assert.assertEquals("3", a.getRegister("enemies"));
	}

	@Test
	public void aHasNoAlliesInBR() throws Exception {
		attachAI(a, "setRegister('allies', '' + count(getAllies()));");
		attachAI(b, "");
		attachAI(c, "");
		attachAI(d, "");
		runFight();
		// getAllies returns the team's entities (including self) — in BR each team is solo
		Assert.assertEquals("1", a.getRegister("allies"));
	}

	// ---------- Winner determination ----------

	@Test
	public void brWithAllAliveEndsAsDraw() throws Exception {
		// All 4 alive, no damage, drawCheckLife=true. computeWinner: alive=4 (≠ 1)
		// → mWinteam = -1. Draw check compares only teams 0 and 1, both equal → still -1.
		attachAI(a, "");
		attachAI(b, "");
		attachAI(c, "");
		attachAI(d, "");
		runFight();
		Assert.assertEquals("All-alive BR should be a draw", -1, fight.getWinner());
	}

	@Test
	public void brWithOneAliveDeclaresThemWinner() throws Exception {
		attachAI(a, "");
		attachAI(b, "");
		attachAI(c, "");
		attachAI(d, "");
		runFight();
		// Kill 3 of 4, run computeWinner manually
		b.removeLife(b.getLife(), 0, a, DamageType.DIRECT, null, null);
		c.removeLife(c.getLife(), 0, a, DamageType.DIRECT, null, null);
		d.removeLife(d.getLife(), 0, a, DamageType.DIRECT, null, null);
		fight.computeWinner(true);
		Assert.assertEquals("Last alive in BR wins", a.getTeam(), fight.getWinner());
	}

	@Test
	public void brTiebreakerComparesAllTeamsLife() throws Exception {
		// computeWinner with drawCheckLife=true picks the team with the strictly
		// highest life across all teams (used to only compare teams 0 and 1, broken
		// in BR with 3+ teams).
		attachAI(a, "");
		attachAI(b, "");
		attachAI(c, "");
		attachAI(d, "");
		runFight();
		// Kill teams 0 and 1, leave c (team 2) at full and d (team 3) at 400.
		a.removeLife(a.getLife(), 0, c, DamageType.DIRECT, null, null);
		b.removeLife(b.getLife(), 0, c, DamageType.DIRECT, null, null);
		d.removeLife(100, 0, c, DamageType.DIRECT, null, null);
		fight.computeWinner(true);
		// alive=2 (c and d), neither team uniquely alive → tiebreaker. c has 500 hp, d has 400.
		Assert.assertEquals("Highest-life team wins the tiebreaker", c.getTeam(), fight.getWinner());
	}

	@Test
	public void brTiebreakerWithEqualLifeReturnsMinusOne() throws Exception {
		attachAI(a, "");
		attachAI(b, "");
		attachAI(c, "");
		attachAI(d, "");
		runFight();
		// All four still at 500 hp → tiebreaker has 4 winners, no unique max → -1.
		fight.computeWinner(true);
		Assert.assertEquals("Multi-way tie returns no winner", -1, fight.getWinner());
	}

	// ---------- Fight type / context introspection ----------

	@Test
	public void getFightTypeReportsBattleRoyale() throws Exception {
		attachAI(a, "setRegister('type', '' + getFightType());");
		attachAI(b, "");
		attachAI(c, "");
		attachAI(d, "");
		runFight();
		String type = a.getRegister("type");
		Assert.assertNotNull(type);
		Assert.assertEquals("FIGHT_TYPE_BATTLE_ROYALE = 3 (matches State.TYPE_BATTLE_ROYALE)",
			"3", type);
	}

	// ---------- Hooks ----------

	@Test
	public void beforeFightRunsForAllBRParticipants() throws Exception {
		String marker = "function beforeFight() { setRegister('ran', 'yes'); }";
		attachAI(a, marker);
		attachAI(b, marker);
		attachAI(c, marker);
		attachAI(d, marker);
		runFight();
		Assert.assertEquals("yes", a.getRegister("ran"));
		Assert.assertEquals("yes", b.getRegister("ran"));
		Assert.assertEquals("yes", c.getRegister("ran"));
		Assert.assertEquals("yes", d.getRegister("ran"));
	}

	// ---------- Side query ----------

	@Test
	public void getSideMatchesTeamInBR() throws Exception {
		attachAI(a, "setRegister('side', '' + getSide());");
		attachAI(b, "setRegister('side', '' + getSide());");
		attachAI(c, "setRegister('side', '' + getSide());");
		attachAI(d, "setRegister('side', '' + getSide());");
		runFight();
		Assert.assertEquals("0", a.getRegister("side"));
		Assert.assertEquals("1", b.getRegister("side"));
		Assert.assertEquals("2", c.getRegister("side"));
		Assert.assertEquals("3", d.getRegister("side"));
	}
}
