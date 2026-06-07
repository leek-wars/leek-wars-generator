package com.leekwars.generator.polyglot;

import org.graalvm.polyglot.Value;

/**
 * Conversions entre les valeurs guest (GraalVM {@link Value}, cote JS/Python) et les
 * objets Java attendus / renvoyes par l'API de combat (LeekScript).
 *
 * Etape 1 : primitifs (entiers, reels, booleens, chaines). Les tableaux et maps
 * (GenericArrayLeekValue &lt;-&gt; ProxyArray, MapLeekValue &lt;-&gt; ProxyObject) seront
 * traites a l'etape 2.
 */
public final class TypeMarshaller {

	private TypeMarshaller() {}

	/** Convertit une valeur guest en l'objet Java attendu par un parametre de methode de combat. */
	public static Object coerce(Value v, Class<?> target) {
		if (v == null || v.isNull()) {
			// Un parametre primitif ne peut pas recevoir null : on fournit la valeur par defaut
			// du type (eviter un NPE a l'unboxing lors de l'invocation reflective).
			if (target == long.class) return 0L;
			if (target == int.class) return 0;
			if (target == double.class) return 0.0;
			if (target == boolean.class) return false;
			return null; // Object / String / type boxe
		}
		if (target == long.class || target == Long.class) {
			return toLong(v);
		}
		if (target == int.class || target == Integer.class) {
			return (int) toLong(v);
		}
		if (target == double.class || target == Double.class) {
			return toDouble(v);
		}
		if (target == boolean.class || target == Boolean.class) {
			return toBoolean(v);
		}
		if (target == String.class) {
			return v.isString() ? v.asString() : v.toString();
		}
		// Parametre Object (cas le plus frequent) ou type non gere : conversion generique.
		return toJava(v);
	}

	/** Conversion generique guest -&gt; Java (parametres Object et valeur de retour de runIA). */
	public static Object toJava(Value v) {
		if (v == null || v.isNull()) {
			return null;
		}
		if (v.isBoolean()) {
			return v.asBoolean();
		}
		if (v.isNumber()) {
			// Un nombre guest entier reste entier (long) cote LeekScript, sinon reel.
			return v.fitsInLong() ? (Object) v.asLong() : (Object) v.asDouble();
		}
		if (v.isString()) {
			return v.asString();
		}
		// Tableaux / objets guest : marshalling complet a l'etape 2.
		return v.toString();
	}

	/** Conversion Java -&gt; guest pour la valeur de retour d'une fonction de combat. */
	public static Object toGuest(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number || o instanceof Boolean || o instanceof String || o instanceof Character) {
			return o; // GraalVM convertit automatiquement les primitifs hote en valeur guest
		}
		// LeekValue (tableaux / maps) : a wrapper en ProxyArray/ProxyObject a l'etape 2.
		return o;
	}

	private static long toLong(Value v) {
		if (v.fitsInLong()) {
			return v.asLong();
		}
		if (v.isNumber()) {
			double d = v.asDouble();
			// NaN / +-Infinity n'ont pas de representation entiere : valeur definie 0
			// (eviter (long) Infinity == Long.MAX_VALUE qui produirait des tailles aberrantes).
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return 0L;
			}
			return (long) d;
		}
		if (v.isBoolean()) {
			return v.asBoolean() ? 1L : 0L;
		}
		if (v.isString()) {
			try {
				return Long.parseLong(v.asString().trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static double toDouble(Value v) {
		if (v.isNumber()) {
			return v.fitsInDouble() ? v.asDouble() : (double) v.asLong();
		}
		if (v.isBoolean()) {
			return v.asBoolean() ? 1.0 : 0.0;
		}
		if (v.isString()) {
			try {
				return Double.parseDouble(v.asString().trim());
			} catch (NumberFormatException e) {
				return 0.0;
			}
		}
		return 0.0;
	}

	private static boolean toBoolean(Value v) {
		if (v.isBoolean()) {
			return v.asBoolean();
		}
		// Truthiness facon langage guest : 0 / "" / null sont faux.
		if (v.isNumber()) {
			return v.asDouble() != 0.0;
		}
		if (v.isString()) {
			return !v.asString().isEmpty();
		}
		return !v.isNull();
	}
}
