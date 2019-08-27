package com.leekwars.game;

public class Log {

	public static String BLUE = "\033[1;34m";
	public static String YELLOW = "\033[1;33m";
	public static String END_COLOR = "\033[0m";

	private static boolean enabled = false;

	public static void enable(boolean enable) {
		enabled = enable;
	}

	public static void i(String tag, String s) {
		if (enabled)
			System.out.println("[" + Util.BLUE + tag + Util.END_COLOR + "] " + s);
	}
	public static void w(String tag, String s) {
		if (enabled)
			System.out.println("[" + Util.YELLOW + tag + Util.END_COLOR + "] " + s);
	}
	public static void start(String tag, String s) {
		if (enabled)
			System.out.print("[" + Util.BLUE + tag + Util.END_COLOR + "] " + s);
	}
	public static void end(String s) {
		if (enabled)
			System.out.println(s);
	}
}
