package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;

/**
 * Régression rapport #11806855 : assigner le résultat integer? d'une builtin
 * (getSummoner/getWeapon) à un champ typé classe utilisateur nullable générait
 * un cast Java invalide `(u_Entity) (Long)` qui faisait crasher la compilation
 * Java du worker (COMPILE_JAVA) au lieu de rester un warning + erreur runtime.
 */
public class TestUserClassFieldCast extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	@Test
	public void integerNullableBuiltinAssignedToUserClassFieldCompiles() throws Exception {
		attachAI(leek1, ""
			+ "class Weapon {}\n"
			+ "class Entity {\n"
			+ "	public Entity? summoner\n"
			+ "	public Weapon? weapon\n"
			+ "	public constructor(integer id) {\n"
			+ "		this.summoner = getSummoner(id);\n"
			+ "		this.weapon = getWeapon(id);\n"
			+ "	}\n"
			+ "}\n"
			+ "var e = new Entity(getEntity());\n"
			+ "setRegister('done', 'yes');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("yes", leek1.getRegister("done"));
	}
}
