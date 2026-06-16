package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.entity.EntityAI;

import leekscript.runner.LeekFunctions;

/**
 * Issue #4165: in strict mode the engine hooks (beforeFight/afterFight) were
 * reported as unused functions because they are invoked by reflection, not from
 * user code. Constructing a {@link Generator} must register the hook names as
 * analyzer entry-point functions so the unused-function pass skips them. The
 * suppression itself is covered by leekscript's TestGeneral.testUnusedFunction;
 * here we verify the generator wires the names in.
 */
public class TestHookAnalysis {

	@Test
	public void generatorRegistersHookNamesAsEntryPoints() {
		new Generator();
		for (String hook : EntityAI.HOOK_NAMES) {
			Assert.assertTrue("Hook " + hook + " should be registered as an entry-point function",
				LeekFunctions.isEntryPointFunction(hook));
		}
	}

	@Test
	public void nonHookFunctionsAreNotEntryPoints() {
		new Generator();
		Assert.assertFalse("A regular function name must not be treated as an entry point",
			LeekFunctions.isEntryPointFunction("notAHook"));
	}
}
