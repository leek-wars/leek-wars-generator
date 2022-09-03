package com.leekwars.generator;

public interface ErrorManager {

    public void exception(Throwable e, int fightID);
    public void exception(Throwable e, int fightID, int farmer, int aiID, int aiVersion);
}
