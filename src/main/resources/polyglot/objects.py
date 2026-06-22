# API de combat ORIENTÉE OBJET pour les IA polyglot Python (style stdlib objet LeekScript v5).
# Couche guest au-dessus de l'API plate déjà bridgée : me.useWeapon(enemy) -> useWeapon(enemy.id).
# Évaluée dans chaque contexte Python après le bridge (cf PolyglotEntityAI). Propriétés = lecture
# d'état, méthodes = actions/calculs. Tranche 1 : Cell, Entity, me + accès minimal aux entités (Fight).
# L'API plate reste valide (coexistence) ; l'objet deviendra la forme idiomatique.

def _cid(x):
    if isinstance(x, Cell):
        return x.id
    if isinstance(x, Entity):
        return getCell(x.id)
    return x

def _eid(x):
    return x.id if isinstance(x, Entity) else x

def _ent(i):
    return None if i is None or i < 0 else Entity(i)

def _ents(ids):
    return [Entity(i) for i in (ids or [])]


class Cell:
    def __init__(self, id):
        self.id = id

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


class Entity:
    def __init__(self, id):
        self.id = id

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
    def weapon(self): return getWeapon(self.id)
    @property
    def alive(self): return isAlive(self.id)
    @property
    def dead(self): return isDead(self.id)

    def isAlly(self): return isAlly(self.id)
    def isEnemy(self): return isEnemy(self.id)
    def distance(self, target): return getCellDistance(getCell(self.id), _cid(target))


class Me(Entity):
    def __init__(self):
        super().__init__(getEntity())

    def moveToward(self, target):
        return moveTowardCell(target.id) if isinstance(target, Cell) else moveToward(_eid(target))
    def moveAwayFrom(self, target):
        return moveAwayFromCell(target.id) if isinstance(target, Cell) else moveAwayFrom(_eid(target))
    def useWeapon(self, target): return useWeapon(_eid(target))
    def useWeaponOnCell(self, cell): return useWeaponOnCell(_cid(cell))
    def useChip(self, chip, target): return useChip(chip, _eid(target))
    def useChipOnCell(self, chip, cell): return useChipOnCell(chip, _cid(cell))
    def setWeapon(self, weapon): return setWeapon(weapon)
    def say(self, message): return say(message)
    def canUseWeapon(self, target): return canUseWeapon(_eid(target))
    def canUseChip(self, chip, target): return canUseChip(chip, _eid(target))


class _Fight:
    @property
    def turn(self): return getTurn()
    def getNearestEnemy(self): return _ent(getNearestEnemy())
    def getNearestAlly(self): return _ent(getNearestAlly())
    def getEnemies(self): return _ents(getEnemies())
    def getAllies(self): return _ents(getAllies())
    def getAliveEnemies(self): return _ents(getAliveEnemies())
    def getAliveAllies(self): return _ents(getAliveAllies())


Fight = _Fight()
me = Me()
