package com.leekwars.generator.turret;

import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.Entity;

public class Turret extends Entity {

    public Turret() {
        addState(EntityState.STATUE);
    }

    @Override
    public int getType() {
        return Entity.TYPE_TURRET;
    }
}