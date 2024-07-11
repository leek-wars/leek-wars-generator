package com.leekwars.generator.turret;

import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.state.Entity;

public class Turret extends Entity {

    public Turret() {}

    @Override
    public void startFight() {
        // Add static state
        Effect.createEffect(this.state, Effect.TYPE_ADD_STATE, -1, 1, EntityState.STATIC.ordinal(), 0, false, this, this, null, 0, false, 0, 1, 0, Effect.MODIFIER_IRREDUCTIBLE);
    }

    @Override
    public int getType() {
        return Entity.TYPE_TURRET;
    }
}