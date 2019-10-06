package com.leekwars.generator.scenario;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.Log;
import com.leekwars.generator.attack.chips.Chips;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.attack.weapons.Weapons;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.leek.Leek;

public class EntityInfo {

    static public final String TAG = EntityInfo.class.getSimpleName();

    static public class Type {
        public static final int LEEK = 1;
        public static final int BULB = 2;
    }

    static private final Class<?> classes[] = { Leek.class };

    public int id;
    public String name;
    public String ai;
    public int type;
    public int farmer;
    public int team;
    public int level;
    public int life;
    public int tp;
    public int mp;
    public int strength;
    public int agility;
    public int frequency;
    public int wisdom;
    public int resistance;
    public int science;
    public int magic;
    public List<Integer> chips;
    public List<Integer> weapons;

    public EntityInfo(JSONObject e) {
        id = e.getIntValue("id");
        name = e.getString("name");
        ai = e.getString("ai");
        farmer = e.getIntValue("farmer");
        team = e.getIntValue("team");
        level = e.getIntValue("level");
        life = e.getIntValue("life");
        tp = e.getIntValue("tp");
        mp = e.getIntValue("mp");
        strength = e.getIntValue("strength");
        agility = e.getIntValue("agility");
        frequency = e.getIntValue("frequency");
        wisdom = e.getIntValue("wisdom");
        resistance = e.getIntValue("resistance");
        science = e.getIntValue("science");
        magic = e.getIntValue("magic");

        JSONArray weapons = e.getJSONArray("weapons");
        if (weapons != null) {
            for (Object w : weapons) {
                this.weapons.add((Integer) w);
            }
        }
        JSONArray chips = e.getJSONArray("chips");
        if (chips != null) {
            for (Object c : chips) {
                this.chips.add((Integer) c);
            }
        }
    }

    public Entity createEntity() {
        try {
            Entity entity = (Entity) classes[type].getDeclaredConstructor().newInstance();

            for (Object w : weapons) {
                Weapon weapon = Weapons.getWeapon((Integer) w);
                if (weapon == null) {
                    Log.e(TAG, "No such weapon: " + w);
                    return null;
                }
                entity.addWeapon(weapon);
            }
            for (Object c : chips) {
                Integer chip = (Integer) c;
                entity.addChip(Chips.getChip(chip));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
	}
}