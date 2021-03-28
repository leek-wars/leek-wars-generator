package com.leekwars.generator;


import java.util.HashSet;
import java.util.regex.Pattern;

import com.leekwars.generator.fight.Fight;

public class Censorship {

	private static HashSet<String> sSwearWords = new HashSet<>();
	private static final char[] replacements = new char[] { '*', '&', '@', '#', '!' };

	public static void setSwearWords(HashSet<String> swearWords) {
		sSwearWords = swearWords;
	}

	public static String getReplacement(Fight fight, int l) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < l; i++)
			sb.append(replacements[fight.getRandom().getInt(0, replacements.length - 1)]);
		return sb.toString();
	}

	public static String checkString(Fight fight, String source) {
		return Pattern.compile("\\p{L}+").matcher(source).replaceAll(mr -> {
			if (sSwearWords.contains(mr.group().toLowerCase())) {
				return getReplacement(fight, mr.group().length());
			} else {
				return mr.group();
			}
		});
	}
}
