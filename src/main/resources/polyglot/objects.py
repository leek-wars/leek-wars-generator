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
    def effects(self): return getWeaponEffects(self.id)


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
    def effects(self): return getChipEffects(self.id)


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


Fight = _Fight()
Field = _Field()
me = Me()
