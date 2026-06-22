// API de combat ORIENTÉE OBJET pour les IA polyglot JS/TS (style stdlib objet LeekScript v5).
// Couche guest au-dessus de l'API plate déjà bridgée : `me.useWeapon(enemy)` -> `useWeapon(enemy.id)`.
// Injectée dans chaque contexte après le bridge (cf PolyglotEntityAI). Propriétés = lecture d'état,
// méthodes = actions/calculs. Tranche 1 : Cell, Entity, me + accès minimal aux entités (Fight.*).
// L'API plate reste valide (coexistence) ; l'objet deviendra la forme idiomatique.
(function () {
	'use strict';

	// id de cellule depuis une Cell, une Entity (sa case) ou un nombre brut.
	function cid(x) {
		if (x instanceof Cell) return x.id;
		if (x instanceof Entity) return getCell(x.id);
		return x;
	}
	// id d'entité depuis une Entity ou un nombre brut.
	function eid(x) {
		return (x instanceof Entity) ? x.id : x;
	}
	// enveloppe un id d'entité en Entity (null si absent/-1).
	function ent(id) {
		return (id === null || id === undefined || id < 0) ? null : new Entity(id);
	}
	// enveloppe un tableau d'ids (ProxyArray du bridge) en tableau d'Entity.
	function ents(ids) {
		var out = [];
		if (ids) for (var i = 0; i < ids.length; i++) out.push(new Entity(ids[i]));
		return out;
	}

	// ---- Cell : une case du terrain ----
	class Cell {
		constructor(id) { this.id = id; }
		get x() { return getCellX(this.id); }
		get y() { return getCellY(this.id); }
		get empty() { return isEmptyCell(this.id); }
		get obstacle() { return isObstacle(this.id); }
		get entity() { return ent(getEntityOnCell(this.id)); }
		distance(target) { return getCellDistance(this.id, cid(target)); }
		pathLength(target) { return getPathLength(this.id, cid(target)); }
		lineOfSight(target) { return lineOfSight(this.id, cid(target)); }
	}

	// ---- Entity : n'importe quelle entité (lecture d'état) ----
	class Entity {
		constructor(id) { this.id = id; }
		get life() { return getLife(this.id); }
		get maxLife() { return getTotalLife(this.id); }
		get tp() { return getTP(this.id); }
		get maxTP() { return getTotalTP(this.id); }
		get mp() { return getMP(this.id); }
		get maxMP() { return getTotalMP(this.id); }
		get strength() { return getStrength(this.id); }
		get agility() { return getAgility(this.id); }
		get wisdom() { return getWisdom(this.id); }
		get resistance() { return getResistance(this.id); }
		get science() { return getScience(this.id); }
		get magic() { return getMagic(this.id); }
		get power() { return getPower(this.id); }
		get level() { return getLevel(this.id); }
		get name() { return getName(this.id); }
		get absoluteShield() { return getAbsoluteShield(this.id); }
		get relativeShield() { return getRelativeShield(this.id); }
		get cell() { return new Cell(getCell(this.id)); }
		get weapon() { return getWeapon(this.id); } // id d'arme (objet Weapon en tranche 2)
		get alive() { return isAlive(this.id); }
		get dead() { return isDead(this.id); }
		isAlly() { return isAlly(this.id); }
		isEnemy() { return isEnemy(this.id); }
		distance(target) { return getCellDistance(getCell(this.id), cid(target)); }
	}

	// ---- me : l'IA courante (Entity + actions) ----
	class Me extends Entity {
		constructor() { super(getEntity()); }
		moveToward(target) { return (target instanceof Cell) ? moveTowardCell(target.id) : moveToward(eid(target)); }
		moveAwayFrom(target) { return (target instanceof Cell) ? moveAwayFromCell(target.id) : moveAwayFrom(eid(target)); }
		useWeapon(target) { return useWeapon(eid(target)); }
		useWeaponOnCell(cell) { return useWeaponOnCell(cid(cell)); }
		useChip(chip, target) { return useChip(chip, eid(target)); }
		useChipOnCell(chip, cell) { return useChipOnCell(chip, cid(cell)); }
		setWeapon(weapon) { return setWeapon(weapon); }
		say(message) { return say(message); }
		canUseWeapon(target) { return canUseWeapon(eid(target)); }
		canUseChip(chip, target) { return canUseChip(chip, eid(target)); }
	}

	// ---- Fight : accès aux entités (tranche 1 = strict minimum pour utiliser Entity) ----
	var Fight = {
		get turn() { return getTurn(); },
		getNearestEnemy: function () { return ent(getNearestEnemy()); },
		getNearestAlly: function () { return ent(getNearestAlly()); },
		getEnemies: function () { return ents(getEnemies()); },
		getAllies: function () { return ents(getAllies()); },
		getAliveEnemies: function () { return ents(getAliveEnemies()); },
		getAliveAllies: function () { return ents(getAliveAllies()); },
	};

	globalThis.Cell = Cell;
	globalThis.Entity = Entity;
	globalThis.me = new Me();
	globalThis.Fight = Fight;
})();
