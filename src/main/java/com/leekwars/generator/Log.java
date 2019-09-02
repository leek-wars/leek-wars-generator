package com.leekwars.generator;

public class Log {

	private static boolean enabled = false;

	public static void enable(boolean enable) {
		enabled = enable;
	}

	public static void i(String tag, String s) {
		if (enabled)
			System.out.println("[" + Util.BLUE + tag + Util.END_COLOR + "] " + s);
	}
	public static void s(String tag, String s) {
		if (enabled)
			System.out.println("[" + Util.GREEN + tag + Util.END_COLOR + "] " + s);
	}
	public static void w(String tag, String s) {
		if (enabled)
			System.out.println("[" + Util.YELLOW + tag + Util.END_COLOR + "] " + s);
	}
	public static void e(String tag, String s) {
		if (enabled)
			System.out.println("[" + Util.RED + tag + Util.END_COLOR + "] " + s);
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
