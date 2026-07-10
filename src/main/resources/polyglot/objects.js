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
	// Fabrique l'instance TYPÉE d'une entité selon son type (getType). Les classes sont déclarées
	// plus bas mais ent() n'est appelée qu'au runtime (après chargement du module) -> OK.
	function ent(id) {
		if (id === null || id === undefined || id < 0) return null;
		switch (getType(id)) {
			case ENTITY_LEEK: return new Leek(id);
			case ENTITY_BULB: return new Bulb(id);
			case ENTITY_TURRET: return new Turret(id);
			case ENTITY_CHEST: return new Chest(id);
			case ENTITY_MOB: return new Mob(id);
			default: return new Entity(id);
		}
	}
	function ents(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) { var e = ent(ids[i]); if (e) o.push(e); } return o; }
	function cells(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(new Cell(ids[i])); return o; }
	// Pool de singletons par id pour Weapon/Chip : une seule instance par id dans un contexte.
	// GARANTIT que les valeurs renvoyees par l'API (me.weapon, entity.weapons...) et les constantes
	// objet (Weapon.pistol) sont LE MEME objet -> comparables par reference (me.weapon === Weapon.pistol).
	// Sans ca, deux `new Weapon(id)` seraient !== et la comparaison serait toujours fausse.
	var weaponPool = {};
	var chipPool = {};
	function weap(id) { return (id === null || id === undefined || id <= 0) ? null : (weaponPool[id] || (weaponPool[id] = new Weapon(id))); }
	function chp(id) { return (id === null || id === undefined || id <= 0) ? null : (chipPool[id] || (chipPool[id] = new Chip(id))); }
	function weaps(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(weap(ids[i])); return o; }
	function chps(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(chp(ids[i])); return o; }
	function cidList(x) {
		if (Array.isArray(x)) { var o = []; for (var i = 0; i < x.length; i++) o.push(cid(x[i])); return o; }
		return cid(x);
	}
	function cell(id) { return (id === null || id === undefined || id < 0) ? null : new Cell(id); }
	// Déballe UN argument vers son id brut (Entity/Weapon/Chip/Cell -> .id ; tableau -> déballé ;
	// sinon inchangé). Sert aux helpers de ciblage (getCellToUseWeapon...) pour accepter des objets
	// quel que soit l'ordre des arguments, sans avoir à connaître leur rôle.
	function unwrap(x) {
		if (x instanceof Entity || x instanceof Weapon || x instanceof Chip || x instanceof Cell) return x.id;
		if (Array.isArray(x)) { var o = []; for (var i = 0; i < x.length; i++) o.push(unwrap(x[i])); return o; }
		return x;
	}
	function unwrapAll(args) { var o = []; for (var i = 0; i < args.length; i++) o.push(unwrap(args[i])); return o; }

	// ---- Cell : une case du terrain ----
	class Cell {
		constructor(id) { this.id = id; }
		get x() { return getCellX(this.id); }
		get y() { return getCellY(this.id); }
		get empty() { return isEmptyCell(this.id); }
		get obstacle() { return isObstacle(this.id); }
		get entity() { return ent(getEntityOnCell(this.id)); }
		// Contenu de la case (CELL_EMPTY/PLAYER/ENTITY/OBSTACLE = Cell.Type.*).
		get content() { return getCellContent(this.id); }
		distance(target) { return getCellDistance(this.id, cid(target)); }
		pathLength(target) { return getPathLength(this.id, cid(target)); }
		lineOfSight(target) { return lineOfSight(this.id, cid(target)); }
		// Chemin (liste de cellules) jusqu'à la cible, en évitant `ignored`. Retour Cell[].
		path(target, ignored) { return cells(ignored === undefined ? getPath(this.id, cid(target)) : getPath(this.id, cid(target), cidList(ignored))); }
	}

	// ---- Item : base commune aux armes et puces. Porte les constantes partagees
	// (Item.LaunchType, Item.Area). Les getters restent dans les sous-classes : ils tapent des
	// fonctions plates distinctes (getWeapon* vs getChip*), donc pas factorisables sans dispatch. ----
	class Item {
		constructor(id) { this.id = id; }
	}

	// ---- Weapon : une arme (stats) ----
	class Weapon extends Item {
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
		get needsLos() { return weaponNeedLos(this.id); }
		// features : caracteristiques declarees de l'arme (Feature[] : degats, poison, teleport...).
		// Distinct de entity.effects (Effect ACTIFS sur une entite). Property (lecture d'etat).
		get features() { return feats(getWeaponEffects(this.id)); }
	}

	// ---- Chip : une puce (stats) ----
	class Chip extends Item {
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
		get needsLos() { return chipNeedLos(this.id); }
		// features : caracteristiques declarees de la puce (Feature[]). cf Weapon.features.
		get features() { return feats(getChipEffects(this.id)); }
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

	// ---- Feature : une CARACTÉRISTIQUE déclarée par une arme/puce (ou un effet passif) : ce que
	// l'item PEUT faire quand il touche (dégâts, poison, téléport, inversion...). Potentiel, pas
	// encore appliqué -> à distinguer d'Effect (actif sur une entité). Brut = [type, minValue,
	// maxValue, turns, targets, modifiers]. ----
	class Feature {
		constructor(raw) { this.raw = raw; }
		get type() { return this.raw[0]; }
		get minValue() { return this.raw[1]; }
		get maxValue() { return this.raw[2]; }
		get turns() { return this.raw[3]; }
		get targets() { return this.raw[4]; }
		get modifiers() { return this.raw[5]; }
	}

	function effs(arr) { var o = []; if (arr) for (var i = 0; i < arr.length; i++) o.push(new Effect(arr[i])); return o; }
	function feats(arr) { var o = []; if (arr) for (var i = 0; i < arr.length; i++) o.push(new Feature(arr[i])); return o; }

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

	// ---- Sous-types d'entité : ent() (donc getNearestEnemy, getEnemies...) renvoie l'instance
	// TYPÉE selon getType() -> instanceof Mob/Chest/Bulb/Leek/Turret fonctionne. Ceux qui ont une
	// sous-catégorie exposent leur propre .type (chest.type === Chest.Type.WOOD). ----
	class Leek extends Entity {}
	class Turret extends Entity {}
	class Bulb extends Entity {
		get type() { return getBulbType(this.id); }
	}
	class Chest extends Entity {
		get type() { return getChestType(this.id); }
	}
	class Mob extends Entity {
		get type() { return getMobType(this.id); }
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
		// Cellule (ou toutes les cellules) d'où utiliser l'arme/puce sur `target` — une entité OU une
		// case (routage automatique). Retour Cell / Cell[]. Args déballés (objets ou ids, ordre libre).
		weaponCell(target) { return cell(((target instanceof Cell) ? getCellToUseWeaponOnCell : getCellToUseWeapon).apply(null, unwrapAll(arguments))); }
		weaponCells(target) { return cells(((target instanceof Cell) ? getCellsToUseWeaponOnCell : getCellsToUseWeapon).apply(null, unwrapAll(arguments))); }
		chipCell(chip, target) { return cell(((target instanceof Cell) ? getCellToUseChipOnCell : getCellToUseChip).apply(null, unwrapAll(arguments))); }
		chipCells(chip, target) { return cells(((target instanceof Cell) ? getCellsToUseChipOnCell : getCellsToUseChip).apply(null, unwrapAll(arguments))); }
		// Entités touchées par une arme/puce lancée sur une cellule. Retour Entity[].
		weaponTargets() { return ents(getWeaponTargets.apply(null, unwrapAll(arguments))); }
		chipTargets() { return ents(getChipTargets.apply(null, unwrapAll(arguments))); }
	}

	// Instance unique de l'IA courante (Me), exposée via Fight.me. Une seule instance suffit : son id
	// est un accessor dynamique qui suit l'entité courante (cf classe Me).
	var meSelf = new Me();

	// ---- Fight : entités et état global du combat ----
	var Fight = {
		// L'IA courante (votre entité). Remplace l'ancien global `me` : var me = Fight.me
		get me() { return meSelf; },
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
		// Entité alliée/ennemie la plus proche d'une CELLULE (complète getNearestEnemyTo qui prend une entité).
		getNearestEnemyToCell: function (c) { return ent(getNearestEnemyToCell(cid(c))); },
		getNearestAllyToCell: function (c) { return ent(getNearestAllyToCell(cid(c))); },
	};

	// ---- Field : terrain et géométrie ----
	var Field = {
		get type() { return getMapType(); },
		cellFromXY: function (x, y) { var c = getCellFromXY(x, y); return (c === null || c === undefined || c < 0) ? null : new Cell(c); },
		getObstacles: function () { return cells(getObstacles()); },
		distance: function (a, b) { return getDistance(cid(a), cid(b)); },
		cellDistance: function (a, b) { return getCellDistance(cid(a), cid(b)); },
		pathLength: function (a, b) { return getPathLength(cid(a), cid(b)); },
		lineOfSight: function (a, b) { return lineOfSight(cid(a), cid(b)); },
		// Chemin (liste de cellules) de a à b, en évitant `ignored`. Retour Cell[].
		path: function (a, b, ignored) { return cells(ignored === undefined ? getPath(cid(a), cid(b)) : getPath(cid(a), cid(b), cidList(ignored))); },
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

	// Conteneur des constantes d'état spécial (State.PACIFIST...). Objet pur (pas d'instances).
	var State = {};

	// Range les constantes plates du bridge (WEAPON_*, EFFECT_*, STATE_*...) en membres OBJET, par
	// famille. Deux natures : les ITEMS (WEAPON_/CHIP_) donnent des INSTANCES poolées en camelCase
	// (Weapon.pistol), tout le reste donne des CATÉGORIES en MAJUSCULES (Effect.SHIELD, State.PACIFIST,
	// Fight.Type.SOLO...). Les globales plates restent disponibles (API plate inchangée). Les préfixes
	// composés (LAUNCH_TYPE_, FIGHT_TYPE_...) sont listés AVANT les simples pour matcher en premier.
	(function attachConstants() {
		function camel(s) { return s.toLowerCase().replace(/_([a-z0-9])/g, function (_m, c) { return c.toUpperCase(); }); }
		// {p: préfixe, fn: attache custom} OU {p, c: conteneur, s?: sous-conteneur}. Nom = clé après préfixe.
		var RULES = [
			{ p: 'WEAPON_', fn: function (n, v) { Weapon[camel(n)] = weap(v); } },
			{ p: 'CHIP_', fn: function (n, v) { Chip[camel(n)] = chp(v); } },
			{ p: 'LAUNCH_TYPE_', c: Item, s: 'LaunchType' },
			{ p: 'FIGHT_TYPE_', c: Fight, s: 'Type' },
			{ p: 'FIGHT_CONTEXT_', c: Fight, s: 'Context' },
			{ p: 'AREA_', c: Item, s: 'Area' },
			{ p: 'STAT_', c: Entity, s: 'Stat' },
			{ p: 'ENTITY_', c: Entity, s: 'Type' },
			{ p: 'CELL_', c: Cell, s: 'Type' },
			{ p: 'CHEST_', c: Chest, s: 'Type' },
			{ p: 'BULB_', c: Bulb, s: 'Type' },
			{ p: 'MOB_', c: Mob, s: 'Type' },
			{ p: 'BOSS_', c: Fight, s: 'Boss' },
			{ p: 'EROSION_', c: Fight, s: 'Erosion' },
			{ p: 'USE_', c: Fight, s: 'Use' },
			{ p: 'MESSAGE_', c: Fight, s: 'Message' },
			{ p: 'MAP_', c: Field },
			{ p: 'EFFECT_', c: Effect },
			{ p: 'STATE_', c: State },
		];
		function attach(r, name, val) {
			if (r.fn) { r.fn(name, val); return; }
			var box = r.c;
			// hasOwnProperty (pas `!box[r.s]`) : Bulb extends Entity -> Bulb.Type HÉRITERAIT
			// Entity.Type ; sans ça on polluerait Entity.Type au lieu de créer un Bulb.Type propre.
			if (r.s) { if (!Object.prototype.hasOwnProperty.call(box, r.s)) box[r.s] = {}; box = box[r.s]; }
			box[name] = val;
		}
		var names = Object.getOwnPropertyNames(globalThis);
		for (var i = 0; i < names.length; i++) {
			var k = names[i];
			for (var j = 0; j < RULES.length; j++) {
				if (k.indexOf(RULES[j].p) === 0) {
					try { attach(RULES[j], k.slice(RULES[j].p.length), globalThis[k]); } catch (e) { /* nom réservé : on saute */ }
					break;
				}
			}
		}
	})();

	globalThis.Cell = Cell;
	globalThis.Entity = Entity;
	globalThis.Leek = Leek;
	globalThis.Turret = Turret;
	globalThis.Bulb = Bulb;
	globalThis.Chest = Chest;
	globalThis.Mob = Mob;
	globalThis.Item = Item;
	globalThis.Weapon = Weapon;
	globalThis.Chip = Chip;
	globalThis.Effect = Effect;
	globalThis.Feature = Feature;
	globalThis.State = State;
	globalThis.Fight = Fight;
	globalThis.Field = Field;
	globalThis.Registers = Registers;
	globalThis.Debug = Debug;
})();
