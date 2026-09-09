package com.leekwars.generator.polyglot;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ContextsListener;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleContext;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.polyglot.SandboxPolicy;

/**
 * Instrument Truffle qui compte les unites de travail guest executees : STATEMENTS + EXPRESSIONS
 * pour tous les langages (granularite fine, proche du "1 op par operateur" LeekScript). GraalJS
 * tague nativement ; pour GraalPy c'est patches/graalpython.patch (repo leek-wars-graal-isolate)
 * qui emet les Tag(Expression). Version SPIKE custom isolate.
 *
 * Compile DANS l'image native isolate (via isolate_deps / LW_INSTRUMENT). Le lookup de service
 * hote (engine.getInstruments().get(ID).lookup(Counter.class)) ne traverse PAS la frontiere
 * isolate (verifie : renvoie null) ; le compteur est donc expose via les POLYGLOT BINDINGS,
 * seule voie marshallee par le nativebridge : a la creation de chaque contexte, l'instrument
 * publie un objet executable sous {@link #BINDING_NAME} ; l'hote le lit via
 * context.getPolyglotBindings().getMember(...).execute() (0 arg = lire, 1 arg = reset).
 *
 * Compteur = simple champ long : PAS de ThreadLocal (blocklist en compilation runtime
 * native-image, et un engine = un combat = un thread chez LeekWars).
 */
@TruffleInstrument.Registration(id = StatementCounter.ID, name = "LeekWars Statement Counter", services = StatementCounter.Counter.class, sandbox = SandboxPolicy.ISOLATED)
public final class StatementCounter extends TruffleInstrument {

	public static final String ID = "lw-statement-counter";
	public static final String BINDING_NAME = "lwStatementCounter";

	/**
	 * Option d'activation : sous isolate le lookup de service hote (qui activait l'instrument
	 * en in-process) ne traverse pas la frontiere ; l'engine doit poser
	 * {@code .option("lw-statement-counter", "true")} pour activer l'instrument.
	 */
	@Option(name = "", help = "Enable the LeekWars statement counter.", category = OptionCategory.EXPERT, stability = OptionStability.STABLE, sandbox = SandboxPolicy.ISOLATED)
	static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);

	@Override
	protected OptionDescriptors getOptionDescriptors() {
		return new StatementCounterOptionDescriptors();
	}

	/** Compteur de statements guest (un engine = un combat = un thread). */
	public static final class Counter {
		private long count;
		public long get() { return count; }
		public void reset() { count = 0L; }
		void increment() { count++; }
	}

	/** Facade interop : execute() = lire, execute(x) = reset. Publiee dans les polyglot bindings. */
	@ExportLibrary(InteropLibrary.class)
	static final class CounterObject implements TruffleObject {
		final Counter counter;
		CounterObject(Counter counter) { this.counter = counter; }
		@ExportMessage boolean isExecutable() { return true; }
		@ExportMessage Object execute(Object[] args) {
			if (args.length > 0) {
				counter.reset();
				return 0L;
			}
			return counter.get();
		}
	}

	private final Counter counter = new Counter();

	@Override
	protected void onCreate(Env env) {
		env.registerService(counter);
		ExecutionEventListener listener = new ExecutionEventListener() {
			@Override public void onEnter(EventContext context, VirtualFrame frame) { counter.increment(); }
			@Override public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {}
			@Override public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {}
		};
		// Granularite EXPRESSION pour TOUS les langages : chaque statement et chaque sous-expression
		// executee compte, proche du modele LeekScript "1 op par operateur" -> facteur de calibration
		// cote generator proche de 1. GraalJS tague nativement ; GraalPy ne taguait que les
		// statements (une ligne = un evenement, les compréhensions gratuites) : notre build applique
		// patches/graalpython.patch (repo leek-wars-graal-isolate) qui ouvre un Tag(Expression) sur
		// les operateurs, appels, acces d'attribut/indice, litteraux et chaque iteration de
		// comprehension, et un Tag(Statement) par statement meme sur la ligne du parent.
		// Un noeud tague Statement ET Expression n'a qu'un seul probe -> compte UNE fois.
		// Les sources nommees "lw:..." (preludes du generator : API objet, gardes de determinisme,
		// enveloppes de facturation, override de System.operations) ne sont PAS comptees : leur
		// travail est O(1) par appel ou deja facture par l'hote (fonctions de combat au cout
		// LeekScript, builtins natifs au prorata via __lw_charge). Sans ce filtre, `min(a, b)`
		// coutait ~13 et `me.cell` ~40 rien qu'en expressions du prelude. Le code du joueur et la
		// bibliotheque standard (sources nommees autrement) restent comptes.
		SourceSectionFilter filter = SourceSectionFilter.newBuilder()
				.sourceIs(source -> source.getName() == null || !source.getName().startsWith("lw:"))
				.tagIs(StandardTags.StatementTag.class, StandardTags.ExpressionTag.class)
				.build();
		env.getInstrumenter().attachExecutionEventListener(filter, listener);
		// Publie la facade dans les polyglot bindings de chaque contexte cree (il faut etre
		// entre dans le contexte pour y acceder). Tente aux deux hooks (creation + init
		// langage) : le premier qui passe gagne, les erreurs sont loggees (jamais bloquant).
		CounterObject facade = new CounterObject(counter);
		env.getInstrumenter().attachContextsListener(new ContextsListener() {
			private void publish(TruffleContext context, String where) {
				try {
					Object prev = null;
					boolean entered = false;
					try {
						prev = context.enter(null);
						entered = true;
					} catch (Exception alreadyEnteredOrForbidden) {
						// peut etre deja entre : on tente l'ecriture directe
					}
					try {
						InteropLibrary.getUncached().writeMember(env.getPolyglotBindings(), BINDING_NAME, facade);
					} finally {
						if (entered) {
							context.leave(null, prev);
						}
					}
				} catch (Exception e) {
					env.getLogger(StatementCounter.class).severe(
							"publish bindings (" + where + ") a echoue: " + e);
				}
			}
			@Override public void onContextCreated(TruffleContext context) { publish(context, "onContextCreated"); }
			@Override public void onLanguageContextCreated(TruffleContext context, LanguageInfo language) {}
			@Override public void onLanguageContextInitialized(TruffleContext context, LanguageInfo language) { publish(context, "onLanguageContextInitialized"); }
			@Override public void onLanguageContextFinalized(TruffleContext context, LanguageInfo language) {}
			@Override public void onLanguageContextDisposed(TruffleContext context, LanguageInfo language) {}
			@Override public void onContextClosed(TruffleContext context) {}
		}, true);
	}
}
