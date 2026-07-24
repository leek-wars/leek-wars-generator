// API de combat ORIENTÉE OBJET pour les IA polyglot JS/TS (style stdlib objet LeekScript v5).
// SEULE API exposée au joueur : le sac caché __lw (fonctions + constantes plates du bridge, cf
// PolyglotAPIBridge) est capturé dans des closures locales puis RETIRÉ du scope global -> aucune
// fonction/constante plate n'est visible. Injectée dans chaque contexte après le bridge et les
// gardes (cf PolyglotEntityAI). Propriétés = lecture d'état, méthodes = actions/calculs.
(function () {
	'use strict';

	// ---- Capture du sac d'API hôte, puis suppression du scope global ----
	// F est un objet GUEST : F.getLife est une propriété locale (rapide), là où __lw.getLife serait
	// un readMember interop (aller-retour hôte potentiel) à CHAQUE appel. __lw_names (liste triée,
	// fournie par le bridge) évite de dépendre de l'énumération interop des membres du proxy.
	var G = globalThis.__lw;
	var NAMES = globalThis.__lw_names;
	var names = [];
	var F = {};
	for (var ni = 0; ni < NAMES.length; ni++) {
		var nm = '' + NAMES[ni];
		names.push(nm);
		F[nm] = G[nm];
	}
	try { delete globalThis.__lw; } catch (e) {}
	try { delete globalThis.__lw_names; } catch (e) {}

	function cid(x) {
		if (x instanceof Cell) return x.id;
		if (x instanceof Entity) return F.getCell(x.id);
		return x;
	}
	function eid(x) { return (x instanceof Entity) ? x.id : x; }
	function wid(x) { return (x instanceof Weapon) ? x.id : x; }
	function cpid(x) { return (x instanceof Chip) ? x.id : x; }
	// Tout objet rendu au joueur est GELÉ. Deux raisons. D'abord les enveloppes sont POOLÉES (une
	// instance par id, cf plus bas) : sans gel, un `c.id = 42` du joueur empoisonnerait la cellule
	// partagée pour tout le reste du combat, et chaque getPath()/getObstacles() retombant sur cet id
	// rendrait un objet corrompu, sans le moindre message. Ensuite ça aligne le runtime sur ce que les
	// déclarations annoncent déjà (readonly sur CHAQUE membre du .d.ts / du .pyi).
	// Gel SUPERFICIEL, ce qui suffit : les objets poolés (Cell/Entity/Weapon/Chip) ne portent qu'un id,
	// et le `raw` d'un Effect/Feature/Message reste mutable mais n'est jamais partagé.
	// Le gel se fait dans les FABRIQUES, jamais dans un constructeur : Me redéfinit son `id` en accessor
	// APRÈS super() (cf classe Me), ce qu'un objet gelé interdirait.
	function frozen(o) { return Object.freeze(o); }
	// Pools de singletons par id. Historiquement sur Weapon/Chip seulement ; étendus à Cell et Entity.
	// GARANTIT que les valeurs renvoyees par l'API (me.weapon, entity.weapons, me.cell, getEnemies...)
	// et les constantes objet (Weapon.pistol) sont LE MEME objet -> comparables par reference
	// (me.weapon === Weapon.pistol, path[0] === me.cell). Sans ca, deux `new Weapon(id)` seraient !==
	// et la comparaison serait toujours fausse.
	// Sur Entity le pool economise en plus un getType() (15 operations) par emballage : le type d'une
	// entite ne change jamais du combat, donc un seul suffit, la ou getEnemies() en payait un PAR
	// ennemi et PAR appel. Sur Cell il economise l'allocation en rafale de getPath()/getObstacles().
	var weaponPool = {};
	var chipPool = {};
	var cellPool = {};
	var entityPool = {};
	// Id d'item (arme OU puce) -> instance, remplie au boot par attachConstants (qui instancie deja les
	// 37 armes et 110 puces). Permet a Effect.item de rendre l'objet SANS aucun appel hote : un
	// isWeapon()+isChip() a l'acces couterait 25 operations, dans une boucle sur les effets.
	var itemsById = {};
	// Fabrique l'instance TYPÉE d'une entité selon son type (getType). Les classes sont déclarées
	// plus bas mais ent() n'est appelée qu'au runtime (après chargement du module) -> OK.
	function ent(id) {
		if (id === null || id === undefined || id < 0) return null;
		var e = entityPool[id];
		if (e) return e;
		switch (F.getType(id)) {
			case F.ENTITY_LEEK: e = new Leek(id); break;
			case F.ENTITY_BULB: e = new Bulb(id); break;
			case F.ENTITY_TURRET: e = new Turret(id); break;
			case F.ENTITY_CHEST: e = new Chest(id); break;
			case F.ENTITY_MOB: e = new Mob(id); break;
			default: e = new Entity(id); break;
		}
		return (entityPool[id] = frozen(e));
	}
	function ents(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) { var e = ent(ids[i]); if (e) o.push(e); } return o; }
	// Cellule poolée SANS garde de validité : conserve le comportement historique de `new Cell(id)`,
	// qui rendait un objet même pour un id négatif (cf cells() et Entity.cell). cell() ajoute la garde.
	function pooledCell(id) { return cellPool[id] || (cellPool[id] = frozen(new Cell(id))); }
	function cells(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(pooledCell(ids[i])); return o; }
	function weap(id) { return (id === null || id === undefined || id <= 0) ? null : (weaponPool[id] || (weaponPool[id] = frozen(new Weapon(id)))); }
	function chp(id) { return (id === null || id === undefined || id <= 0) ? null : (chipPool[id] || (chipPool[id] = frozen(new Chip(id)))); }
	function weaps(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(weap(ids[i])); return o; }
	function chps(ids) { var o = []; if (ids) for (var i = 0; i < ids.length; i++) o.push(chp(ids[i])); return o; }
	function cidList(x) {
		if (Array.isArray(x)) { var o = []; for (var i = 0; i < x.length; i++) o.push(cid(x[i])); return o; }
		return cid(x);
	}
	function cell(id) { return (id === null || id === undefined || id < 0) ? null : pooledCell(id); }
	// Déballe UN argument vers son id brut (Entity/Weapon/Chip/Cell -> .id ; tableau -> déballé ;
	// sinon inchangé). Sert aux helpers de ciblage (weaponCell...) pour accepter des objets
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
		get x() { return F.getCellX(this.id); }
		get y() { return F.getCellY(this.id); }
		get empty() { return F.isEmptyCell(this.id); }
		get obstacle() { return F.isObstacle(this.id); }
		get entity() { return ent(F.getEntityOnCell(this.id)); }
		// Une entité (poireau, bulbe, tourelle...) occupe-t-elle la case.
		get hasEntity() { return F.isEntity(this.id); }
		// Contenu de la case (CELL_EMPTY/PLAYER/ENTITY/OBSTACLE = Cell.Type.*).
		get content() { return F.getCellContent(this.id); }
		distance(target) { return F.getCellDistance(this.id, cid(target)); }
		pathLength(target, ignored) { return ignored === undefined ? F.getPathLength(this.id, cid(target)) : F.getPathLength(this.id, cid(target), cidList(ignored)); }
		// Ligne de vue jusqu'à la cible, en ignorant éventuellement des entités (`ignoredEntities`).
		lineOfSight(target, ignoredEntities) { return ignoredEntities === undefined ? F.lineOfSight(this.id, cid(target)) : F.lineOfSight(this.id, cid(target), unwrap(ignoredEntities)); }
		// Chemin (liste de cellules) jusqu'à la cible, en évitant `ignored`. Retour Cell[].
		path(target, ignored) { return cells(ignored === undefined ? F.getPath(this.id, cid(target)) : F.getPath(this.id, cid(target), cidList(ignored))); }
		// La case est-elle alignée (même ligne ou colonne) avec la cible.
		onSameLine(target) { return F.isOnSameLine(this.id, cid(target)); }
		// Cellule d'id `id`, ou null s'il est invalide. L'API ACCEPTE des ids partout (moveToward(210)),
		// il faut donc pouvoir faire le chemin inverse : typiquement relire un id rangé dans un registre.
		static get(id) { return cell(id); }
	}

	// ---- Item : base commune aux armes et puces. Porte les constantes partagees
	// (Item.LaunchType, Item.Area). Les getters restent dans les sous-classes : ils tapent des
	// fonctions plates distinctes (getWeapon* vs getChip*), donc pas factorisables sans dispatch. ----
	class Item {
		constructor(id) { this.id = id; }
		// L'item (arme OU puce) d'id `id`, ou null si l'id n'en designe aucun. Weapon.get/Chip.get
		// restreignent a leur type. Lookup pur (itemsById), aucun appel hote.
		static get(id) { return itemsById[id] || null; }
	}

	// ---- Weapon : une arme (stats) ----
	class Weapon extends Item {
		get cost() { return F.getWeaponCost(this.id); }
		get minRange() { return F.getWeaponMinRange(this.id); }
		get maxRange() { return F.getWeaponMaxRange(this.id); }
		get name() { return F.getWeaponName(this.id); }
		get area() { return F.getWeaponArea(this.id); }
		get launchType() { return F.getWeaponLaunchType(this.id); }
		get maxUses() { return F.getWeaponMaxUses(this.id); }
		get inline() { return F.isInlineWeapon(this.id); }
		get needsLos() { return F.weaponNeedLos(this.id); }
		// Pourcentage d'échec de l'arme.
		get failure() { return F.getWeaponFailure(this.id); }
		// features : caracteristiques declarees de l'arme (Feature[] : degats, poison, teleport...).
		// Distinct de entity.effects (Effect ACTIFS sur une entite). Property (lecture d'etat).
		get features() { return feats(F.getWeaponEffects(this.id)); }
		// Caractéristiques passives de l'arme (bonus quand elle est équipée). Feature[].
		get passiveFeatures() { return feats(F.getWeaponPassiveEffects(this.id)); }
		// Zone d'effet réelle de l'arme sur `cell`, tirée depuis `from` (défaut : position courante). Cell[].
		effectiveArea(cell, from) { return cells(from === undefined ? F.getWeaponEffectiveArea(this.id, cid(cell)) : F.getWeaponEffectiveArea(this.id, cid(cell), cid(from))); }
		// L'arme d'id `id`, ou null si l'id n'est pas celui d'une arme.
		static get(id) { var i = itemsById[id]; return (i instanceof Weapon) ? i : null; }
		// Toutes les armes du jeu. Weapon[].
		static getAll() { return weaps(F.getAllWeapons()); }
		// La valeur est-elle un id d'arme valide.
		static isWeapon(value) { return F.isWeapon(wid(value)); }
	}

	// ---- Chip : une puce (stats) ----
	class Chip extends Item {
		get cost() { return F.getChipCost(this.id); }
		get cooldown() { return F.getChipCooldown(this.id); }
		get currentCooldown() { return F.getCooldown(this.id); }
		get minRange() { return F.getChipMinRange(this.id); }
		get maxRange() { return F.getChipMaxRange(this.id); }
		get minScope() { return F.getChipMinScope(this.id); }
		get maxScope() { return F.getChipMaxScope(this.id); }
		get name() { return F.getChipName(this.id); }
		get area() { return F.getChipArea(this.id); }
		get launchType() { return F.getChipLaunchType(this.id); }
		get maxUses() { return F.getChipMaxUses(this.id); }
		get inline() { return F.isInlineChip(this.id); }
		get needsLos() { return F.chipNeedLos(this.id); }
		// Pourcentage d'échec de la puce.
		get failure() { return F.getChipFailure(this.id); }
		// features : caracteristiques declarees de la puce (Feature[]). cf Weapon.features.
		get features() { return feats(F.getChipEffects(this.id)); }
		// Cooldown restant de la puce pour une AUTRE entité que soi.
		currentCooldownOf(entity) { return F.getCooldown(this.id, eid(entity)); }
		// Pour une puce d'INVOCATION : puces du bulbe invoqué (Chip[]), ses caractéristiques et stats (maps).
		get bulbChips() { return chps(F.getBulbChips(this.id)); }
		get bulbCharacteristics() { return F.getBulbCharacteristics(this.id); }
		get bulbStats() { return F.getBulbStats(this.id); }
		// Zone d'effet réelle de la puce sur `cell`, lancée depuis `from` (défaut : position courante). Cell[].
		effectiveArea(cell, from) { return cells(from === undefined ? F.getChipEffectiveArea(this.id, cid(cell)) : F.getChipEffectiveArea(this.id, cid(cell), cid(from))); }
		// La puce d'id `id`, ou null si l'id n'est pas celui d'une puce.
		static get(id) { var i = itemsById[id]; return (i instanceof Chip) ? i : null; }
		// Toutes les puces du jeu. Chip[].
		static getAll() { return chps(F.getAllChips()); }
		// La valeur est-elle un id de puce valide.
		static isChip(value) { return F.isChip(cpid(value)); }
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
		// Arme ou puce qui a applique l'effet (null si aucune). Instance, comme caster/target sont des
		// Entity : l'id brut reste accessible via raw[5]. Lookup dans itemsById -> AUCUN appel hote.
		get item() { var id = this.raw[5]; return id ? (itemsById[id] || null) : null; }
		get target() { return ent(this.raw[6]); }
		get modifiers() { return this.raw[7]; }
		// Liste des ids de TYPES d'effets existants (Effect.DAMAGE, Effect.HEAL...). number[].
		static getAll() { return F.getAllEffects(); }
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

	function effs(arr) { var o = []; if (arr) for (var i = 0; i < arr.length; i++) o.push(frozen(new Effect(arr[i]))); return o; }
	function feats(arr) { var o = []; if (arr) for (var i = 0; i < arr.length; i++) o.push(frozen(new Feature(arr[i]))); return o; }

	// ---- Entity : n'importe quelle entité (lecture d'état) ----
	class Entity {
		constructor(id) { this.id = id; }
		// Genre d'entité (Entity.Type.LEEK/BULB/TURRET/CHEST/MOB), cf #4634. Ne pas
		// confondre avec .type des sous-classes (sous-variante : Bulb.Type.*, etc.).
		get entityType() { return F.getType(this.id); }
		get life() { return F.getLife(this.id); }
		get maxLife() { return F.getTotalLife(this.id); }
		get tp() { return F.getTP(this.id); }
		get maxTP() { return F.getTotalTP(this.id); }
		get mp() { return F.getMP(this.id); }
		get maxMP() { return F.getTotalMP(this.id); }
		get strength() { return F.getStrength(this.id); }
		get agility() { return F.getAgility(this.id); }
		get wisdom() { return F.getWisdom(this.id); }
		get resistance() { return F.getResistance(this.id); }
		get science() { return F.getScience(this.id); }
		get magic() { return F.getMagic(this.id); }
		get power() { return F.getPower(this.id); }
		get level() { return F.getLevel(this.id); }
		get name() { return F.getName(this.id); }
		get absoluteShield() { return F.getAbsoluteShield(this.id); }
		get relativeShield() { return F.getRelativeShield(this.id); }
		get damageReturn() { return F.getDamageReturn(this.id); }
		get frequency() { return F.getFrequency(this.id); }
		get cores() { return F.getCores(this.id); }
		get ram() { return F.getRAM(this.id); }
		get cell() { return pooledCell(F.getCell(this.id)); }
		get weapon() { return weap(F.getWeapon(this.id)); }
		get weapons() { return weaps(F.getWeapons(this.id)); }
		get chips() { return chps(F.getChips(this.id)); }
		get effects() { return effs(F.getEffects(this.id)); }
		get launchedEffects() { return effs(F.getLaunchedEffects(this.id)); }
		get passiveEffects() { return feats(F.getPassiveEffects(this.id)); }
		get states() { return F.getStates(this.id); }
		get summons() { return ents(F.getSummons(this.id)); }
		get summoner() { return ent(F.getSummoner(this.id)); }
		get summoned() { return F.isSummon(this.id); }
		get alive() { return F.isAlive(this.id); }
		get dead() { return F.isDead(this.id); }
		// L'entité ne peut pas bouger (tourelle, coffre...).
		get isStatic() { return F.isStatic(this.id); }
		get birthTurn() { return F.getBirthTurn(this.id); }
		get turnOrder() { return F.getEntityTurnOrder(this.id); }
		get side() { return F.getSide(this.id); }
		get leekID() { return F.getLeekID(this.id); }
		get teamID() { return F.getTeamID(this.id); }
		get teamName() { return F.getTeamName(this.id); }
		get compositionName() { return F.getCompositionName(this.id); }
		get farmerID() { return F.getFarmerID(this.id); }
		get farmerName() { return F.getFarmerName(this.id); }
		get farmerCountry() { return F.getFarmerCountry(this.id); }
		get aiID() { return F.getAIID(this.id); }
		get aiName() { return F.getAIName(this.id); }
		isAlly() { return F.isAlly(this.id); }
		isEnemy() { return F.isEnemy(this.id); }
		// Valeur d'une caractéristique par sa constante (Entity.Stat.STRENGTH...).
		stat(stat) { return F.getStat(stat, this.id); }
		distance(target) { return F.getCellDistance(F.getCell(this.id), cid(target)); }
		// Entité d'id `id` (typée : Leek, Bulb, Mob...), ou null s'il est invalide. Chemin inverse des
		// ids acceptés partout par l'API : relire un id d'entité rangé dans un registre, par exemple.
		static get(id) { return ent(id); }
	}

	// ---- Sous-types d'entité : ent() (donc Fight.getNearestEnemy, getEnemies...) renvoie l'instance
	// TYPÉE selon getType() -> instanceof Mob/Chest/Bulb/Leek/Turret fonctionne. Ceux qui ont une
	// sous-catégorie exposent leur propre .type (chest.type === Chest.Type.WOOD). ----
	class Leek extends Entity {}
	class Turret extends Entity {}
	class Bulb extends Entity {
		get type() { return F.getBulbType(this.id); }
	}
	class Chest extends Entity {
		get type() { return F.getChestType(this.id); }
	}
	class Mob extends Entity {
		get type() { return F.getMobType(this.id); }
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
				get: function () { return F.getEntity(); },
				set: function () {},
				configurable: true,
				enumerable: true,
			});
		}
		moveToward(target, mp) {
			if (target instanceof Cell) return (mp === undefined) ? F.moveTowardCell(target.id) : F.moveTowardCell(target.id, mp);
			return (mp === undefined) ? F.moveToward(eid(target)) : F.moveToward(eid(target), mp);
		}
		moveAwayFrom(target, mp) {
			if (target instanceof Cell) return (mp === undefined) ? F.moveAwayFromCell(target.id) : F.moveAwayFromCell(target.id, mp);
			return (mp === undefined) ? F.moveAwayFrom(eid(target)) : F.moveAwayFrom(eid(target), mp);
		}
		// Variantes plurielles / géométriques du déplacement (mp = limite de PM à dépenser).
		moveTowardCells(cellList, mp) { var ids = cidList(cellList); return (mp === undefined) ? F.moveTowardCells(ids) : F.moveTowardCells(ids, mp); }
		moveTowardEntities(entities, mp) { var ids = unwrap(entities); return (mp === undefined) ? F.moveTowardEntities(ids) : F.moveTowardEntities(ids, mp); }
		moveTowardLine(a, b, mp) { return (mp === undefined) ? F.moveTowardLine(cid(a), cid(b)) : F.moveTowardLine(cid(a), cid(b), mp); }
		moveAwayFromCells(cellList, mp) { var ids = cidList(cellList); return (mp === undefined) ? F.moveAwayFromCells(ids) : F.moveAwayFromCells(ids, mp); }
		moveAwayFromEntities(entities, mp) { var ids = unwrap(entities); return (mp === undefined) ? F.moveAwayFromEntities(ids) : F.moveAwayFromEntities(ids, mp); }
		moveAwayFromLine(a, b, mp) { return (mp === undefined) ? F.moveAwayFromLine(cid(a), cid(b)) : F.moveAwayFromLine(cid(a), cid(b), mp); }
		useWeapon(target) { return F.useWeapon(eid(target)); }
		useWeaponOnCell(cell) { return F.useWeaponOnCell(cid(cell)); }
		useChip(chip, target) { return (target === undefined) ? F.useChip(cpid(chip)) : F.useChip(cpid(chip), eid(target)); }
		useChipOnCell(chip, cell) { return F.useChipOnCell(cpid(chip), cid(cell)); }
		setWeapon(weapon) { return F.setWeapon(wid(weapon)); }
		say(message) { return F.say(message); }
		// Fait dire « lama » à l'entité (trophée).
		lama() { return F.lama(); }
		// Peut-on utiliser l'arme (courante, ou `weapon` en 1er argument) sur `target`.
		canUseWeapon() { return F.canUseWeapon.apply(null, unwrapAll(arguments)); }
		canUseWeaponOnCell() { return F.canUseWeaponOnCell.apply(null, unwrapAll(arguments)); }
		canUseChip(chip, target) { return F.canUseChip(cpid(chip), eid(target)); }
		canUseChipOnCell(chip, cell) { return F.canUseChipOnCell(cpid(chip), cid(cell)); }
		resurrect(target, cell) { return F.resurrect(eid(target), cid(cell)); }
		// Nombre d'utilisations de l'item (arme ou puce) par l'entité courante ce tour.
		itemUses(item) { return F.getItemUses(unwrap(item)); }
		// Change l'équipement courant (nom du loadout).
		setLoadout(name, keep) { return (keep === undefined) ? F.setLoadout(name) : F.setLoadout(name, keep); }
		// Invoque un bulbe : callback = fonction guest rejouee a chaque tour du bulbe (pendant laquelle
		// me/getEntity() designent le bulbe). cf TypeMarshaller.wrapGuestFunction + BulbAI.
		summon(chip, cell, callback, name) {
			return (name === undefined) ? F.summon(cpid(chip), cid(cell), callback) : F.summon(cpid(chip), cid(cell), callback, name);
		}
		// Cellule (ou toutes les cellules) d'où utiliser l'arme/puce sur `target` — une entité OU une
		// case (routage automatique). Retour Cell / Cell[]. Args déballés (objets ou ids, ordre libre).
		weaponCell(target) { return cell(((target instanceof Cell) ? F.getCellToUseWeaponOnCell : F.getCellToUseWeapon).apply(null, unwrapAll(arguments))); }
		weaponCells(target) { return cells(((target instanceof Cell) ? F.getCellsToUseWeaponOnCell : F.getCellsToUseWeapon).apply(null, unwrapAll(arguments))); }
		chipCell(chip, target) { return cell(((target instanceof Cell) ? F.getCellToUseChipOnCell : F.getCellToUseChip).apply(null, unwrapAll(arguments))); }
		chipCells(chip, target) { return cells(((target instanceof Cell) ? F.getCellsToUseChipOnCell : F.getCellsToUseChip).apply(null, unwrapAll(arguments))); }
		// Entités touchées par une arme/puce lancée sur une cellule. Retour Entity[].
		weaponTargets() { return ents(F.getWeaponTargets.apply(null, unwrapAll(arguments))); }
		chipTargets() { return ents(F.getChipTargets.apply(null, unwrapAll(arguments))); }
	}

	// Instance unique de l'IA courante (Me), exposée via Fight.me. Une seule instance suffit : son id
	// est un accessor dynamique qui suit l'entité courante (cf classe Me).
	// PAS gelée, à la différence de tout le reste, et volontairement : `me` n'est pas une enveloppe
	// poolée mais un singleton qui vit tout le combat, donc le joueur peut déjà y ranger son propre
	// état d'un tour sur l'autre (me.cible = ...) et ça marche. Geler casserait ce comportement
	// existant, sans rien protéger : il n'y a qu'un seul `me`, et son `id` est un accessor à setter
	// no-op, donc déjà immunisé contre une écriture. ent() ne fabrique jamais de Me (Leek/Bulb/...),
	// donc aucune instance de Me ne passe par frozen().
	var meSelf = new Me();

	// ---- Message : un message d'équipe reçu (cf Network) ----
	class Message {
		constructor(raw) { this.raw = raw; }
		get author() { return ent(F.getMessageAuthor(this.raw)); }
		get type() { return F.getMessageType(this.raw); }
		get params() { return F.getMessageParams(this.raw); }
	}

	// ---- Fight : entités et état global du combat ----
	var Fight = {
		// L'IA courante (votre entité). Remplace l'ancien global `me` : var me = Fight.me
		get me() { return meSelf; },
		get turn() { return F.getTurn(); },
		get id() { return F.getFightID(); },
		get type() { return F.getFightType(); },
		get context() { return F.getFightContext(); },
		get boss() { return F.getFightBoss(); },
		get winner() { return F.getWinner(); },
		get alliesLife() { return F.getAlliesLife(); },
		get enemiesLife() { return F.getEnemiesLife(); },
		getNearestEnemy: function () { return ent(F.getNearestEnemy()); },
		getNearestAlly: function () { return ent(F.getNearestAlly()); },
		getFarthestEnemy: function () { return ent(F.getFarthestEnemy()); },
		getFarthestAlly: function () { return ent(F.getFarthestAlly()); },
		getNearestEnemyTo: function (target) { return ent(F.getNearestEnemyTo(eid(target))); },
		getNearestAllyTo: function (target) { return ent(F.getNearestAllyTo(eid(target))); },
		getEnemies: function () { return ents(F.getEnemies()); },
		getAllies: function () { return ents(F.getAllies()); },
		getAliveEnemies: function () { return ents(F.getAliveEnemies()); },
		getAliveAllies: function () { return ents(F.getAliveAllies()); },
		getDeadEnemies: function () { return ents(F.getDeadEnemies()); },
		getDeadAllies: function () { return ents(F.getDeadAllies()); },
		getEnemiesCount: function () { return F.getEnemiesCount(); },
		getAlliesCount: function () { return F.getAlliesCount(); },
		getAliveEnemiesCount: function () { return F.getAliveEnemiesCount(); },
		getAliveAlliesCount: function () { return F.getAliveAlliesCount(); },
		getDeadEnemiesCount: function () { return F.getDeadEnemiesCount(); },
		getAlliedTurret: function () { return ent(F.getAlliedTurret()); },
		getEnemyTurret: function () { return ent(F.getEnemyTurret()); },
		// Entité alliée/ennemie la plus proche d'une CELLULE (complète getNearestEnemyTo qui prend une entité).
		getNearestEnemyToCell: function (c) { return ent(F.getNearestEnemyToCell(cid(c))); },
		getNearestAllyToCell: function (c) { return ent(F.getNearestAllyToCell(cid(c))); },
		// Joueur suivant / précédent dans l'ordre de jeu (défaut : relatif à soi).
		getNextPlayer: function (e) { return ent(e === undefined ? F.getNextPlayer() : F.getNextPlayer(eid(e))); },
		getPreviousPlayer: function (e) { return ent(e === undefined ? F.getPreviousPlayer() : F.getPreviousPlayer(eid(e))); },
		// Paroles prononcées (say) par les entités : liste de [entité, message].
		listen: function () { return F.listen(); },
	};

	// ---- Field : terrain et géométrie ----
	var Field = {
		get type() { return F.getMapType(); },
		cellFromXY: function (x, y) { var c = F.getCellFromXY(x, y); return cell(c); },
		getObstacles: function () { return cells(F.getObstacles()); },
		distance: function (a, b) { return F.getDistance(cid(a), cid(b)); },
		cellDistance: function (a, b) { return F.getCellDistance(cid(a), cid(b)); },
		pathLength: function (a, b, ignored) { return ignored === undefined ? F.getPathLength(cid(a), cid(b)) : F.getPathLength(cid(a), cid(b), cidList(ignored)); },
		lineOfSight: function (a, b, ignoredEntities) { return ignoredEntities === undefined ? F.lineOfSight(cid(a), cid(b)) : F.lineOfSight(cid(a), cid(b), unwrap(ignoredEntities)); },
		onSameLine: function (a, b) { return F.isOnSameLine(cid(a), cid(b)); },
		// Chemin (liste de cellules) de a à b, en évitant `ignored`. Retour Cell[].
		path: function (a, b, ignored) { return cells(ignored === undefined ? F.getPath(cid(a), cid(b)) : F.getPath(cid(a), cid(b), cidList(ignored))); },
	};

	// ---- Network : messages d'équipe entre IA alliées ----
	var Network = {
		// Envoie un message typé (Message.*) à une entité alliée.
		sendTo: function (entity, type, params) { return F.sendTo(eid(entity), type, params); },
		// Envoie un message typé à toutes les entités alliées.
		sendAll: function (type, params) { return F.sendAll(type, params); },
		// Messages reçus (de `entity` seulement si fourni). Retour Message[].
		getMessages: function (entity) {
			var raw = (entity === undefined) ? F.getMessages() : F.getMessages(eid(entity));
			var o = []; if (raw) for (var i = 0; i < raw.length; i++) o.push(frozen(new Message(raw[i])));
			return o;
		},
	};

	// ---- Registers : stockage persistant de l'IA (clé -> valeur, entre combats) ----
	var Registers = {
		get: function (key) { return F.getRegister(key); },
		set: function (key, value) { return F.setRegister(key, value); },
		delete: function (key) { return F.deleteRegister(key); },
		all: function () { return F.getRegisters(); },
	};

	// ---- Debug : marquage, visualisation et journal de combat (aide au développement d'IA) ----
	var Debug = {
		// Écrit dans le journal de combat, éventuellement en couleur (cf Color). console.log existe aussi.
		log: function (value, color) { return (color === undefined) ? F.debug(value) : F.debugC(value, color); },
		mark: function (cells, color, duration) {
			if (color === undefined) return F.mark(cidList(cells));
			if (duration === undefined) return F.mark(cidList(cells), color);
			return F.mark(cidList(cells), color, duration);
		},
		markText: function (cells, text, color, duration) {
			if (color === undefined) return F.markText(cidList(cells), text);
			if (duration === undefined) return F.markText(cidList(cells), text, color);
			return F.markText(cidList(cells), text, color, duration);
		},
		clearMarks: function () { return F.clearMarks(); },
		show: function (cell, color) { return (color === undefined) ? F.show(cid(cell)) : F.show(cid(cell), color); },
		pause: function () { return F.pause(); },
	};

	// ---- System : budget d'exécution et horloge du combat ----
	// L'implémentation de `operations` est SWAPPABLE : PolyglotEntityAI y branche (via le hook
	// one-shot __lw_setOps, consommé avant tout code joueur) la lecture GUEST du compteur de
	// statements (rapide), à la place de l'aller-retour hôte. cf JS_GETOPS_OVERRIDE.
	var opsImpl = function () { return F.getOperations(); };
	var System = {
		// Opérations consommées ce tour (à comparer à maxOperations pour borner une recherche).
		get operations() { return opsImpl(); },
		get maxOperations() { return F.getMaxOperations(); },
		get instructionsCount() { return F.getInstructionsCount(); },
		get usedRAM() { return F.getUsedRAM(); },
		get maxRAM() { return F.getMaxRAM(); },
		get date() { return F.getDate(); },
		get time() { return F.getTime(); },
		get timestamp() { return F.getTimestamp(); },
	};
	Object.defineProperty(globalThis, '__lw_setOps', {
		value: function (fn) { opsImpl = fn; try { delete globalThis.__lw_setOps; } catch (e) {} },
		writable: false, configurable: true,
	});

	// ---- Color : couleurs du journal / des marquages ----
	var Color = {
		// Compose une couleur depuis ses composantes 0-255.
		rgb: function (r, g, b) { return F.getColor(r, g, b); },
		red: function (color) { return F.getRed(color); },
		green: function (color) { return F.getGreen(color); },
		blue: function (color) { return F.getBlue(color); },
	};

	// Conteneur des constantes d'état spécial (State.PACIFIST...). Objet pur (pas d'instances).
	var State = {};

	// Range les constantes plates du bridge (WEAPON_*, EFFECT_*, STATE_*...) en membres OBJET, par
	// famille. Deux natures : les ITEMS (WEAPON_/CHIP_) donnent des INSTANCES poolées en camelCase
	// (Weapon.pistol), tout le reste donne des CATÉGORIES en MAJUSCULES (Effect.SHIELD, State.PACIFIST,
	// Fight.Type.SOLO...). Les préfixes composés (LAUNCH_TYPE_, FIGHT_TYPE_...) sont listés AVANT les
	// simples pour matcher en premier. Les constantes sans famille (PI, SORT_*, TYPE_*...) ne sont PAS
	// exposées : elles n'ont pas de sens en JS (Math.PI, typeof...).
	(function attachConstants() {
		function camel(s) { return s.toLowerCase().replace(/_([a-z0-9])/g, function (_m, c) { return c.toUpperCase(); }); }
		// Constantes attachées TELLES QUELLES à un conteneur (pas de préfixe à retirer).
		var EXACT = { OPERATIONS_LIMIT: System, INSTRUCTIONS_LIMIT: System,
			CRITICAL_FACTOR: Fight, MAX_TURNS: Fight, SUMMON_LIMIT: Fight };
		// {p: préfixe, fn: attache custom} OU {p, c: conteneur, s?: sous-conteneur}. Nom = clé après préfixe.
		var RULES = [
			// Les items alimentent au passage itemsById (id -> instance), qui sert a Effect.item et aux
			// accesseurs Item.get/Weapon.get/Chip.get sans jamais rappeler l'hote.
			{ p: 'WEAPON_', fn: function (n, v) { itemsById[v] = Weapon[camel(n)] = weap(v); } },
			{ p: 'CHIP_', fn: function (n, v) { itemsById[v] = Chip[camel(n)] = chp(v); } },
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
			{ p: 'MESSAGE_', c: Message, s: 'Type' },
			{ p: 'MAP_', c: Field },
			// EFFECT_* melangeait trois familles a plat : les TYPES d'effet (DAMAGE, HEAL...), les
			// MODIFICATEURS (bitmask de effect.modifiers) et les CIBLES (bitmask de feature.targets).
			// Les deux dernieres passent en sous-conteneurs -> Effect.Modifier.STACKABLE, Effect.Target.ALLIES.
			// A garder AVANT 'EFFECT_' : le premier prefixe qui matche gagne.
			{ p: 'EFFECT_MODIFIER_', c: Effect, s: 'Modifier' },
			{ p: 'EFFECT_TARGET_', c: Effect, s: 'Target' },
			{ p: 'EFFECT_', c: Effect },
			{ p: 'STATE_', c: State },
			{ p: 'COLOR_', c: Color },
		];
		function attach(r, name, val) {
			if (r.fn) { r.fn(name, val); return; }
			var box = r.c;
			// hasOwnProperty (pas `!box[r.s]`) : Bulb extends Entity -> Bulb.Type HÉRITERAIT
			// Entity.Type ; sans ça on polluerait Entity.Type au lieu de créer un Bulb.Type propre.
			if (r.s) { if (!Object.prototype.hasOwnProperty.call(box, r.s)) box[r.s] = {}; box = box[r.s]; }
			box[name] = val;
		}
		for (var i = 0; i < names.length; i++) {
			var k = names[i];
			if (typeof F[k] === 'function') continue; // fonction de combat, pas une constante
			if (EXACT[k]) { try { EXACT[k][k] = F[k]; } catch (e) {} continue; }
			for (var j = 0; j < RULES.length; j++) {
				if (k.indexOf(RULES[j].p) === 0) {
					try { attach(RULES[j], k.slice(RULES[j].p.length), F[k]); } catch (e) { /* nom réservé : on saute */ }
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
	globalThis.Message = Message;
	globalThis.Me = Me;
	globalThis.State = State;
	globalThis.Fight = Fight;
	globalThis.Field = Field;
	globalThis.Network = Network;
	globalThis.Registers = Registers;
	globalThis.Debug = Debug;
	globalThis.System = System;
	globalThis.Color = Color;
})();
