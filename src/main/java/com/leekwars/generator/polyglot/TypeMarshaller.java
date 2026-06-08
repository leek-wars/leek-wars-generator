package com.leekwars.generator.polyglot;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

import leekscript.common.Error;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.GenericMapLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.values.SetLeekValue;

/**
 * Conversions entre les valeurs guest (GraalVM {@link Value}, cote JS/Python) et les
 * objets Java attendus / renvoyes par l'API de combat (LeekScript).
 *
 * Primitifs : entiers, reels, booleens, chaines.
 * Collections :
 *   Java -&gt; guest : {@link GenericArrayLeekValue}/{@link SetLeekValue} -&gt; {@link ProxyArray},
 *                    {@link MapLeekValue} -&gt; {@link ProxyObject} (vues paresseuses, lecture seule).
 *   guest -&gt; Java : tableau guest -&gt; {@link ArrayLeekValue}, objet guest -&gt; {@link MapLeekValue}.
 */
public final class TypeMarshaller {

	/**
	 * Profondeur d'imbrication maximale lors du marshalling guest -&gt; Java. Borne les
	 * structures cycliques (ex: tableau JS auto-reference) AVANT l'epuisement de la pile :
	 * la conversion descend recursivement, donc une structure cyclique boucle a l'infini.
	 */
	private static final int MAX_MARSHALLING_DEPTH = 500;

	private TypeMarshaller() {}

	// ----- guest -> Java -----

