package com.leekwars.generator.leek;

public interface RegisterManager {
	public String getRegisters(int leek);
	public void saveRegisters(int leek, String registers, boolean is_new);
}