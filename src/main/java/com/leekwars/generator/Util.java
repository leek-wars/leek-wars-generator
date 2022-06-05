package com.leekwars.generator;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class Util {

	private static HashSet<Integer> primes = null;
	public static String BLUE = "\033[1;34m";
	public static String GREEN = "\033[1;32m";
	public static String YELLOW = "\033[1;33m";
	public static String RED = "\033[1;31m";
	public static String END_COLOR = "\033[0m";

	public static boolean isPrime(int value) {
		if (primes == null) {
			primes = new HashSet<Integer>();
			int values[] = new int[] { 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691 };
			for (int i = 0; i < values.length; i++)
				primes.add(values[i]);
		}
		return primes.contains(value);
	}

	public static String getHexaColor(long color) {
		String retour = Long.toString(color & 0xFFFFFF, 16);
		while (retour.length() < 6)
			retour = "0" + retour;
		return retour;
	}

	public static String[] jsonArrayToStringArray(JSONArray array) {
		String[] res = new String[array.size()];
		for (int i = 0; i < array.size(); ++i) {
			res[i] = array.getString(i);
		}
		return res;
	}

	public static void save(JSON data, String file) {
		File f = new File(file);
		try {
			PrintWriter out = new PrintWriter(f);
			out.append(data.toJSONString());
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static JSONArray readJSONArray(String file) {
		File f = new File(file);
		if (!f.exists())
			return null;
		try {
			BufferedInputStream reader = new BufferedInputStream(new FileInputStream(f));
			ByteArrayOutputStream datas = new ByteArrayOutputStream();
			byte b[] = new byte[256];
			int len;
			while ((len = reader.read(b)) != -1) {
				datas.write(b, 0, len);
			}
			reader.close();
			return JSON.parseArray(new String(datas.toByteArray()));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String sha1(String data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] sha1_data = md.digest(data.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(2 * sha1_data.length);
			for (byte b : sha1_data) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (Exception e) {}
		return "";
	}

	public static String readFile(String filepath) {
		File file = new File(filepath);
		return readFile(file);
	}

	public static String readFile(File file) {
		try {
			return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String inputStreamToString(InputStream is) {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		try {
			while ((length = is.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			return result.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static class Worker extends Thread {
		private final Process process;
		public Integer exit;
		public Worker(Process process) {
			this.process = process;
		}
		public void run() {
			try {
				exit = process.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void writeFile(String data, String file) {
		File f = new File(file);
		try {
			PrintWriter out = new PrintWriter(f);
			out.append(data);
			out.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
