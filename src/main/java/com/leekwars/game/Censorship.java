package com.leekwars.game;

import java.text.Normalizer;
import java.util.HashSet;

public class Censorship {

	private static HashSet<String> sInsults = null;

	public static boolean isInsult(String word) {
		if (sInsults == null) {
//			sInsults = LeekWars.getDB().loadInsults();
			sInsults = new HashSet<>();
		}

		return sInsults.contains(word.toLowerCase());
	}

	public static String getReplacement(int l) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < l; i++)
			sb.append("*");
		return sb.toString();
	}

	public static char getStrip(char letter) {
		if (letter == 'à' || letter == 'â' || letter == 'ä')
			return 'a';
		if (letter == 'ê' || letter == 'ë' || letter == 'é' || letter == 'è')
			return 'e';
		if (letter == 'ï' || letter == 'î')
			return 'i';
		if (letter == 'ô' || letter == 'ö')
			return 'o';
		if (letter == 'ü' || letter == 'û')
			return 'u';
		return letter;
	}

	public static String checkString(String source) {
		StringBuilder sb = new StringBuilder();
		StringBuilder word = new StringBuilder();
		StringBuilder tmp = new StringBuilder();
		int j = 0;
		try {
			String str = new String(source.getBytes(), "UTF-8");
			String copy = Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").toLowerCase();
			int i = 0;
			for (i = 0; i < copy.length(); i++) {
				char c = copy.charAt(i);
				boolean isletter = (c >= 'a' && c <= 'z');
				if (isletter) {
					word.append(source.charAt(j));
					if (source.charAt(j) == 195)
						word.append(source.charAt(j + 1));
					tmp.append(c);
				}

				if (!isletter || i == source.length() - 1) {
					// On regarde si y'a un mot à vérifier
					if (word.length() > 0) {
						if (isInsult(tmp.toString()))
							sb.append(getReplacement(tmp.length()));
						else
							sb.append(word);
						tmp.setLength(0);
						word.setLength(0);
					}
					if (!isletter) {
						sb.append(source.charAt(j));
						if (source.charAt(j) == 195)
							word.append(source.charAt(j + 1));
					}
				}

				if (source.charAt(j) == 195)
					j += 2;
				else
					j++;
			}
		} catch (Exception e) {

		}
		return sb.toString();
	}
}
