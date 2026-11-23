package com.leekwars.generator.polyglot;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongUnaryOperator;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ContextsListener;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.ContextLocal;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;
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
 * Compile DANS l'image native isolate (via isolate_deps / LW_INSTRUMENT). Un service d'instrument
 * ne traverse PAS la frontiere isolate (son lookup hote renvoie null) ; le compteur est donc expose
 * via les POLYGLOT BINDINGS,
 * seule voie marshallee par le nativebridge : a la creation de chaque contexte, l'instrument
 * publie un objet executable sous {@link #BINDING_NAME} ; l'hote le lit via
 * context.getPolyglotBindings().getMember(...).execute() (0 arg = lire, 1 arg = reset).
 *
 * Un compteur PAR CONTEXTE ({@link ContextLocal}) : l'engine, donc cet instrument, est partage
 * par tous les contextes du processus, qui peuvent tourner en parallele sur plusieurs threads.
 * Un compteur unique additionnait les statements de tous ces contextes (et la remise a zero de
 * l'un effacait ceux des autres). Un contexte ne s'execute que sur un thread a la fois : le
 * compteur reste un simple champ long, PAS de ThreadLocal (blocklist en compilation runtime
 * native-image).
 *
 * <p>Publie aussi {@link #HEAP_BINDING_NAME} : la memoire retenue par le contexte (System.usedRAM
 * des IA polyglot), meme calcul que celui de sandbox.MaxHeapMemory.
 */
@TruffleInstrument.Registration(id = StatementCounter.ID, name = "LeekWars Statement Counter", sandbox = SandboxPolicy.ISOLATED)
public final class StatementCounter extends TruffleInstrument {

	public static final String ID = "lw-statement-counter";
	public static final String BINDING_NAME = "lwStatementCounter";
	public static final String HEAP_BINDING_NAME = "lwHeapSize";
	public static final String READER_BINDING_NAME = "lwOperations";

	/**
	 * Option d'activation : un instrument est lazy et, sous isolate, aucun lookup de service
	 * hote ne peut l'activer ; l'engine doit poser
	 * {@code .option("lw-statement-counter", "true")} pour activer l'instrument.
	 */
	@Option(name = "", help = "Enable the LeekWars statement counter.", category = OptionCategory.EXPERT, stability = OptionStability.STABLE, sandbox = SandboxPolicy.ISOLATED)
	static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);

	@Override
	protected OptionDescriptors getOptionDescriptors() {
		return new StatementCounterOptionDescriptors();
	}

	/** Message de l'erreur levee au-dela de la limite, reconnu par le generateur (PolyglotEntityAI). */
	public static final String LIMIT_MESSAGE = "operation limit reached in Leek Wars";

	/**
	 * Compteur d'operations d'un contexte, publie pour l'HOTE dans ses polyglot bindings ({@link #BINDING_NAME}) :
	 * <ul>
	 * <li>execute() : le compte (statements, expressions, natif) ;</li>
	 * <li>execute("reset") : remet le compte a zero ;</li>
	 * <li>execute("turn", budget, facteur, hote) : debut de tour, compte a zero ;</li>
	 * <li>execute("host", hote) : operations faites par l'hote depuis le debut du tour ;</li>
	 * <li>execute("exceeded") : vrai si le compte a depasse la limite (meme si le guest a attrape l'erreur).</li>
	 * </ul>
	 * Le total du tour est hote + compte x facteur ; au-dela du budget, chaque operation comptee leve
	 * {@link LimitReached} : attrapee par le guest, l'erreur revient a l'operation suivante. Le guest ne
	 * voit que {@link ReaderObject} : avec cet objet, il remettrait son compte a zero ou leverait la limite.
	 *
	 * <p>Un langage y ajoute les operations qu'il fait hors de ses noeuds instrumentes (boucles natives
	 * de ses builtins) par {@link LongUnaryOperator#applyAsLong}, interface du JDK qu'il connait sans
	 * dependre de cet instrument : GraalPy recoit l'objet du generateur (cf patches/graalpython.patch).
	 */
	@ExportLibrary(InteropLibrary.class)
	static final class CounterObject implements TruffleObject, LongUnaryOperator {
		long count;
		long limit = Long.MAX_VALUE;
		long budget = Long.MAX_VALUE;
		double factor = 1;
		long host;

		/** Ajoute n operations, rend le reste avant la limite (negatif : depassee). */
		@Override
		public long applyAsLong(long n) {
			count += n;
			return limit - count;
		}

		/** Total du tour, dans l'unite du budget. */
		long total() {
			return host + (long) (count * factor);
		}

		/** Limite du compte : ce qui reste du budget apres l'hote, en operations comptees. */
		private void updateLimit() {
			limit = budget == Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor((budget - host) / factor);
		}

		@ExportMessage boolean isExecutable() { return true; }
		@ExportMessage @TruffleBoundary Object execute(Object[] args) throws UnsupportedMessageException {
			if (args.length > 0) {
				InteropLibrary interop = InteropLibrary.getUncached();
				String command = interop.isString(args[0]) ? interop.asString(args[0]) : "reset";
				if (command.equals("turn") && args.length == 4) {
					count = 0L;
					budget = interop.asLong(args[1]);
					factor = interop.asDouble(args[2]);
					host = interop.asLong(args[3]);
					updateLimit();
				} else if (command.equals("host") && args.length == 2) {
					host = interop.asLong(args[1]);
					updateLimit();
				} else if (command.equals("exceeded")) {
					return count > limit;
				} else {
					count = 0L;
				}
			}
			return count;
		}
	}

	/** Facade du compteur pour le GUEST ({@link #READER_BINDING_NAME}) : execute() = total du tour, en lecture seule. */
	@ExportLibrary(InteropLibrary.class)
	static final class ReaderObject implements TruffleObject {
		private final CounterObject counter;
		ReaderObject(CounterObject counter) { this.counter = counter; }
		@ExportMessage boolean isExecutable() { return true; }
		@ExportMessage Object execute(@SuppressWarnings("unused") Object[] args) {
			return counter.total();
		}
	}

	/** Limite d'operations du contexte depassee : visible du guest, comme une erreur du langage. */
	static final class LimitReached extends AbstractTruffleException {
		private static final long serialVersionUID = 1L;

		LimitReached(Node location) {
			super(LIMIT_MESSAGE, location);
		}
	}

	@TruffleBoundary
	private static LimitReached limitReached(Node location) {
		return new LimitReached(location);
	}

	/**
	 * Facade interop : execute(stopAtBytes) = octets retenus par le contexte qui l'a publiee. Le calcul
	 * parcourt le tas du contexte, son cout croit avec lui : il s'arrete des que stopAtBytes est depasse
	 * (et renvoie alors une valeur superieure a stopAtBytes). Sans argument, pas d'arret.
	 */
	@ExportLibrary(InteropLibrary.class)
	static final class HeapSizeObject implements TruffleObject {
		final Env env;
		final TruffleContext context;
		HeapSizeObject(Env env, TruffleContext context) { this.env = env; this.context = context; }
		@ExportMessage boolean isExecutable() { return true; }
		@ExportMessage @TruffleBoundary Object execute(Object[] args) throws UnsupportedMessageException {
			long stopAtBytes = args.length > 0 ? InteropLibrary.getUncached().asLong(args[0]) : Long.MAX_VALUE;
			return env.calculateContextHeapSize(context, stopAtBytes, new AtomicBoolean());
		}
	}

	private final ContextLocal<CounterObject> counters = locals.createContextLocal(context -> new CounterObject());

	@Override
	protected void onCreate(Env env) {
		ExecutionEventListener listener = new ExecutionEventListener() {
			@Override public void onEnter(EventContext context, VirtualFrame frame) {
				CounterObject counter = counters.get();
				if (++counter.count > counter.limit) {
					throw limitReached(context.getInstrumentedNode());
				}
			}
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
		// Publie la facade du compteur du contexte dans ses polyglot bindings (il faut etre
		// entre dans le contexte pour y acceder). Tente aux deux hooks (creation + init
		// langage) : le premier qui passe gagne, les erreurs sont loggees (jamais bloquant).
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
						InteropLibrary bindings = InteropLibrary.getUncached();
						CounterObject counter = counters.get(context);
						bindings.writeMember(env.getPolyglotBindings(), BINDING_NAME, counter);
						bindings.writeMember(env.getPolyglotBindings(), READER_BINDING_NAME, new ReaderObject(counter));
						bindings.writeMember(env.getPolyglotBindings(), HEAP_BINDING_NAME, new HeapSizeObject(env, context));
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
