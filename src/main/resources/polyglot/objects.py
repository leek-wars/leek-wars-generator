# API de combat ORIENTÉE OBJET pour les IA polyglot Python (style stdlib objet LeekScript v5).
# SEULE API exposée au joueur : le sac caché __lw (fonctions + constantes plates du bridge, cf
# PolyglotAPIBridge) est capturé dans la closure de _lw_build puis RETIRÉ du scope main -> aucune
# fonction/constante plate n'est visible. Les noms publics (Fight, Entity, Weapon...) sont posés
# sur BUILTINS : visibles depuis le fichier principal ET les modules importés du joueur.
# Propriétés = lecture d'état, méthodes = actions/calculs.

def _lw_build(G, NAMES):
    import builtins as B

    # Capture du sac dans un porte-fonctions LOCAL : F.getLife est un attribut Python (rapide),
    # là où G.getLife serait un readMember interop (aller-retour hôte potentiel) à CHAQUE appel.
    # NAMES (liste triée fournie par le bridge) évite de dépendre de l'énumération des membres.
    class _FBox: pass
    F = _FBox()
    _const_names = []
    for _n in NAMES:
        _n = str(_n)
        _v = getattr(G, _n)
        setattr(F, _n, _v)
        if not callable(_v):
            _const_names.append(_n)

    def _cid(x):
        if isinstance(x, Cell):
            return x.id
        if isinstance(x, Entity):
            return F.getCell(x.id)
        return x

    def _eid(x): return x.id if isinstance(x, Entity) else x
    def _wid(x): return x.id if isinstance(x, Weapon) else x
    def _cpid(x): return x.id if isinstance(x, Chip) else x
    # Fabrique l'instance TYPEE d'une entite selon son type (getType). Classes definies plus bas mais
    # _ent n'est appelee qu'au runtime (closure) -> OK.
    # Pools de singletons par id. Historiquement sur Weapon/Chip seulement ; etendus a Cell et Entity,
    # pour que les valeurs de l'API (me.weapon, me.cell, getEnemies()...) et les constantes objet
    # (Weapon.pistol) soient LE MEME objet -> comparables par identite (me.weapon is Weapon.pistol,
    # path[0] is me.cell). Sinon deux Weapon(id) differeraient.
    # Sur Entity le pool economise en plus un getType() (15 operations) par emballage : le type d'une
    # entite ne change jamais du combat, la ou getEnemies() en payait un PAR ennemi et PAR appel.
    _weapon_pool = {}
    _chip_pool = {}
    _cell_pool = {}
    _entity_pool = {}
    # Id d'item (arme OU puce) -> instance, remplie au boot par l'attache des constantes (qui instancie
    # deja les 37 armes et 110 puces). Permet a Effect.item de rendre l'objet SANS appel hote.
    _items_by_id = {}
    def _ent(i):
        if i is None or i < 0: return None
        e = _entity_pool.get(i)
        if e is not None: return e
        t = F.getType(i)
        if t == F.ENTITY_LEEK: e = Leek(i)
        elif t == F.ENTITY_BULB: e = Bulb(i)
        elif t == F.ENTITY_TURRET: e = Turret(i)
        elif t == F.ENTITY_CHEST: e = Chest(i)
        elif t == F.ENTITY_MOB: e = Mob(i)
        else: e = Entity(i)
        _entity_pool[i] = e
        return e
    def _ents(ids): return [e for e in (_ent(i) for i in (ids or [])) if e is not None]
    # Cellule poolee SANS garde de validite : conserve le comportement historique de Cell(i), qui
    # rendait un objet meme pour un id negatif (cf _cells et Entity.cell). _cell ajoute la garde.
    def _pooled_cell(i):
        c = _cell_pool.get(i)
        if c is None: c = Cell(i); _cell_pool[i] = c
        return c
    def _cells(ids): return [_pooled_cell(i) for i in (ids or [])]
    def _weap(i):
        if i is None or i <= 0: return None
        w = _weapon_pool.get(i)
        if w is None: w = Weapon(i); _weapon_pool[i] = w
        return w
    def _chp(i):
        if i is None or i <= 0: return None
        c = _chip_pool.get(i)
        if c is None: c = Chip(i); _chip_pool[i] = c
        return c
    def _weaps(ids): return [_weap(i) for i in (ids or [])]
    def _chps(ids): return [_chp(i) for i in (ids or [])]
    def _cidlist(x): return [_cid(i) for i in x] if isinstance(x, list) else _cid(x)
    def _cell(i): return None if i is None or i < 0 else _pooled_cell(i)
    # Déballe un argument vers son id brut (Entity/Weapon/Chip/Cell -> .id ; liste -> déballée). Sert aux
    # helpers de ciblage pour accepter des objets quel que soit l'ordre des arguments.
    def _unwrap(x):
        if isinstance(x, (Entity, Weapon, Chip, Cell)): return x.id
        if isinstance(x, list): return [_unwrap(i) for i in x]
        return x
    def _unwrapall(args): return [_unwrap(a) for a in args]

    # Tout objet rendu au joueur est en LECTURE SEULE. Equivalent Python de l'Object.freeze du cote JS
    # (objects.js), pour la meme raison : les enveloppes sont POOLEES, donc sans garde un `c.id = 42`
    # du joueur empoisonnerait la cellule partagee pour tout le reste du combat, et chaque getPath()
    # retombant sur cet id rendrait un objet corrompu, sans le moindre message. Et ca aligne le runtime
    # sur ce que les declarations annoncent deja (readonly sur chaque membre du .pyi / du .d.ts).
    # Les __init__ passent par object.__setattr__ pour poser leur id/raw malgre le garde.
    # NB : ici on LEVE, la ou un objet gele en JS ignore silencieusement l'ecriture (sauf IA multi-
    # fichiers, chargee en module ES donc en mode strict, qui leve aussi).
    class _ReadOnly:
        def __setattr__(self, name, value):
            raise AttributeError("les objets de l'API Leek Wars sont en lecture seule")
        def __delattr__(self, name):
            raise AttributeError("les objets de l'API Leek Wars sont en lecture seule")

    class Cell(_ReadOnly):
        def __init__(self, id): object.__setattr__(self, 'id', id)
        # Cellule d'id `id`, ou None s'il est invalide. L'API ACCEPTE des ids partout, il faut donc
        # pouvoir faire le chemin inverse : typiquement relire un id range dans un registre.
        
        def get(id): return _cell(id)
        @property
        def x(self): return F.getCellX(self.id)
        @property
        def y(self): return F.getCellY(self.id)
        @property
        def empty(self): return F.isEmptyCell(self.id)
        @property
        def obstacle(self): return F.isObstacle(self.id)
        @property
        def entity(self): return _ent(F.getEntityOnCell(self.id))
        # Une entité (poireau, bulbe, tourelle...) occupe-t-elle la case.
        @property
        def hasEntity(self): return F.isEntity(self.id)
        # Contenu de la case (Cell.Type.EMPTY/PLAYER/ENTITY/OBSTACLE).
        @property
        def content(self): return F.getCellContent(self.id)
        def distance(self, target): return F.getCellDistance(self.id, _cid(target))
        def pathLength(self, target, ignored=None):
            return F.getPathLength(self.id, _cid(target)) if ignored is None else F.getPathLength(self.id, _cid(target), _cidlist(ignored))
        # Ligne de vue jusqu'à la cible, en ignorant éventuellement des entités (ignoredEntities).
        def lineOfSight(self, target, ignoredEntities=None):
            return F.lineOfSight(self.id, _cid(target)) if ignoredEntities is None else F.lineOfSight(self.id, _cid(target), _unwrap(ignoredEntities))
        # Chemin (liste de cellules) jusqu'à la cible. Retour list[Cell].
        def path(self, target, ignored=None):
            return _cells(F.getPath(self.id, _cid(target)) if ignored is None else F.getPath(self.id, _cid(target), _cidlist(ignored)))
        # La case est-elle alignée (même ligne ou colonne) avec la cible.
        def onSameLine(self, target): return F.isOnSameLine(self.id, _cid(target))

    # Base commune aux armes et puces. Porte les constantes partagees (Item.LaunchType, Item.Area).
    # Les getters restent dans les sous-classes (fonctions plates distinctes getWeapon*/getChip*).
    class Item(_ReadOnly):
        def __init__(self, id): object.__setattr__(self, 'id', id)
        # L'item (arme OU puce) d'id `id`, ou None. Weapon.get/Chip.get restreignent a leur type.
        
        def get(id): return _items_by_id.get(id)

    class Weapon(Item):
        @property
        def cost(self): return F.getWeaponCost(self.id)
        @property
        def minRange(self): return F.getWeaponMinRange(self.id)
        @property
        def maxRange(self): return F.getWeaponMaxRange(self.id)
        @property
        def name(self): return F.getWeaponName(self.id)
        @property
        def area(self): return F.getWeaponArea(self.id)
        @property
        def launchType(self): return F.getWeaponLaunchType(self.id)
        @property
        def maxUses(self): return F.getWeaponMaxUses(self.id)
        @property
        def inline(self): return F.isInlineWeapon(self.id)
        @property
        def needsLos(self): return F.weaponNeedLos(self.id)
        # Pourcentage d'échec de l'arme.
        @property
        def failure(self): return F.getWeaponFailure(self.id)
        # features : caracteristiques declarees de l'arme (list[Feature] : degats, poison, teleport...),
        # distinct de entity.effects (Effect ACTIFS). Property (lecture d'etat).
        @property
        def features(self): return _feats(F.getWeaponEffects(self.id))
        # Caractéristiques passives de l'arme (bonus quand elle est équipée). list[Feature].
        @property
        def passiveFeatures(self): return _feats(F.getWeaponPassiveEffects(self.id))
        # Zone d'effet réelle de l'arme sur cell, tirée depuis frm (défaut : position courante). list[Cell].
        def effectiveArea(self, cell, frm=None):
            return _cells(F.getWeaponEffectiveArea(self.id, _cid(cell)) if frm is None else F.getWeaponEffectiveArea(self.id, _cid(cell), _cid(frm)))
        # Toutes les armes du jeu. list[Weapon].
        @staticmethod
        def getAll(): return _weaps(F.getAllWeapons())
        # L'arme d'id `id`, ou None si l'id n'est pas celui d'une arme.
        @staticmethod
        def get(id):
            i = _items_by_id.get(id)
            return i if isinstance(i, Weapon) else None
        # La valeur est-elle un id d'arme valide.
        @staticmethod
        def isWeapon(value): return F.isWeapon(_wid(value))

    class Chip(Item):
        @property
        def cost(self): return F.getChipCost(self.id)
        @property
        def cooldown(self): return F.getChipCooldown(self.id)
        @property
        def currentCooldown(self): return F.getCooldown(self.id)
        @property
        def minRange(self): return F.getChipMinRange(self.id)
        @property
        def maxRange(self): return F.getChipMaxRange(self.id)
        @property
        def minScope(self): return F.getChipMinScope(self.id)
        @property
        def maxScope(self): return F.getChipMaxScope(self.id)
        @property
        def name(self): return F.getChipName(self.id)
        @property
        def area(self): return F.getChipArea(self.id)
        @property
        def launchType(self): return F.getChipLaunchType(self.id)
        @property
        def maxUses(self): return F.getChipMaxUses(self.id)
        @property
        def inline(self): return F.isInlineChip(self.id)
        @property
        def needsLos(self): return F.chipNeedLos(self.id)
        # Pourcentage d'échec de la puce.
        @property
        def failure(self): return F.getChipFailure(self.id)
        # features : caracteristiques declarees de la puce (list[Feature]). cf Weapon.features.
        @property
        def features(self): return _feats(F.getChipEffects(self.id))
        # Pour une puce d'INVOCATION : puces du bulbe invoqué (list[Chip]), ses caractéristiques et stats (dicts).
        @property
        def bulbChips(self): return _chps(F.getBulbChips(self.id))
        @property
        def bulbCharacteristics(self): return F.getBulbCharacteristics(self.id)
        @property
        def bulbStats(self): return F.getBulbStats(self.id)
        # Cooldown restant de la puce pour une AUTRE entité que soi.
        def currentCooldownOf(self, entity): return F.getCooldown(self.id, _eid(entity))
        # Zone d'effet réelle de la puce sur cell, lancée depuis frm (défaut : position courante). list[Cell].
        def effectiveArea(self, cell, frm=None):
            return _cells(F.getChipEffectiveArea(self.id, _cid(cell)) if frm is None else F.getChipEffectiveArea(self.id, _cid(cell), _cid(frm)))
        # Toutes les puces du jeu. list[Chip].
        @staticmethod
        def getAll(): return _chps(F.getAllChips())
        # La puce d'id `id`, ou None si l'id n'est pas celui d'une puce.
        @staticmethod
        def get(id):
            i = _items_by_id.get(id)
            return i if isinstance(i, Chip) else None
        # La valeur est-elle un id de puce valide.
        @staticmethod
        def isChip(value): return F.isChip(_cpid(value))

    # Effet ACTIF/lancé : [type, value, caster, turns, critical, item, target, modifiers]
    class Effect(_ReadOnly):
        def __init__(self, raw): object.__setattr__(self, 'raw', raw)
        @property
        def type(self): return self.raw[0]
        @property
        def value(self): return self.raw[1]
        @property
        def caster(self): return _ent(self.raw[2])
        @property
        def turns(self): return self.raw[3]
        @property
        def critical(self): return self.raw[4]
        @property
        def item(self):
            i = self.raw[5]
            return _items_by_id.get(i) if i else None
        @property
        def target(self): return _ent(self.raw[6])
        @property
        def modifiers(self): return self.raw[7]
        # Liste des ids de TYPES d'effets existants (Effect.DAMAGE, Effect.HEAL...). list[int].
        @staticmethod
        def getAll(): return F.getAllEffects()

    # Feature : CARACTÉRISTIQUE déclarée par une arme/puce (ou effet passif) : ce que l'item PEUT faire
    # (dégâts, poison, téléport, inversion...). Potentiel, pas encore appliqué -> distinct d'Effect
    # (actif sur une entité). Brut = [type, minValue, maxValue, turns, targets, modifiers].
    class Feature(_ReadOnly):
        def __init__(self, raw): object.__setattr__(self, 'raw', raw)
        @property
        def type(self): return self.raw[0]
        @property
        def minValue(self): return self.raw[1]
        @property
        def maxValue(self): return self.raw[2]
        @property
        def turns(self): return self.raw[3]
        @property
        def targets(self): return self.raw[4]
        @property
        def modifiers(self): return self.raw[5]

    def _effs(arr): return [Effect(e) for e in (arr or [])]
    def _feats(arr): return [Feature(e) for e in (arr or [])]

    class Entity(_ReadOnly):
        def __init__(self, id): object.__setattr__(self, 'id', id)
        # Genre d'entite (Entity.Type.LEEK/BULB/TURRET/CHEST/MOB), cf #4634. Ne pas
        # confondre avec .type des sous-classes (sous-variante : Bulb.Type.*, etc.).
        @property
        def entityType(self): return F.getType(self.id)
        @property
        def life(self): return F.getLife(self.id)
        @property
        def maxLife(self): return F.getTotalLife(self.id)
        @property
        def tp(self): return F.getTP(self.id)
        @property
        def maxTP(self): return F.getTotalTP(self.id)
        @property
        def mp(self): return F.getMP(self.id)
        @property
        def maxMP(self): return F.getTotalMP(self.id)
        @property
        def strength(self): return F.getStrength(self.id)
        @property
        def agility(self): return F.getAgility(self.id)
        @property
        def wisdom(self): return F.getWisdom(self.id)
        @property
        def resistance(self): return F.getResistance(self.id)
        @property
        def science(self): return F.getScience(self.id)
        @property
        def magic(self): return F.getMagic(self.id)
        @property
        def power(self): return F.getPower(self.id)
        @property
        def level(self): return F.getLevel(self.id)
        @property
        def name(self): return F.getName(self.id)
        @property
        def absoluteShield(self): return F.getAbsoluteShield(self.id)
        @property
        def relativeShield(self): return F.getRelativeShield(self.id)
        @property
        def damageReturn(self): return F.getDamageReturn(self.id)
        @property
        def frequency(self): return F.getFrequency(self.id)
        @property
        def cores(self): return F.getCores(self.id)
        @property
        def ram(self): return F.getRAM(self.id)
        @property
        def cell(self): return _pooled_cell(F.getCell(self.id))
        @property
        def weapon(self): return _weap(F.getWeapon(self.id))
        @property
        def weapons(self): return _weaps(F.getWeapons(self.id))
        @property
        def chips(self): return _chps(F.getChips(self.id))
        @property
        def effects(self): return _effs(F.getEffects(self.id))
        @property
        def launchedEffects(self): return _effs(F.getLaunchedEffects(self.id))
        @property
        def passiveEffects(self): return _feats(F.getPassiveEffects(self.id))
        @property
        def states(self): return F.getStates(self.id)
        @property
        def summons(self): return _ents(F.getSummons(self.id))
        @property
        def summoner(self): return _ent(F.getSummoner(self.id))
        @property
        def summoned(self): return F.isSummon(self.id)
        @property
        def alive(self): return F.isAlive(self.id)
        @property
        def dead(self): return F.isDead(self.id)
        # L'entité ne peut pas bouger (tourelle, coffre...).
        @property
        def isStatic(self): return F.isStatic(self.id)
        @property
        def birthTurn(self): return F.getBirthTurn(self.id)
        @property
        def turnOrder(self): return F.getEntityTurnOrder(self.id)
        @property
        def side(self): return F.getSide(self.id)
        @property
        def leekID(self): return F.getLeekID(self.id)
        @property
        def teamID(self): return F.getTeamID(self.id)
        @property
        def teamName(self): return F.getTeamName(self.id)
        @property
        def compositionName(self): return F.getCompositionName(self.id)
        @property
        def farmerID(self): return F.getFarmerID(self.id)
        @property
        def farmerName(self): return F.getFarmerName(self.id)
        @property
        def farmerCountry(self): return F.getFarmerCountry(self.id)
        @property
        def aiID(self): return F.getAIID(self.id)
        @property
        def aiName(self): return F.getAIName(self.id)
        def isAlly(self): return F.isAlly(self.id)
        def isEnemy(self): return F.isEnemy(self.id)
        # Valeur d'une caractéristique par sa constante (Entity.Stat.STRENGTH...).
        def stat(self, stat): return F.getStat(stat, self.id)
        def distance(self, target): return F.getCellDistance(F.getCell(self.id), _cid(target))
        # Entite d'id `id` (typee : Leek, Bulb, Mob...), ou None s'il est invalide. Chemin inverse
        # des ids acceptes partout par l'API : relire un id d'entite range dans un registre.
        @staticmethod
        def get(id): return _ent(id)

    class Me(Entity):
        def __init__(self): super().__init__(-1)  # id jetable : la property ci-dessous le rend dynamique
        # SEUL objet de l'API qui reste inscriptible, volontairement : `me` n'est pas une enveloppe
        # poolee mais un singleton qui vit tout le combat, donc le joueur peut deja y ranger son propre
        # etat d'un tour sur l'autre (me.cible = ...) et ca marche. Le garde de _ReadOnly casserait ce
        # comportement existant sans rien proteger : il n'y a qu'un seul `me`, et son id est une
        # property a setter no-op, donc deja immunise contre une ecriture.
        def __setattr__(self, name, value): object.__setattr__(self, name, value)
        def __delattr__(self, name): object.__delattr__(self, name)
        # me suit l'entite COURANTE. Pendant le tour d'un bulbe (summon), BulbAI rebascule mEntity de l'IA
        # invocatrice sur le bulbe -> me.id doit renvoyer le bulbe. Setter no-op OBLIGATOIRE (Entity.__init__
        # fait self.id = id). Property posee SEULEMENT sur Me : Entity/Cell/Weapon/Chip gardent un id fige.
        @property
        def id(self): return F.getEntity()
        @id.setter
        def id(self, value): pass
        def moveToward(self, target, mp=None):
            if isinstance(target, Cell):
                return F.moveTowardCell(target.id) if mp is None else F.moveTowardCell(target.id, mp)
            return F.moveToward(_eid(target)) if mp is None else F.moveToward(_eid(target), mp)
        def moveAwayFrom(self, target, mp=None):
            if isinstance(target, Cell):
                return F.moveAwayFromCell(target.id) if mp is None else F.moveAwayFromCell(target.id, mp)
            return F.moveAwayFrom(_eid(target)) if mp is None else F.moveAwayFrom(_eid(target), mp)
        # Variantes plurielles / géométriques du déplacement (mp = limite de PM à dépenser).
        def moveTowardCells(self, cells, mp=None):
            ids = _cidlist(cells)
            return F.moveTowardCells(ids) if mp is None else F.moveTowardCells(ids, mp)
        def moveTowardEntities(self, entities, mp=None):
            ids = _unwrap(entities)
            return F.moveTowardEntities(ids) if mp is None else F.moveTowardEntities(ids, mp)
        def moveTowardLine(self, a, b, mp=None):
            return F.moveTowardLine(_cid(a), _cid(b)) if mp is None else F.moveTowardLine(_cid(a), _cid(b), mp)
        def moveAwayFromCells(self, cells, mp=None):
            ids = _cidlist(cells)
            return F.moveAwayFromCells(ids) if mp is None else F.moveAwayFromCells(ids, mp)
        def moveAwayFromEntities(self, entities, mp=None):
            ids = _unwrap(entities)
            return F.moveAwayFromEntities(ids) if mp is None else F.moveAwayFromEntities(ids, mp)
        def moveAwayFromLine(self, a, b, mp=None):
            return F.moveAwayFromLine(_cid(a), _cid(b)) if mp is None else F.moveAwayFromLine(_cid(a), _cid(b), mp)
        def useWeapon(self, target): return F.useWeapon(_eid(target))
        def useWeaponOnCell(self, cell): return F.useWeaponOnCell(_cid(cell))
        def useChip(self, chip, target=None):
            return F.useChip(_cpid(chip)) if target is None else F.useChip(_cpid(chip), _eid(target))
        def useChipOnCell(self, chip, cell): return F.useChipOnCell(_cpid(chip), _cid(cell))
        def setWeapon(self, weapon): return F.setWeapon(_wid(weapon))
        def say(self, message): return F.say(message)
        # Fait dire « lama » à l'entité (trophée).
        def lama(self): return F.lama()
        # Peut-on utiliser l'arme (courante, ou weapon en 1er argument) sur target.
        def canUseWeapon(self, *args): return F.canUseWeapon(*_unwrapall(args))
        def canUseWeaponOnCell(self, *args): return F.canUseWeaponOnCell(*_unwrapall(args))
        def canUseChip(self, chip, target): return F.canUseChip(_cpid(chip), _eid(target))
        def canUseChipOnCell(self, chip, cell): return F.canUseChipOnCell(_cpid(chip), _cid(cell))
        def resurrect(self, target, cell): return F.resurrect(_eid(target), _cid(cell))
        # Nombre d'utilisations de l'item (arme ou puce) par l'entité courante ce tour.
        def itemUses(self, item): return F.getItemUses(_unwrap(item))
        # Change l'équipement courant (nom du loadout).
        def setLoadout(self, name, keep=None):
            return F.setLoadout(name) if keep is None else F.setLoadout(name, keep)
        # Invoque un bulbe : callback = fonction guest rejouee a chaque tour du bulbe (me/getEntity() = bulbe).
        def summon(self, chip, cell, callback, name=None):
            return F.summon(_cpid(chip), _cid(cell), callback) if name is None else F.summon(_cpid(chip), _cid(cell), callback, name)
        # Cellule (ou toutes les cellules) d'ou utiliser l'arme/puce sur `target` -- une entite OU une case
        # (routage automatique). Retour Cell / list[Cell]. Args deballes (objets ou ids, ordre libre).
        def weaponCell(self, target, *rest): return _cell((F.getCellToUseWeaponOnCell if isinstance(target, Cell) else F.getCellToUseWeapon)(*_unwrapall((target,) + rest)))
        def weaponCells(self, target, *rest): return _cells((F.getCellsToUseWeaponOnCell if isinstance(target, Cell) else F.getCellsToUseWeapon)(*_unwrapall((target,) + rest)))
        def chipCell(self, chip, target, *rest): return _cell((F.getCellToUseChipOnCell if isinstance(target, Cell) else F.getCellToUseChip)(*_unwrapall((chip, target) + rest)))
        def chipCells(self, chip, target, *rest): return _cells((F.getCellsToUseChipOnCell if isinstance(target, Cell) else F.getCellsToUseChip)(*_unwrapall((chip, target) + rest)))
        # Entites touchees par une arme/puce lancee sur une cellule. Retour list[Entity].
        def weaponTargets(self, *args): return _ents(F.getWeaponTargets(*_unwrapall(args)))
        def chipTargets(self, *args): return _ents(F.getChipTargets(*_unwrapall(args)))

    # Sous-types d'entite : _ent() renvoie l'instance TYPEE selon getType() -> isinstance(x, Mob) marche.
    # Ceux qui ont une sous-categorie exposent leur propre .type (chest.type == Chest.Type.WOOD).
    class Leek(Entity): pass
    class Turret(Entity): pass
    class Bulb(Entity):
        @property
        def type(self): return F.getBulbType(self.id)
    class Chest(Entity):
        @property
        def type(self): return F.getChestType(self.id)
    class Mob(Entity):
        @property
        def type(self): return F.getMobType(self.id)

    # Message : un message d'equipe recu (cf Network).
    class Message(_ReadOnly):
        def __init__(self, raw): object.__setattr__(self, 'raw', raw)
        @property
        def author(self): return _ent(F.getMessageAuthor(self.raw))
        @property
        def type(self): return F.getMessageType(self.raw)
        @property
        def params(self): return F.getMessageParams(self.raw)

    class _Fight:
        # L'IA courante (votre entite). Remplace l'ancien global `me` : me = Fight.me
        @property
        def me(self): return _me_self
        @property
        def turn(self): return F.getTurn()
        @property
        def id(self): return F.getFightID()
        @property
        def type(self): return F.getFightType()
        @property
        def context(self): return F.getFightContext()
        @property
        def boss(self): return F.getFightBoss()
        @property
        def winner(self): return F.getWinner()
        @property
        def alliesLife(self): return F.getAlliesLife()
        @property
        def enemiesLife(self): return F.getEnemiesLife()
        def getNearestEnemy(self): return _ent(F.getNearestEnemy())
        def getNearestAlly(self): return _ent(F.getNearestAlly())
        def getFarthestEnemy(self): return _ent(F.getFarthestEnemy())
        def getFarthestAlly(self): return _ent(F.getFarthestAlly())
        def getNearestEnemyTo(self, target): return _ent(F.getNearestEnemyTo(_eid(target)))
        def getNearestAllyTo(self, target): return _ent(F.getNearestAllyTo(_eid(target)))
        def getNearestEnemyToCell(self, c): return _ent(F.getNearestEnemyToCell(_cid(c)))
        def getNearestAllyToCell(self, c): return _ent(F.getNearestAllyToCell(_cid(c)))
        def getEnemies(self): return _ents(F.getEnemies())
        def getAllies(self): return _ents(F.getAllies())
        def getAliveEnemies(self): return _ents(F.getAliveEnemies())
        def getAliveAllies(self): return _ents(F.getAliveAllies())
        def getDeadEnemies(self): return _ents(F.getDeadEnemies())
        def getDeadAllies(self): return _ents(F.getDeadAllies())
        def getEnemiesCount(self): return F.getEnemiesCount()
        def getAlliesCount(self): return F.getAlliesCount()
        def getAliveEnemiesCount(self): return F.getAliveEnemiesCount()
        def getAliveAlliesCount(self): return F.getAliveAlliesCount()
        def getDeadEnemiesCount(self): return F.getDeadEnemiesCount()
        def getAlliedTurret(self): return _ent(F.getAlliedTurret())
        def getEnemyTurret(self): return _ent(F.getEnemyTurret())
        # Joueur suivant / precedent dans l'ordre de jeu (defaut : relatif a soi).
        def getNextPlayer(self, e=None): return _ent(F.getNextPlayer() if e is None else F.getNextPlayer(_eid(e)))
        def getPreviousPlayer(self, e=None): return _ent(F.getPreviousPlayer() if e is None else F.getPreviousPlayer(_eid(e)))
        # Paroles prononcees (say) par les entites : liste de [entite, message].
        def listen(self): return F.listen()

    class _Field:
        @property
        def type(self): return F.getMapType()
        def cellFromXY(self, x, y):
            c = F.getCellFromXY(x, y)
            return _cell(c)
        def getObstacles(self): return _cells(F.getObstacles())
        def distance(self, a, b): return F.getDistance(_cid(a), _cid(b))
        def cellDistance(self, a, b): return F.getCellDistance(_cid(a), _cid(b))
        def pathLength(self, a, b, ignored=None):
            return F.getPathLength(_cid(a), _cid(b)) if ignored is None else F.getPathLength(_cid(a), _cid(b), _cidlist(ignored))
        def lineOfSight(self, a, b, ignoredEntities=None):
            return F.lineOfSight(_cid(a), _cid(b)) if ignoredEntities is None else F.lineOfSight(_cid(a), _cid(b), _unwrap(ignoredEntities))
        def onSameLine(self, a, b): return F.isOnSameLine(_cid(a), _cid(b))
        # Chemin (liste de cellules) de a a b. Retour list[Cell].
        def path(self, a, b, ignored=None):
            return _cells(F.getPath(_cid(a), _cid(b)) if ignored is None else F.getPath(_cid(a), _cid(b), _cidlist(ignored)))

    # Network : messages d'equipe entre IA alliees.
    class _Network:
        # Envoie un message type (Message.Type.*) a une entite alliee.
        def sendTo(self, entity, type, params): return F.sendTo(_eid(entity), type, params)
        # Envoie un message type a toutes les entites alliees.
        def sendAll(self, type, params): return F.sendAll(type, params)
        # Messages recus (de `entity` seulement si fourni). Retour list[Message].
        def getMessages(self, entity=None):
            raw = F.getMessages() if entity is None else F.getMessages(_eid(entity))
            return [Message(m) for m in (raw or [])]

    class _Registers:
        def get(self, key): return F.getRegister(key)
        def set(self, key, value): return F.setRegister(key, value)
        def delete(self, key): return F.deleteRegister(key)
        def all(self): return F.getRegisters()

    class _Debug:
        # Ecrit dans le journal de combat, eventuellement en couleur (cf Color). print() existe aussi.
        def log(self, value, color=None):
            return F.debug(value) if color is None else F.debugC(value, color)
        def mark(self, cells, color=None, duration=None):
            if color is None: return F.mark(_cidlist(cells))
            if duration is None: return F.mark(_cidlist(cells), color)
            return F.mark(_cidlist(cells), color, duration)
        def markText(self, cells, text, color=None, duration=None):
            if color is None: return F.markText(_cidlist(cells), text)
            if duration is None: return F.markText(_cidlist(cells), text, color)
            return F.markText(_cidlist(cells), text, color, duration)
        def clearMarks(self): return F.clearMarks()
        def show(self, cell, color=None):
            return F.show(_cid(cell)) if color is None else F.show(_cid(cell), color)
        def pause(self): return F.pause()

    # System : budget d'execution et horloge du combat. L'implementation de `operations` est
    # SWAPPABLE : PolyglotEntityAI y branche (via le hook one-shot __lw_set_ops sur builtins,
    # consomme avant tout code joueur) la lecture GUEST du compteur de statements (rapide),
    # a la place de l'aller-retour hote. cf PY_GETOPS_OVERRIDE.
    _ops_impl = [lambda: F.getOperations()]
    class _System:
        # Operations consommees ce tour (a comparer a maxOperations pour borner une recherche).
        @property
        def operations(self): return _ops_impl[0]()
        @property
        def maxOperations(self): return F.getMaxOperations()
        @property
        def instructionsCount(self): return F.getInstructionsCount()
        @property
        def usedRAM(self): return F.getUsedRAM()
        @property
        def maxRAM(self): return F.getMaxRAM()
        @property
        def date(self): return F.getDate()
        @property
        def time(self): return F.getTime()
        @property
        def timestamp(self): return F.getTimestamp()
    def _set_ops(fn):
        _ops_impl[0] = fn
        try:
            delattr(B, '__lw_set_ops')
        except Exception:
            pass
    setattr(B, '__lw_set_ops', _set_ops)

    # Color : couleurs du journal / des marquages.
    class _Color:
        # Compose une couleur depuis ses composantes 0-255.
        def rgb(self, r, g, b): return F.getColor(r, g, b)
        def red(self, color): return F.getRed(color)
        def green(self, color): return F.getGreen(color)
        def blue(self, color): return F.getBlue(color)

    Fight = _Fight()
    Field = _Field()
    Network = _Network()
    Registers = _Registers()
    Debug = _Debug()
    System = _System()
    Color = _Color()
    # Instance unique de l'IA courante, exposee via Fight.me (pas de global `me`). Une seule instance
    # suffit : son id est un accessor dynamique qui suit l'entite courante (cf classe Me).
    _me_self = Me()

    # Conteneur des constantes d'etat special (State.UNHEALABLE...).
    class State: pass

    # Namespace vide pour les sous-conteneurs (Fight.Type, Item.LaunchType...).
    class _NS: pass

    # Range les constantes plates du bridge (WEAPON_*, EFFECT_*, STATE_*...) en membres OBJET, par
    # famille : ITEMS (WEAPON_/CHIP_) -> INSTANCES poolees camelCase (Weapon.pistol), le reste ->
    # CATEGORIES en MAJUSCULES (Effect.DAMAGE, State.UNHEALABLE, Fight.Type.SOLO...). Prefixes composes
    # (LAUNCH_TYPE_...) listes AVANT les simples. Les constantes sans famille (PI, SORT_*, TYPE_*...)
    # ne sont PAS exposees : elles n'ont pas de sens en Python (math.pi, type()...).
    def _camel(s):
        parts = s.lower().split('_')
        return parts[0] + ''.join(p.capitalize() for p in parts[1:])

    # Constantes attachees TELLES QUELLES a un conteneur (pas de prefixe a retirer).
    _EXACT = { 'OPERATIONS_LIMIT': System, 'INSTRUCTIONS_LIMIT': System,
               'CRITICAL_FACTOR': Fight, 'MAX_TURNS': Fight, 'SUMMON_LIMIT': Fight }
    # (prefixe, 'item'|'cat', conteneur, poolfn_ou_souscategorie)
    _RULES = [
        ('WEAPON_', 'item', Weapon, _weap),
        ('CHIP_', 'item', Chip, _chp),
        ('LAUNCH_TYPE_', 'cat', Item, 'LaunchType'),
        ('FIGHT_TYPE_', 'cat', Fight, 'Type'),
        ('FIGHT_CONTEXT_', 'cat', Fight, 'Context'),
        ('AREA_', 'cat', Item, 'Area'),
        ('STAT_', 'cat', Entity, 'Stat'),
        ('ENTITY_', 'cat', Entity, 'Type'),
        ('CELL_', 'cat', Cell, 'Type'),
        ('CHEST_', 'cat', Chest, 'Type'),
        ('BULB_', 'cat', Bulb, 'Type'),
        ('MOB_', 'cat', Mob, 'Type'),
        ('BOSS_', 'cat', Fight, 'Boss'),
        ('EROSION_', 'cat', Fight, 'Erosion'),
        ('USE_', 'cat', Fight, 'Use'),
        ('MESSAGE_', 'cat', Message, 'Type'),
        ('MAP_', 'cat', Field, None),
        # EFFECT_* melangeait trois familles a plat : les TYPES d'effet (DAMAGE, HEAL...), les
        # MODIFICATEURS (bitmask de effect.modifiers) et les CIBLES (bitmask de feature.targets).
        # Les deux dernieres passent en sous-conteneurs. A garder AVANT 'EFFECT_' : le premier
        # prefixe qui matche gagne.
        ('EFFECT_MODIFIER_', 'cat', Effect, 'Modifier'),
        ('EFFECT_TARGET_', 'cat', Effect, 'Target'),
        ('EFFECT_', 'cat', Effect, None),
        ('STATE_', 'cat', State, None),
        ('COLOR_', 'cat', Color, None),
    ]

    def _sub(container, name):
        # __dict__ (pas getattr) : Bulb(Entity) HERITERAIT Entity.Type ; sans ce test on polluerait
        # Entity.Type au lieu de creer un Bulb.Type propre. Pour une INSTANCE (Fight, Field...),
        # __dict__ est son dict d'attributs : meme logique.
        if name not in container.__dict__:
            setattr(container, name, _NS())
        return container.__dict__[name]

    for _k in _const_names:
        _v = getattr(F, _k)
        if _k in _EXACT:
            try: setattr(_EXACT[_k], _k, _v)
            except Exception: pass
            continue
        for _p, _mode, _c, _extra in _RULES:
            if _k.startswith(_p):
                try:
                    _name = _k[len(_p):]
                    if _mode == 'item':
                        # Alimente au passage _items_by_id (id -> instance), qui sert a Effect.item et
                        # aux accesseurs Item.get/Weapon.get/Chip.get sans jamais rappeler l'hote.
                        _inst = _extra(_v)
                        _items_by_id[_v] = _inst
                        setattr(_c, _camel(_name), _inst)
                    else:
                        _box = _c if _extra is None else _sub(_c, _extra)
                        setattr(_box, _name, _v)
                except Exception:
                    pass  # nom reserve : on saute
                break

    return {
        'Cell': Cell, 'Entity': Entity, 'Leek': Leek, 'Turret': Turret, 'Bulb': Bulb,
        'Chest': Chest, 'Mob': Mob, 'Item': Item, 'Weapon': Weapon, 'Chip': Chip,
        'Effect': Effect, 'Feature': Feature, 'Message': Message, 'Me': Me, 'State': State,
        'Fight': Fight, 'Field': Field, 'Network': Network, 'Registers': Registers,
        'Debug': Debug, 'System': System, 'Color': Color,
    }


# Les noms publics vont sur BUILTINS : visibles depuis le fichier principal ET les modules importes.
import builtins as _lw_builtins
for _lw_k, _lw_v in _lw_build(__lw, __lw_names).items():
    setattr(_lw_builtins, _lw_k, _lw_v)
# Nettoyage du scope main : ni le sac, ni le builder, ni les residus des preludes (_lw_install du
# charge guard...) ne doivent rester visibles du joueur. '_lw_k' en DERNIER (variable de la boucle).
for _lw_k in ('__lw', '__lw_names', '_lw_build', '_lw_builtins', '_lw_install', '_lwb', '_lwc',
              '_lw_mk_print', '_lw_v', '_lw_k'):
    try:
        del globals()[_lw_k]
    except Exception:
        pass
