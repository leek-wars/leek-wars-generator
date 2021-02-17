package com.leekwars.generator.test;

import java.sql.ResultSet;

import com.leekwars.generator.Log;
import com.leekwars.generator.leek.RegisterManager;

public class LocalDbRegisterManager implements RegisterManager {

	private static final String TAG = LocalDbRegisterManager.class.getSimpleName();

	@Override
	public String getRegisters(int leek) {
		Log.i(TAG, "getRegisters " + leek);
		try {
			var statement = LocalDB.getDB().prepareStatement("SELECT values FROM register WHERE leek = ?");
			statement.setInt(1, leek);
			statement.execute();
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				return null;
			}
			return rs.getString("values");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void saveRegisters(int leek, String registers, boolean is_new) {
		// On sauvegarde pas
	}
}
