# API de combat ORIENTÉE OBJET pour les IA polyglot Python (style stdlib objet LeekScript v5).
# Couche guest au-dessus de l'API plate déjà bridgée. Propriétés = lecture d'état, méthodes =
# actions/calculs. Évaluée dans chaque contexte Python après le bridge (cf PolyglotEntityAI).
# L'API plate reste valide (coexistence) ; l'objet est la forme idiomatique.

def _cid(x):
    if isinstance(x, Cell):
        return x.id
    if isinstance(x, Entity):
        return getCell(x.id)
    return x

def _eid(x): return x.id if isinstance(x, Entity) else x
def _wid(x): return x.id if isinstance(x, Weapon) else x
def _cpid(x): return x.id if isinstance(x, Chip) else x
# Fabrique l'instance TYPEE d'une entite selon son type (getType). Classes definies plus bas mais
# _ent n'est appelee qu'au runtime (module charge) -> OK.
def _ent(i):
    if i is None or i < 0: return None
    t = getType(i)
    if t == ENTITY_LEEK: return Leek(i)
    if t == ENTITY_BULB: return Bulb(i)
    if t == ENTITY_TURRET: return Turret(i)
    if t == ENTITY_CHEST: return Chest(i)
    if t == ENTITY_MOB: return Mob(i)
    return Entity(i)
def _ents(ids): return [e for e in (_ent(i) for i in (ids or [])) if e is not None]
def _cells(ids): return [Cell(i) for i in (ids or [])]
# Pool de singletons par id pour Weapon/Chip : une seule instance par id dans un contexte, pour
# que les valeurs de l'API (me.weapon...) et les constantes objet (Weapon.pistol) soient LE MEME
# objet -> comparables par identite (me.weapon is Weapon.pistol). Sinon deux Weapon(id) differeraient.
_weapon_pool = {}
_chip_pool = {}
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


class Cell:
    def __init__(self, id): self.id = id
    @property
    def x(self): return getCellX(self.id)
    @property
    def y(self): return getCellY(self.id)
    @property
    def empty(self): return isEmptyCell(self.id)
    @property
    def obstacle(self): return isObstacle(self.id)
    @property
    def entity(self): return _ent(getEntityOnCell(self.id))
    def distance(self, target): return getCellDistance(self.id, _cid(target))
    def pathLength(self, target): return getPathLength(self.id, _cid(target))
    def lineOfSight(self, target): return lineOfSight(self.id, _cid(target))


# Base commune aux armes et puces. Porte les constantes partagees (Item.LaunchType, Item.Area).
# Les getters restent dans les sous-classes (fonctions plates distinctes getWeapon*/getChip*).
class Item:
    def __init__(self, id): self.id = id


class Weapon(Item):
    @property
    def cost(self): return getWeaponCost(self.id)
    @property
    def minRange(self): return getWeaponMinRange(self.id)
    @property
    def maxRange(self): return getWeaponMaxRange(self.id)
    @property
    def minScope(self): return getWeaponMinScope(self.id)
    @property
    def maxScope(self): return getWeaponMaxScope(self.id)
    @property
    def name(self): return getWeaponName(self.id)
    @property
    def area(self): return getWeaponArea(self.id)
    @property
    def launchType(self): return getWeaponLaunchType(self.id)
    @property
    def maxUses(self): return getWeaponMaxUses(self.id)
    @property
    def inline(self): return isInlineWeapon(self.id)
    def needLos(self): return weaponNeedLos(self.id)
    # features : caracteristiques declarees de l'arme (Feature[] : degats, poison, teleport...),
    # distinct de entity.effects (Effect ACTIFS). Property (lecture d'etat).
    @property
    def features(self): return _feats(getWeaponEffects(self.id))


class Chip(Item):
    @property
    def cost(self): return getChipCost(self.id)
    @property
    def cooldown(self): return getChipCooldown(self.id)
    @property
    def currentCooldown(self): return getCurrentCooldown(self.id)
    @property
    def minRange(self): return getChipMinRange(self.id)
    @property
    def maxRange(self): return getChipMaxRange(self.id)
    @property
    def minScope(self): return getChipMinScope(self.id)
    @property
    def maxScope(self): return getChipMaxScope(self.id)
    @property
    def name(self): return getChipName(self.id)
    @property
    def area(self): return getChipArea(self.id)
    @property
    def launchType(self): return getChipLaunchType(self.id)
    @property
    def maxUses(self): return getChipMaxUses(self.id)
    @property
    def inline(self): return isInlineChip(self.id)
    def needLos(self): return chipNeedLos(self.id)
    # features : caracteristiques declarees de la puce (Feature[]). cf Weapon.features.
    @property
    def features(self): return _feats(getChipEffects(self.id))