	/** Convertit une valeur guest en l'objet Java attendu par un parametre de methode de combat. */
	public static Object coerce(Value v, Class<?> target, AI ai) {
		if (v == null || v.isNull()) {
			// Un parametre primitif ne peut pas recevoir null : valeur par defaut du type
			// (eviter un NPE a l'unboxing lors de l'invocation reflective).
			if (target == long.class) return 0L;
			if (target == int.class) return 0;
			if (target == double.class) return 0.0;
			if (target == boolean.class) return false;
			return null; // Object / String / type boxe / collection
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
		// Collections : le marshalling peut lever (ops/RAM/profondeur). Appele depuis le
		// ProxyExecutable qui n'autorise pas d'exception checkee -> on encapsule
		// (deballee par PolyglotEntityAI.mapException via isHostException()).
		try {
			if (GenericArrayLeekValue.class.isAssignableFrom(target)) {
				return toLeekArray(v, ai, 0);
			}
			if (GenericMapLeekValue.class.isAssignableFrom(target)) {
				return toLeekMap(v, ai, 0);
			}
			return toJava(v, ai, 0);
		} catch (LeekRunException e) {
			throw new RuntimeException(e);
		}
	}

	/** Conversion generique guest -&gt; Java (parametres Object et valeur de retour de runIA). */
	public static Object toJava(Value v, AI ai) throws LeekRunException {
		return toJava(v, ai, 0);
	}

	private static Object toJava(Value v, AI ai, int depth) throws LeekRunException {
		if (v == null || v.isNull()) {
			return null;
		}
		if (v.isBoolean()) {
			return v.asBoolean();
		}
		if (v.isNumber()) {
			return numberToJava(v);
		}
		if (v.isString()) {
			return v.asString();
		}
		if (v.hasArrayElements()) {
			return toLeekArray(v, ai, depth); // tableau JS, list/tuple Python
		}
		if (v.hasHashEntries()) {
			return toLeekMap(v, ai, depth); // dict Python, Map JS (cles/valeurs via l'API hash)
		}
		if (v.hasIterator()) {
			return toLeekArray(v, ai, depth); // set Python et autres iterables non indexes
		}
		if (v.hasMembers() && !v.canExecute()) {
			return toLeekMap(v, ai, depth); // objet JS { ... } (les methodes sont ignorees)
		}
		// Fonctions / objets opaques (modules Python, etc.) : pas de donnee LeekScript exploitable.
		// (La valeur de retour de runIA est de toute facon ignoree par le moteur en combat.)
		return null;
	}

	/** Nombre guest -&gt; long (entier) ou double (reel), avec garde sur les bignums Python. */
	private static Object numberToJava(Value v) {
		if (v.fitsInLong()) {
			return v.asLong();
		}
		if (v.fitsInDouble()) {
			return v.asDouble();
		}
		// Entier Python hors plage double : LeekScript n'a pas de bigint -> on sature.
		return v.fitsInBigInteger() ? clampBigInteger(v.asBigInteger()) : 0L;
	}

	private static long clampBigInteger(BigInteger b) {
		if (b.bitLength() >= 63) {
			return b.signum() >= 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
		}
		return b.longValue();
	}

	private static void checkDepth(int depth) throws LeekRunException {
		if (depth >= MAX_MARSHALLING_DEPTH) {
			// Structure trop imbriquee ou cyclique : on s'arrete avant l'epuisement de pile.
			throw new LeekRunException(Error.STACKOVERFLOW);
		}
	}

	/** Tableau guest -&gt; ArrayLeekValue (indexe : list/tuple/tableau JS ; ou iterable : set Python). */
	private static ArrayLeekValue toLeekArray(Value v, AI ai, int depth) throws LeekRunException {
		checkDepth(depth);
		ArrayLeekValue array = new ArrayLeekValue(ai);
		if (v.hasArrayElements()) {
			long n = v.getArraySize();
			for (long i = 0; i < n; i++) {
				ai.ops(1); // borne la largeur du marshalling (non compte par le statement limit)
				array.push(ai, toJava(v.getArrayElement(i), ai, depth + 1));
			}
		} else if (v.hasIterator()) {
			Value it = v.getIterator();
			while (it.hasIteratorNextElement()) {
				ai.ops(1);
				array.push(ai, toJava(it.getIteratorNextElement(), ai, depth + 1));
			}
		}
		return array;
	}

	/** Objet guest -&gt; MapLeekValue (dict Python via l'API hash, objet JS via les membres). */
	private static MapLeekValue toLeekMap(Value v, AI ai, int depth) throws LeekRunException {
		checkDepth(depth);
		MapLeekValue map = new MapLeekValue(ai);
		if (v.hasHashEntries()) {
			Value it = v.getHashEntriesIterator();
			while (it.hasIteratorNextElement()) {
				ai.ops(1);
				Value entry = it.getIteratorNextElement(); // [cle, valeur]
				map.set(ai, toJava(entry.getArrayElement(0), ai, depth + 1), toJava(entry.getArrayElement(1), ai, depth + 1));
			}
		} else if (v.hasMembers()) {
			for (String key : v.getMemberKeys()) {
				Value mv = v.getMember(key);
				if (mv != null && mv.canExecute()) {
					continue; // on ignore les methodes (objets/modules Python exposent leurs methodes)
				}
				ai.ops(1);
				map.set(ai, key, toJava(mv, ai, depth + 1));
			}
		} else if (v.hasArrayElements()) {
			// Un tableau guest la ou une map est attendue : indices entiers comme cles.
			long n = v.getArraySize();
			for (long i = 0; i < n; i++) {
				ai.ops(1);
				map.set(ai, i, toJava(v.getArrayElement(i), ai, depth + 1));
			}
		}
		return map;
	}

	private static long toLong(Value v) {
		if (v.fitsInLong()) {
			return v.asLong();
		}
		if (v.isNumber()) {
			if (!v.fitsInDouble()) {
				// Entier Python hors plage double : pas de bigint en LeekScript -> on sature.
				return v.fitsInBigInteger() ? clampBigInteger(v.asBigInteger()) : 0L;
			}
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
			if (v.fitsInDouble()) {
				return v.asDouble();
			}
			if (v.fitsInLong()) {
				return (double) v.asLong();
			}
			return v.fitsInBigInteger() ? v.asBigInteger().doubleValue() : 0.0;
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
		// Truthiness facon langage guest : 0 / NaN / "" / null sont faux.
		if (v.isNumber()) {
			double d = v.asDouble();
			return d != 0.0 && !Double.isNaN(d);
		}
		if (v.isString()) {
			return !v.asString().isEmpty();
		}
		return !v.isNull();
	}

	// ----- Java -> guest -----

	/** Conversion Java -&gt; guest pour la valeur de retour d'une fonction de combat. */
	public static Object toGuest(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number || o instanceof Boolean || o instanceof String || o instanceof Character) {
			return o; // GraalVM convertit automatiquement les primitifs hote en valeur guest
		}
		if (o instanceof GenericArrayLeekValue) {
			return new LeekArrayProxy((GenericArrayLeekValue) o);
		}
		if (o instanceof MapLeekValue) {
			return new LeekMapProxy((MapLeekValue) o);
		}
		if (o instanceof SetLeekValue) {
			return new LeekListProxy(new ArrayList<Object>((SetLeekValue) o));
		}
		// Type LeekScript non encore gere : on le laisse passer (opaque sous HostAccess.NONE).
		return o;
	}

	/** Vue paresseuse, lecture seule, d'un tableau de combat ({@link GenericArrayLeekValue}). */
	private static final class LeekArrayProxy implements ProxyArray {
		private final GenericArrayLeekValue array;

		LeekArrayProxy(GenericArrayLeekValue array) {
			this.array = array;
		}

		@Override
		public long getSize() {
			return array.size();
		}

		@Override
		public Object get(long index) {
			try {
				return toGuest(array.get((int) index));
			} catch (LeekRunException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void set(long index, Value value) {
			throw new UnsupportedOperationException("tableau de combat en lecture seule (etape 2)");
		}
	}

	/** Vue paresseuse, lecture seule, d'une liste Java de valeurs LeekScript (ex: SetLeekValue). */
	private static final class LeekListProxy implements ProxyArray {
		private final List<Object> list;

		LeekListProxy(List<Object> list) {
			this.list = list;
		}

		@Override
		public long getSize() {
			return list.size();
		}

		@Override
		public Object get(long index) {
			return toGuest(list.get((int) index));
		}

		@Override
		public void set(long index, Value value) {
			throw new UnsupportedOperationException("collection de combat en lecture seule (etape 2)");
		}
	}

	/** Vue paresseuse, lecture seule, d'une map de combat ({@link MapLeekValue}). Cles -&gt; chaines. */
	private static final class LeekMapProxy implements ProxyObject {
		private final MapLeekValue map;

		LeekMapProxy(MapLeekValue map) {
			this.map = map;
		}

		@Override
		public Object getMember(String key) {
			Object raw = lookup(key);
			return raw == null ? null : toGuest(raw);
		}

		@Override
		public Object getMemberKeys() {
			List<Object> keys = new ArrayList<>(map.size());
			for (Object k : map.keySet()) {
				keys.add(String.valueOf(k));
			}
			return ProxyArray.fromList(keys);
		}

		@Override
		public boolean hasMember(String key) {
			return map.containsKey(key) || asLong(key) != null && map.containsKey(asLong(key));
		}

		@Override
		public void putMember(String key, Value value) {
			throw new UnsupportedOperationException("map de combat en lecture seule (etape 2)");
		}

		// Les cles LeekScript peuvent etre des entiers ; le guest les voit en chaines.
		private Object lookup(String key) {
			if (map.containsKey(key)) {
				return map.get(key);
			}
			Long l = asLong(key);
			return l != null ? map.get(l) : null;
		}

		private static Long asLong(String key) {
			try {
				return Long.parseLong(key);
			} catch (NumberFormatException e) {
				return null;
			}
		}
	}
}
