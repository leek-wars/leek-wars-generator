package com.leekwars.generator.polyglot;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;

/**
 * Instrument Truffle qui compte les statements guest executes, PAR THREAD.
 *
 * Sert de compteur d'operations DETERMINISTE pour les IA polyglot : le nombre de statements executes
 * par un meme code sur les memes entrees est reproductible (contrairement au compteur temps-based de
 * {@link PolyglotEntityAI#getOperations()}). Les IA de recherche (qui bornent leur exploration sur
 * {@code getOperations() > seuil}) redeviennent ainsi bit-reproductibles -> replays fiables, arene classee.
 *
 * Chaque IA execute son guest de facon SYNCHRONE sur le thread de combat ; le compteur est donc
 * ThreadLocal (remis a zero au debut de tour par {@link PolyglotEntityAI}, lu dans getOperations()).
 * Decouvert par l'embedding via {@code engine.getInstruments().get(ID).lookup(Counter.class)} (ce
 * lookup active l'instrument).
 */
@TruffleInstrument.Registration(id = StatementCounter.ID, name = "LeekWars Statement Counter", services = StatementCounter.Counter.class)
public final class StatementCounter extends TruffleInstrument {

	public static final String ID = "lw-statement-counter";

	/** Compteur de statements guest par thread (le travail d'une IA tient sur le thread de combat). */
	public static final class Counter {
		private final ThreadLocal<long[]> count = ThreadLocal.withInitial(() -> new long[1]);
		public long get() { return count.get()[0]; }
		public void reset() { count.get()[0] = 0L; }
		void increment() { count.get()[0]++; }
	}

	private final Counter counter = new Counter();

	@Override
	protected void onCreate(Env env) {
		env.registerService(counter);
		SourceSectionFilter filter = SourceSectionFilter.newBuilder()
				.tagIs(StandardTags.StatementTag.class)
				.build();
		env.getInstrumenter().attachExecutionEventListener(filter, new ExecutionEventListener() {
			@Override public void onEnter(EventContext context, VirtualFrame frame) { counter.increment(); }
			@Override public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {}
			@Override public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {}
		});
	}
}