# Effet ACTIF/lancé : [type, value, caster, turns, critical, item, target, modifiers]
class Effect:
    def __init__(self, raw): self.raw = raw
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
    def item(self): return self.raw[5]
    @property
    def target(self): return _ent(self.raw[6])
    @property
    def modifiers(self): return self.raw[7]


# Feature : CARACTÉRISTIQUE déclarée par une arme/puce (ou effet passif) : ce que l'item PEUT faire
# (dégâts, poison, téléport, inversion...). Potentiel, pas encore appliqué -> distinct d'Effect
# (actif sur une entité). Brut = [type, minValue, maxValue, turns, targets, modifiers].
class Feature:
    def __init__(self, raw): self.raw = raw
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


class Entity:
    def __init__(self, id): self.id = id
    @property
    def life(self): return getLife(self.id)
    @property
    def maxLife(self): return getTotalLife(self.id)
    @property
    def tp(self): return getTP(self.id)
    @property
    def maxTP(self): return getTotalTP(self.id)
    @property
    def mp(self): return getMP(self.id)
    @property
    def maxMP(self): return getTotalMP(self.id)
    @property
    def strength(self): return getStrength(self.id)
    @property
    def agility(self): return getAgility(self.id)
    @property
    def wisdom(self): return getWisdom(self.id)
    @property
    def resistance(self): return getResistance(self.id)
    @property
    def science(self): return getScience(self.id)
    @property
    def magic(self): return getMagic(self.id)
    @property
    def power(self): return getPower(self.id)
    @property
    def level(self): return getLevel(self.id)
    @property
    def name(self): return getName(self.id)
    @property
    def absoluteShield(self): return getAbsoluteShield(self.id)
    @property
    def relativeShield(self): return getRelativeShield(self.id)
    @property
    def cell(self): return Cell(getCell(self.id))
    @property
    def weapon(self): return _weap(getWeapon(self.id))
    @property
    def weapons(self): return _weaps(getWeapons(self.id))
    @property
    def chips(self): return _chps(getChips(self.id))
    @property
    def effects(self): return _effs(getEffects(self.id))
    @property
    def launchedEffects(self): return _effs(getLaunchedEffects(self.id))
    @property
    def passiveEffects(self): return _feats(getPassiveEffects(self.id))
    @property
    def states(self): return getStates(self.id)
    @property
    def summons(self): return _ents(getSummons(self.id))
    @property
    def summoner(self): return _ent(getSummoner(self.id))
    @property
    def summoned(self): return isSummon(self.id)
    @property
    def alive(self): return isAlive(self.id)
    @property
    def dead(self): return isDead(self.id)
    def isAlly(self): return isAlly(self.id)
    def isEnemy(self): return isEnemy(self.id)
    def distance(self, target): return getCellDistance(getCell(self.id), _cid(target))


class Me(Entity):
    def __init__(self): super().__init__(-1)  # id jetable : la property ci-dessous le rend dynamique
    # me suit l'entite COURANTE. Pendant le tour d'un bulbe (summon), BulbAI rebascule mEntity de l'IA
    # invocatrice sur le bulbe -> me.id doit renvoyer le bulbe. Setter no-op OBLIGATOIRE (Entity.__init__
    # fait self.id = id). Property posee SEULEMENT sur Me : Entity/Cell/Weapon/Chip gardent un id fige.
    @property
    def id(self): return getEntity()
    @id.setter
    def id(self, value): pass
    def moveToward(self, target):
        return moveTowardCell(target.id) if isinstance(target, Cell) else moveToward(_eid(target))
    def moveAwayFrom(self, target):
        return moveAwayFromCell(target.id) if isinstance(target, Cell) else moveAwayFrom(_eid(target))
    def useWeapon(self, target): return useWeapon(_eid(target))
    def useWeaponOnCell(self, cell): return useWeaponOnCell(_cid(cell))
    def useChip(self, chip, target): return useChip(_cpid(chip), _eid(target))
    def useChipOnCell(self, chip, cell): return useChipOnCell(_cpid(chip), _cid(cell))
    def setWeapon(self, weapon): return setWeapon(_wid(weapon))
    def say(self, message): return say(message)
    def canUseWeapon(self, target): return canUseWeapon(_eid(target))
    def canUseChip(self, chip, target): return canUseChip(_cpid(chip), _eid(target))
    def resurrect(self, target, cell): return resurrect(_eid(target), _cid(cell))
    # Invoque un bulbe : callback = fonction guest rejouee a chaque tour du bulbe (me/getEntity() = bulbe).
    def summon(self, chip, cell, callback, name=None):
        return summon(_cpid(chip), _cid(cell), callback) if name is None else summon(_cpid(chip), _cid(cell), callback, name)


# Sous-types d'entite : _ent() renvoie l'instance TYPEE selon getType() -> isinstance(x, Mob) marche.
# Ceux qui ont une sous-categorie exposent leur propre .type (chest.type == Chest.Type.WOOD).
class Leek(Entity): pass
class Turret(Entity): pass
class Bulb(Entity):
    @property
    def type(self): return getBulbType(self.id)
