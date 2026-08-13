package test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.Assert;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotFileSystem;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.SandboxPolicy;

/** Mesure : fermer un Engine isolate rend-il sa mémoire native ? (viabilité du recyclage, #4631) */
public class TestScratchRecycle {

	private static long rssMB() {
		try {
			for (String line : Files.readAllLines(Paths.get("/proc/self/status"))) {
				if (line.startsWith("VmRSS:")) {
					return Long.parseLong(line.trim().split("\\s+")[1]) / 1024;
				}
			}
		} catch (Exception ignore) {}
		return -1;
	}

	@Test
	public void engineCloseReturnsNativeMemory() throws Exception {
		int generations = Integer.getInteger("leak.gens", 6);
		int contextsPerGen = Integer.getInteger("leak.ctxs", 15);
		long base = rssMB();
		System.out.printf("[recycle] base RSS = %d MB%n", base);
		for (int g = 0; g < generations; g++) {
			Engine engine = Engine.newBuilder("python")
				.sandbox(SandboxPolicy.ISOLATED)
				.option("engine.MaxIsolateMemory", "4GB")
				.out(java.io.OutputStream.nullOutputStream())
				.err(java.io.OutputStream.nullOutputStream())
				.build();
			for (int i = 0; i < contextsPerGen; i++) {
				try (Context c = Context.newBuilder("python")
						.engine(engine)
						.sandbox(SandboxPolicy.ISOLATED)
						.option("sandbox.MaxHeapMemory", "100MB")
						.option("sandbox.MaxStatements", "500000000")
						.option("sandbox.MaxCPUTime", "20s")
						.option("sandbox.MaxCPUTimeCheckInterval", "5ms")
						.option("sandbox.MaxStackFrames", "4096")
						.option("sandbox.MaxThreads", "1")
						.option("sandbox.MaxASTDepth", "1000")
						.option("sandbox.MaxOutputStreamSize", "1MB")
						.option("sandbox.MaxErrorStreamSize", "1MB")
						.out(java.io.OutputStream.nullOutputStream())
						.err(java.io.OutputStream.nullOutputStream())
						.build()) {
					c.eval("python", "cache = {k: 'v' * 50 for k in range(20000)}\nlen(cache)");
				}
			}
			engine.close();
			System.gc();
			Thread.sleep(300);
			System.out.printf("[recycle] gen %d fermee : RSS = %d MB (delta base %+d)%n", g + 1, rssMB(), rssMB() - base);
		}
	}
}
