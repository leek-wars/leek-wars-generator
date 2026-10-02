package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.scenario.EntityInfo;

import leekscript.compiler.Folder;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

/**
 * L'AIFile d'une IA désignée par son chemin est partagé entre les combats des différents
 * threads. EntityAI.resolve y écrit le mode enregistré en base pendant qu'un autre thread
 * peut être en train de le compiler : l'écriture doit attendre la fin de la compilation,
 * sinon le Java généré mélange code strict et non strict, et javac le refuse.
 */
public class TestEntityAIResolve {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test(timeout = 10000)
	public void resolveWaitsForTheCompilationOfTheSameFile() throws Exception {
		Files.writeString(tmp.getRoot().toPath().resolve("main.leek"), "return 1");
		var root = new Folder(0, 0, tmp.getRoot().toString(), null, null, new NativeFileSystem(), 0);
		LeekScript.setFileSystem(new NativeFileSystem() {
			@Override public Folder getRoot(int owner) { return root; }
		});
		try {
			var file = root.resolve("main.leek");

			// Un autre combat équipe la même IA, avec le mode de la base (sans le pragma)
			var info = new EntityInfo();
			info.ai_path = "main.leek";
			info.ai_strict = false;
			var otherFight = new Thread(() -> EntityAI.resolve(null, info, null));

			// Compilation en cours (EntityAI.build) : le pragma // @strict est appliqué
			synchronized (file) {
				file.setVersion(LeekScript.LATEST_VERSION, true);
				otherFight.start();
				while (otherFight.isAlive() && otherFight.getState() != Thread.State.BLOCKED) {
					Thread.sleep(1);
				}
				assertEquals(Thread.State.BLOCKED, otherFight.getState());
				assertTrue(file.isStrict());
			}

			otherFight.join();
			assertFalse(file.isStrict());
		} finally {
			LeekScript.resetFileSystem();
		}
	}
}
