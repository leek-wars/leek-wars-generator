package com.leekwars.generator;

import leekscript.compiler.AIFile;

public interface ErrorManager {

    public void exception(Throwable e, int fightID);
    public void exception(Throwable e, int fightID, int farmer, AIFile file);
}
