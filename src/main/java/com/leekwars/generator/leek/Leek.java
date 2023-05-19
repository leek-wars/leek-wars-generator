package com.leekwars.generator.leek;

import com.leekwars.generator.state.Entity;

public class Leek extends Entity {

	public Leek() {}

	public Leek(Integer id, String name, int farmer, int level, int life, int turn_point, int move_point, int force, int agility, int frequency, int wisdom, int resistance, int science, int magic, int skin, boolean metal, int face, int team_id, String team_name, int ai_id, String ai_name, String farmer_name, String farmer_country, int hat) {
		super(id, name, farmer, level, life, turn_point, move_point, force, agility, frequency, wisdom, resistance, science, magic, skin, metal, face, team_id, team_name, ai_id, ai_name, farmer_name, farmer_country, hat);
	}

	public Leek(Integer id, String name) {
		super(id, name);
	}

	public Leek(Leek leek) {
		super(leek);
		// super(leek.getId(), leek.name, leek.getFarmer(), leek.getLevel(), leek.getLife(), leek.getTP(), leek.getMP(), leek.getStrength(), leek.getAgility(), leek.getFrequency(), leek.getWisdom(), leek.getResistance(), leek.getScience(), leek.getMagic(), leek.getSkin(), leek.getMetal(), leek.getFace(), leek.getTeamId(), leek.getTeamName(), leek.getAIId(), leek.getAIName(), leek.getFarmerName(), leek.getFarmerCountry(), leek.getHat());
	}

	@Override
	public int getType() {
		return Entity.TYPE_LEEK;
	}

	@Override
	public Leek getLeek() {
		return this;
	}

	@Override
	public boolean isSummon() {
		return false;
	}
}
