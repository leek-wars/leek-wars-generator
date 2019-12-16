package com.leekwars.generator.fight.turret;

import com.leekwars.generator.fight.entity.Entity;

public class Turret extends Entity {

    @Override
    public int getType() {
        return Entity.TYPE_TURRET;
    }
}