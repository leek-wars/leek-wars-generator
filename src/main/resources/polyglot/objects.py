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
def _ent(i): return None if i is None or i < 0 else Entity(i)
def _ents(ids): return [Entity(i) for i in (ids or [])]
def _cells(ids): return [Cell(i) for i in (ids or [])]
def _weap(i): return None if i is None or i <= 0 else Weapon(i)
def _weaps(ids): return [Weapon(i) for i in (ids or [])]
def _chps(ids): return [Chip(i) for i in (ids or [])]
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


class Weapon:
    def __init__(self, id): self.id = id
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
    def effects(self): return _feats(getWeaponEffects(self.id))


class Chip:
    def __init__(self, id): self.id = id
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
    def effects(self): return _feats(getChipEffects(self.id))


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


# Effet DÉCLARÉ par une arme/puce ou effet passif : [type, minValue, maxValue, turns, targets, modifiers]
class EffectTemplate:
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
def _feats(arr): return [EffectTemplate(e) for e in (arr or [])]


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
    def __init__(self): super().__init__(getEntity())
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
