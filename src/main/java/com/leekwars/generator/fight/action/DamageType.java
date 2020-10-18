package com.leekwars.generator.fight.action;

public enum DamageType {
    DIRECT(101),
	NOVA(107),
	RETURN(108),
    LIFE(109),
    POISON(110),
    AFTEREFFECT(110);

    public int value;
    DamageType(int v) {
        this.value = v;
    }
}
