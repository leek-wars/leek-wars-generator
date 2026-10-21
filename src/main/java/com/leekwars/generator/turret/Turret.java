package com.leekwars.generator.turret;

import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.Entity;

public class Turret extends Entity {

    public Turret() {}

    @Override
    public void startFight() {
        applyPermanentStates();
    }

    @Override
    public void applyPermanentStates() {
        addPermanentState(EntityState.STATIC);
    }

    @Override
    public int getType() {
        return Entity.TYPE_TURRET;
    }
}