package com.leekwars.generator.effect;

import com.leekwars.generator.state.State;

/**
 * Effet qui agit au début de chaque tour de sa cible : poison, séquelle, soin sur la durée.
 * Sa durée se compte en coups, chez sa cible (Entity.startTurn) : N tours valent N coups,
 * quel que soit le moment où il a été posé — au tour de son lanceur, au réveil d'une plante
 * ou par propagation. Les autres effets comptent leur durée au tour de leur lanceur.
 */
public abstract class PeriodicEffect extends Effect {

	public abstract void applyStartTurn(State state);
}
