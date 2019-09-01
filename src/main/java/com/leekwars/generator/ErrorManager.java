package com.leekwars.game;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.entity.Entity;

public class ErrorManager {

	private static final String TAG = ErrorManager.class.getSimpleName();

	public static void exception(Throwable e) {
		Log.i(TAG, e.getMessage());
		Log.i(TAG, traceToString(e));
	}

	public static void exception(Throwable e, int ai) {
		Log.i(TAG, e.getMessage());
		Log.i(TAG, traceToString(e));
	}

	public static void exceptionFight(Throwable e, int fight) {
		Log.i(TAG, e.getMessage());
		Log.i(TAG, traceToString(e));
	}

	public static String traceToString(Throwable throwable) {

		StringBuilder sb = new StringBuilder();
		sb.append(throwable.toString());
		int nb = 0;
		for (StackTraceElement t : throwable.getStackTrace()) {
			nb++;
			if (nb > 20)
				break;
			sb.append("\n\tat ").append(t.getClassName()).append(".").append(t.getMethodName());
			if (!t.isNativeMethod())
				sb.append("(").append(t.getFileName()).append(":").append(t.getLineNumber()).append(")");
		}
		return sb.toString();
	}

	public static String traceToString(StackTraceElement[] trace) {

		StringBuilder sb = new StringBuilder();
		int nb = 0;
		for (StackTraceElement t : trace) {
			nb++;
			if (nb > 20)
				break;
			sb.append("\n\tat ").append(t.getClassName()).append(".").append(t.getMethodName());
			if (!t.isNativeMethod())
				sb.append("(").append(t.getFileName()).append(":").append(t.getLineNumber()).append(")");
		}
		return sb.toString();
	}

	/*
	 * OLD
	 */

	public final static int AI_ERROR = 1;
	public final static int COMPILATION_ERROR = 2;
	public final static int FIGHT_ERROR = 3;
	public final static int NULL_ERROR = 4;
	public final static int CRITIC_ERROR = 5;

	public static void registerCompilationError(String ai, String informations) {
		System.out.println("Error compilation " + ai + " : " + informations);
	}

	public static void registerAIError(Fight fight, Entity leek, String trace, Throwable throwable, int type) {
		String informations = (leek != null ? (leek.getName() + " (" + leek.getId() + ") : ") : "") + trace;
		registerError(fight, type, throwable, informations);
	}

	public static void registerAIError(Fight fight, Entity leek, String trace, Throwable throwable) {
		registerAIError(fight, leek, trace, throwable, AI_ERROR);
	}

	public static void registerError(Fight fight, Throwable throwable, int type) {
		registerError(fight, type, throwable, "");
	}

	private static void registerError(Fight f, int type, Throwable throwable, String complement) {
		
	}

}
