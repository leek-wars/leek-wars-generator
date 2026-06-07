package com.leekwars.generator.polyglot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.FightFunctions;
import com.leekwars.generator.fight.entity.EntityAI;

import leekscript.common.Type;
import leekscript.compiler.LeekScript;
import leekscript.runner.LeekFunctions;

/**
 * Expose l'API de combat (fonctions + constantes) dans le scope global d'un contexte
 * GraalVM, pour que les IA JS/Python puissent l'appeler comme en LeekScript.
 *
 * Resolution nom -&gt; methode Java (validee sur le code genere v4) :
 *   nom logique "Entity"/"Fight"/"Util"... -&gt; classe {@code com.leekwars.generator.classes.<Nom>Class}
 *   methode statique du meme nom, avec l'{@link EntityAI} injecte en 1er argument, puis les
 *   arguments utilisateur. On cible la derniere version du langage : pas de variantes _v1_3.
 *
 * La resolution reflective (independante de l'entite) est calculee UNE fois au chargement
 * de la classe et reutilisee pour chaque contexte ; seul le binding du ProxyExecutable
 * (qui capture l'EntityAI) est refait par contexte.
 */
public class PolyglotAPIBridge {

	private static final String CLASSES_PACKAGE = "com.leekwars.generator.classes.";

	/** nom de fonction -&gt; (arite utilisateur -&gt; methode statique). Resolu une seule fois. */
	private static final Map<String, Map<Integer, Method>> FUNCTIONS = resolveFunctions();
	/** nom de constante -&gt; valeur (long pour les INT, double sinon). Resolu une seule fois. */
	private static final Map<String, Object> CONSTANTS = resolveConstants();

	/** Installe fonctions et constantes de combat dans le scope global du contexte. */
	public static void install(Context context, String languageId, EntityAI ai) {
		Value bindings = context.getBindings(languageId);
		for (Map.Entry<String, Map<Integer, Method>> entry : FUNCTIONS.entrySet()) {
			bindings.putMember(entry.getKey(), makeProxy(ai, entry.getKey(), entry.getValue()));
		}
		for (Map.Entry<String, Object> entry : CONSTANTS.entrySet()) {
			bindings.putMember(entry.getKey(), entry.getValue());
		}
	}

	private static Map<String, Map<Integer, Method>> resolveFunctions() {
		Map<String, Map<Integer, Method>> resolved = new HashMap<>();
		for (Map.Entry<String, LeekFunctions> entry : FightFunctions.getFunctions().entrySet()) {
			LeekFunctions fn = entry.getValue();
			// On cible la derniere version : on ignore les fonctions retirees ou pas encore disponibles.
			if (LeekScript.LATEST_VERSION < fn.getMinVersion() || LeekScript.LATEST_VERSION > fn.getMaxVersion()) {
				continue;
			}
			Map<Integer, Method> byArity = resolveOverloads(fn.getStandardClass(), entry.getKey());
			if (!byArity.isEmpty()) {
				resolved.put(entry.getKey(), byArity);
			}
		}
		return resolved;
	}

	private static Map<String, Object> resolveConstants() {
		Map<String, Object> resolved = new HashMap<>();
		for (FightConstants c : FightConstants.values()) {
			Object value = c.getType() == Type.INT ? (Object) (long) c.getIntValue() : (Object) c.getValue();
			resolved.put(c.name(), value);
		}
		return resolved;
	}

	/** Resout les surcharges (par arite utilisateur) de la methode statique implementant {@code name}. */
	private static Map<Integer, Method> resolveOverloads(String standardClass, String name) {
		Map<Integer, Method> byArity = new HashMap<>();
		try {
			Class<?> clazz = Class.forName(CLASSES_PACKAGE + standardClass + "Class");
			for (Method m : clazz.getMethods()) {
				if (!m.getName().equals(name)) {
					continue;
				}
				Class<?>[] params = m.getParameterTypes();
				// 1er parametre = l'EntityAI injecte ; on accepte EntityAI ou un supertype (AI).
				if (params.length == 0 || !params[0].isAssignableFrom(EntityAI.class)) {
					continue;
				}
				byArity.putIfAbsent(params.length - 1, m); // arite cote utilisateur = sans l'EntityAI
			}
		} catch (ClassNotFoundException e) {
			// Pas de classe d'implementation pour ce nom logique : ignore (cas residuels en etape 2).
		}
		return byArity;
	}

	private static ProxyExecutable makeProxy(EntityAI ai, String name, Map<Integer, Method> byArity) {
		return (Value... args) -> {
			Method m = byArity.get(args.length);
			if (m == null) {
				// Arite exacte absente : on prend la plus grande surcharge <= args.length.
				int best = -1;
				for (int arity : byArity.keySet()) {
					if (arity <= args.length && arity > best) {
						best = arity;
					}
				}
				m = best >= 0 ? byArity.get(best) : null;
				if (m == null) {
					throw new IllegalArgumentException("Fonction " + name + " : nombre d'arguments invalide (" + args.length + ")");
				}
			}
			Class<?>[] params = m.getParameterTypes();
			Object[] callArgs = new Object[params.length];
			callArgs[0] = ai;
			for (int i = 1; i < params.length; i++) {
				Value a = (i - 1) < args.length ? args[i - 1] : null;
				callArgs[i] = TypeMarshaller.coerce(a, params[i]);
			}
			try {
				return TypeMarshaller.toGuest(m.invoke(null, callArgs));
			} catch (InvocationTargetException ite) {
				Throwable cause = ite.getCause();
				// LeekRunException (checked) et autres erreurs remontent comme exception hote ;
				// PolyglotEntityAI.runIA les deballe via isHostException() + getCause().
				if (cause instanceof RuntimeException) {
					throw (RuntimeException) cause;
				}
				throw new RuntimeException(cause == null ? ite : cause);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		};
	}
}
