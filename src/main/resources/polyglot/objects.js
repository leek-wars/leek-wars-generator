// API de combat ORIENTÉE OBJET pour les IA polyglot JS/TS (style stdlib objet LeekScript v5).
// Couche guest au-dessus de l'API plate déjà bridgée : `me.useWeapon(enemy)` -> `useWeapon(enemy.id)`.
// Injectée dans chaque contexte après le bridge (cf PolyglotEntityAI). Propriétés = lecture d'état,
// méthodes = actions/calculs. L'API plate reste valide (coexistence) ; l'objet est la forme idiomatique.
(function () {
	'use strict';

	function cid(x) {
		if (x instanceof Cell) return x.id;
		if (x instanceof Entity) return getCell(x.id);
		return x;
	}
	function eid(x) { return (x instanceof Entity) ? x.id : x; }
	function wid(x) { return (x instanceof Weapon) ? x.id : x; }
	function cpid(x) { return (x instanceof Chip) ? x.id : x; }
	function ent(id) { return (id === null || id === undefined || id < 0) ? null : new Entity(id); }
	function ents(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(new Entity(ids[i])); return o; }
	function cells(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(new Cell(ids[i])); return o; }
	function weap(id) { return (id === null || id === undefined || id <= 0) ? null : new Weapon(id); }
	function weaps(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(new Weapon(ids[i])); return o; }
	function chps(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(new Chip(ids[i])); return o; }
	function cidList(x) {
		if (Array.isArray(x)) { var o = []; for (var i = 0; i < x.length; i++) o.push(cid(x[i])); return o; }
		return cid(x);
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

	// ---- Weapon : une arme (stats) ----
	class Weapon {
		constructor(id) { this.id = id; }
		get cost() { return getWeaponCost(this.id); }
		get minRange() { return getWeaponMinRange(this.id); }
		get maxRange() { return getWeaponMaxRange(this.id); }
		get minScope() { return getWeaponMinScope(this.id); }
		get maxScope() { return getWeaponMaxScope(this.id); }
		get name() { return getWeaponName(this.id); }
		get area() { return getWeaponArea(this.id); }
		get launchType() { return getWeaponLaunchType(this.id); }
		get maxUses() { return getWeaponMaxUses(this.id); }
		get inline() { return isInlineWeapon(this.id); }
		needLos() { return weaponNeedLos(this.id); }
		effects() { return feats(getWeaponEffects(this.id)); }
	}

	// ---- Chip : une puce (stats) ----
	class Chip {
		constructor(id) { this.id = id; }
		get cost() { return getChipCost(this.id); }
		get cooldown() { return getChipCooldown(this.id); }
		get currentCooldown() { return getCurrentCooldown(this.id); }
		get minRange() { return getChipMinRange(this.id); }
		get maxRange() { return getChipMaxRange(this.id); }
		get minScope() { return getChipMinScope(this.id); }
		get maxScope() { return getChipMaxScope(this.id); }
		get name() { return getChipName(this.id); }
		get area() { return getChipArea(this.id); }
		get launchType() { return getChipLaunchType(this.id); }
		get maxUses() { return getChipMaxUses(this.id); }
		get inline() { return isInlineChip(this.id); }
		needLos() { return chipNeedLos(this.id); }
		effects() { return feats(getChipEffects(this.id)); }
	}

	// ---- Effect : un effet ACTIF/lancé sur une entité (vue nommée du tableau brut) ----
	// Tableau brut = [type, value, caster, turns, critical, item, target, modifiers].
	class Effect {
		constructor(raw) { this.raw = raw; }
		get type() { return this.raw[0]; }
		get value() { return this.raw[1]; }
		get caster() { return ent(this.raw[2]); }
		get turns() { return this.raw[3]; }
		get critical() { return this.raw[4]; }
		get item() { return this.raw[5]; }
		get target() { return ent(this.raw[6]); }
		get modifiers() { return this.raw[7]; }
	}

	// ---- EffectTemplate : un effet DÉCLARÉ par une arme/puce ou un effet passif (potentiel, pas encore appliqué) ----
	// Tableau brut = [type, minValue, maxValue, turns, targets, modifiers].
	class EffectTemplate {
		constructor(raw) { this.raw = raw; }
		get type() { return this.raw[0]; }
		get minValue() { return this.raw[1]; }
		get maxValue() { return this.raw[2]; }
		get turns() { return this.raw[3]; }
		get targets() { return this.raw[4]; }
		get modifiers() { return this.raw[5]; }
	}

	function effs(arr) { var o = []; if (arr) for (var i = 0; i < arr.length; i++) o.push(new Effect(arr[i])); return o; }
	function feats(arr) { var o = []; if (arr) for (var i = 0; i < arr.length; i++) o.push(new EffectTemplate(arr[i])); return o; }

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
		get weapon() { return weap(getWeapon(this.id)); }
		get weapons() { return weaps(getWeapons(this.id)); }
		get chips() { return chps(getChips(this.id)); }
		get effects() { return effs(getEffects(this.id)); }
		get launchedEffects() { return effs(getLaunchedEffects(this.id)); }
		get passiveEffects() { return feats(getPassiveEffects(this.id)); }
		get states() { return getStates(this.id); }
		get summons() { return ents(getSummons(this.id)); }
		get summoner() { return ent(getSummoner(this.id)); }
		get summoned() { return isSummon(this.id); }
		get alive() { return isAlive(this.id); }
		get dead() { return isDead(this.id); }
		isAlly() { return isAlly(this.id); }
		isEnemy() { return isEnemy(this.id); }
		distance(target) { return getCellDistance(getCell(this.id), cid(target)); }
	}

	// ---- me : l'IA courante (Entity + actions) ----
	class Me extends Entity {
		constructor() {
			super(-1); // id jetable : remplace juste apres par un accessor dynamique
			// me suit l'entite COURANTE de l'IA. Pendant le tour d'un bulbe (summon), BulbAI rebascule
			// mEntity de l'IA invocatrice sur le bulbe -> me.id doit renvoyer le bulbe, pas l'invocateur.
			// Setter no-op OBLIGATOIRE : 'use strict' + le constructeur Entity fait this.id = id (leverait
			// un TypeError sur un accessor sans setter). Les autres Entity gardent un id fige (non impactees).
			Object.defineProperty(this, 'id', {
				get: function () { return getEntity(); },
				set: function () {},
				configurable: true,
				enumerable: true,
			});
		}
		moveToward(target) { return (target instanceof Cell) ? moveTowardCell(target.id) : moveToward(eid(target)); }
		moveAwayFrom(target) { return (target instanceof Cell) ? moveAwayFromCell(target.id) : moveAwayFrom(eid(target)); }
		useWeapon(target) { return useWeapon(eid(target)); }
		useWeaponOnCell(cell) { return useWeaponOnCell(cid(cell)); }
		useChip(chip, target) { return useChip(cpid(chip), eid(target)); }
		useChipOnCell(chip, cell) { return useChipOnCell(cpid(chip), cid(cell)); }
		setWeapon(weapon) { return setWeapon(wid(weapon)); }
		say(message) { return say(message); }
		canUseWeapon(target) { return canUseWeapon(eid(target)); }
		canUseChip(chip, target) { return canUseChip(cpid(chip), eid(target)); }
		resurrect(target, cell) { return resurrect(eid(target), cid(cell)); }
		// Invoque un bulbe : callback = fonction guest rejouee a chaque tour du bulbe (pendant laquelle
		// me/getEntity() designent le bulbe). cf TypeMarshaller.wrapGuestFunction + BulbAI.
		summon(chip, cell, callback, name) {
			return (name === undefined) ? summon(cpid(chip), cid(cell), callback) : summon(cpid(chip), cid(cell), callback, name);
		}
	}

	// ---- Fight : entités et état global du combat ----
	var Fight = {
		get turn() { return getTurn(); },
		getNearestEnemy: function () { return ent(getNearestEnemy()); },
		getNearestAlly: function () { return ent(getNearestAlly()); },
		getFarthestEnemy: function () { return ent(getFarthestEnemy()); },
		getFarthestAlly: function () { return ent(getFarthestAlly()); },
		getNearestEnemyTo: function (target) { return ent(getNearestEnemyTo(eid(target))); },
		getNearestAllyTo: function (target) { return ent(getNearestAllyTo(eid(target))); },
		getEnemies: function () { return ents(getEnemies()); },
		getAllies: function () { return ents(getAllies()); },
		getAliveEnemies: function () { return ents(getAliveEnemies()); },
		getAliveAllies: function () { return ents(getAliveAllies()); },
		getDeadEnemies: function () { return ents(getDeadEnemies()); },
		getDeadAllies: function () { return ents(getDeadAllies()); },
		getEnemiesCount: function () { return getEnemiesCount(); },
		getAlliesCount: function () { return getAlliesCount(); },
		getAliveEnemiesCount: function () { return getAliveEnemiesCount(); },
		getAliveAlliesCount: function () { return getAliveAlliesCount(); },
		getAlliedTurret: function () { return ent(getAlliedTurret()); },
		getEnemyTurret: function () { return ent(getEnemyTurret()); },
	};

	// ---- Field : terrain et géométrie ----
	var Field = {
		get mapType() { return getMapType(); },
		cellFromXY: function (x, y) { var c = getCellFromXY(x, y); return (c === null || c === undefined || c < 0) ? null : new Cell(c); },
		getObstacles: function () { return cells(getObstacles()); },
		distance: function (a, b) { return getDistance(cid(a), cid(b)); },
		cellDistance: function (a, b) { return getCellDistance(cid(a), cid(b)); },
		pathLength: function (a, b) { return getPathLength(cid(a), cid(b)); },
		lineOfSight: function (a, b) { return lineOfSight(cid(a), cid(b)); },
	};

	// ---- Registers : stockage persistant de l'IA (clé -> valeur, entre combats) ----
	var Registers = {
		get: function (key) { return getRegister(key); },
		set: function (key, value) { return setRegister(key, value); },
		delete: function (key) { return deleteRegister(key); },
		all: function () { return getRegisters(); },
	};

	// ---- Debug : marquage et visualisation du terrain (aide au développement d'IA) ----
	var Debug = {
		mark: function (cells, color, duration) {
			if (color === undefined) return mark(cidList(cells));
			if (duration === undefined) return mark(cidList(cells), color);
			return mark(cidList(cells), color, duration);
		},
		markText: function (cells, text, color, duration) {
			if (color === undefined) return markText(cidList(cells), text);
			if (duration === undefined) return markText(cidList(cells), text, color);
			return markText(cidList(cells), text, color, duration);
		},
		clearMarks: function () { return clearMarks(); },
		show: function (cell, color) { return (color === undefined) ? show(cid(cell)) : show(cid(cell), color); },
		pause: function () { return pause(); },
	};

	globalThis.Cell = Cell;
	globalThis.Entity = Entity;
	globalThis.Weapon = Weapon;
	globalThis.Chip = Chip;
	globalThis.Effect = Effect;
	globalThis.EffectTemplate = EffectTemplate;
	globalThis.me = new Me();
	globalThis.Fight = Fight;
	globalThis.Field = Field;
	globalThis.Registers = Registers;
	globalThis.Debug = Debug;
})();
