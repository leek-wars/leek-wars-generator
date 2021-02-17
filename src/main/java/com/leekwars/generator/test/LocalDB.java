package com.leekwars.generator.test;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LocalDB {

	private static Connection db;

	static {
		try {
			// long t = System.currentTimeMillis();
			Class.forName("org.postgresql.Driver").getDeclaredConstructor().newInstance();
			db = DriverManager.getConnection("jdbc:postgresql://localhost:5432/leekwars", "leekwars", "local");
			// System.out.println("Connection OK");
			// System.out.println("connect time = " + (System.currentTimeMillis() - t));
		} catch (SQLException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static Connection getDB() {
		return db;
	}
}