class Chest(Entity):
    @property
    def type(self): return getChestType(self.id)
class Mob(Entity):
    @property
    def type(self): return getMobType(self.id)


class _Fight:
    @property
    def turn(self): return getTurn()
    def getNearestEnemy(self): return _ent(getNearestEnemy())
    def getNearestAlly(self): return _ent(getNearestAlly())
    def getFarthestEnemy(self): return _ent(getFarthestEnemy())
    def getFarthestAlly(self): return _ent(getFarthestAlly())
    def getNearestEnemyTo(self, target): return _ent(getNearestEnemyTo(_eid(target)))
    def getNearestAllyTo(self, target): return _ent(getNearestAllyTo(_eid(target)))
    def getEnemies(self): return _ents(getEnemies())
    def getAllies(self): return _ents(getAllies())
    def getAliveEnemies(self): return _ents(getAliveEnemies())
    def getAliveAllies(self): return _ents(getAliveAllies())
    def getDeadEnemies(self): return _ents(getDeadEnemies())
    def getDeadAllies(self): return _ents(getDeadAllies())
    def getEnemiesCount(self): return getEnemiesCount()
    def getAlliesCount(self): return getAlliesCount()
    def getAliveEnemiesCount(self): return getAliveEnemiesCount()
    def getAliveAlliesCount(self): return getAliveAlliesCount()
    def getAlliedTurret(self): return _ent(getAlliedTurret())
    def getEnemyTurret(self): return _ent(getEnemyTurret())


class _Field:
    @property
    def mapType(self): return getMapType()
    def cellFromXY(self, x, y):
        c = getCellFromXY(x, y)
        return None if c is None or c < 0 else Cell(c)
    def getObstacles(self): return _cells(getObstacles())
    def distance(self, a, b): return getDistance(_cid(a), _cid(b))
    def cellDistance(self, a, b): return getCellDistance(_cid(a), _cid(b))
    def pathLength(self, a, b): return getPathLength(_cid(a), _cid(b))
    def lineOfSight(self, a, b): return lineOfSight(_cid(a), _cid(b))


class _Registers:
    def get(self, key): return getRegister(key)
    def set(self, key, value): return setRegister(key, value)
    def delete(self, key): return deleteRegister(key)
    def all(self): return getRegisters()


class _Debug:
    def mark(self, cells, color=None, duration=None):
        if color is None: return mark(_cidlist(cells))
        if duration is None: return mark(_cidlist(cells), color)
        return mark(_cidlist(cells), color, duration)
    def markText(self, cells, text, color=None, duration=None):
        if color is None: return markText(_cidlist(cells), text)
        if duration is None: return markText(_cidlist(cells), text, color)
        return markText(_cidlist(cells), text, color, duration)
    def clearMarks(self): return clearMarks()
    def show(self, cell, color=None):
        return show(_cid(cell)) if color is None else show(_cid(cell), color)
    def pause(self): return pause()


Fight = _Fight()
Field = _Field()
Registers = _Registers()
Debug = _Debug()
me = Me()


# Conteneur des constantes d'etat special (State.UNHEALABLE...).
class State: pass


# Namespace vide pour les sous-conteneurs (Fight.Type, Item.LaunchType...).
class _NS: pass


# Range les constantes plates du bridge (WEAPON_*, EFFECT_*, STATE_*...) en membres OBJET, par
# famille : ITEMS (WEAPON_/CHIP_) -> INSTANCES poolees camelCase (Weapon.pistol), le reste ->
# CATEGORIES en MAJUSCULES (Effect.DAMAGE, State.UNHEALABLE, Fight.Type.SOLO...). Les globales plates
# restent disponibles. Prefixes composes (LAUNCH_TYPE_...) listes AVANT les simples.
def _camel(s):
    parts = s.lower().split('_')
    return parts[0] + ''.join(p.capitalize() for p in parts[1:])

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
    ('MESSAGE_', 'cat', Fight, 'Message'),
    ('MAP_', 'cat', Field, None),
    ('EFFECT_', 'cat', Effect, None),
    ('STATE_', 'cat', State, None),
]

def _sub(container, name):
    ns = getattr(container, name, None)
    if ns is None:
        ns = _NS()
        setattr(container, name, ns)
    return ns

for _k in list(globals().keys()):
    for _p, _mode, _c, _extra in _RULES:
        if _k.startswith(_p):
            try:
                _name = _k[len(_p):]
                if _mode == 'item':
                    setattr(_c, _camel(_name), _extra(globals()[_k]))
                else:
                    _box = _c if _extra is None else _sub(_c, _extra)
                    setattr(_box, _name, globals()[_k])
            except Exception:
                pass  # nom reserve : on saute
            break
