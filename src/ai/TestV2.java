package ai;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.evaluation.EvaluationFunction;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rts.GameState;
import rts.PhysicalGameState;
import static rts.PhysicalGameState.TERRAIN_WALL;
import rts.Player;
import rts.PlayerAction;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.UnitActionAssignment;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class TestV2 extends AIWithComputationBudget{
    
    
    UnitTypeTable utt = null;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType heavyType;
    UnitType lightType;
    UnitType rangedType;
    UnitType resourceType;

    List<UnitType> barracksUnits;

    List<Unit> workersAllay;
    List<Unit> workersEnemy;
    List<Unit> basesAllay;
    List<Unit> basesEnemy;
    List<Unit> barracksAllay;
    List<Unit> barracksEnemy;
    List<Unit> heavyAllay;
    List<Unit> heavyEnemy;
    List<Unit> lightAllay;
    List<Unit> lightEnemy;
    List<Unit> rangedAllay;
    List<Unit> rangedEnemy;
    List<Unit> resourcesReachable;
    List<Unit> resourcesAll;
    List<Unit> myHarvestWorkers;
    List<Unit> allayUnits;
    List<Unit> enemyUnits;

    List<Unit> barracksUnderConstruction;
    List<Unit> basesUnderConstruction;

    int maxBases = 1;
    int maxBarracks = 1;
    int maxWorkers = 5;
    int maxHarvestWorkers = 1;
    int harvestCoef = 2;

    GameState gameState;
    PhysicalGameState physicalGameState;
    Player player;
    Player enemyPlayer;
    PlayerAction playerAction;
    int resourcesUsed;

    List<Integer> actionDirs;
    List<Integer> locationsTaken;

    AStarPathFinding astarPath;

    int NoDirection = 100;
    int w;
    int h;
    long startCycleMilli;
    long latestTsMilli;

    Pos rallyPoint;

    HashMap<Long, Unit> dyingEnemies = new HashMap<>();

    public void resetPathFind() {
        astarPath = new AStarPathFinding();
    }

    public TestV2(UnitTypeTable a_utt) {
        super(-1, -1);
        utt = a_utt;
        resetPathFind(); //FloodFillPathFinding(); //AStarPathFinding();

        actionDirs = new ArrayList<>();
        actionDirs.add(UnitAction.DIRECTION_UP);
        actionDirs.add(UnitAction.DIRECTION_RIGHT);
        actionDirs.add(UnitAction.DIRECTION_DOWN);
        actionDirs.add(UnitAction.DIRECTION_LEFT);

        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        heavyType = utt.getUnitType("Heavy");
        lightType = utt.getUnitType("Light");
        rangedType = utt.getUnitType("Ranged");
        resourceType = utt.getUnitType("Resource");
    }

    void init(){
        w = physicalGameState.getWidth();
        h = physicalGameState.getHeight();

        workersAllay = new ArrayList<>();
        workersEnemy = new ArrayList<>();
        basesAllay = new ArrayList<>();
        basesEnemy = new ArrayList<>();
        barracksAllay = new ArrayList<>();
        barracksEnemy = new ArrayList<>();
        heavyAllay = new ArrayList<>();
        heavyEnemy = new ArrayList<>();
        lightAllay = new ArrayList<>();
        lightEnemy = new ArrayList<>();
        rangedAllay = new ArrayList<>();
        rangedEnemy = new ArrayList<>();
        resourcesReachable = new ArrayList<>();
        resourcesAll = new ArrayList<>();
        allayUnits = new ArrayList<>();
        enemyUnits = new ArrayList<>();

        myHarvestWorkers = new ArrayList<>();

        locationsTaken = new ArrayList<>();
        
        barracksUnderConstruction = new ArrayList<>();
        basesUnderConstruction = new ArrayList<>();

        barracksUnits = new ArrayList<>();

        for (Unit u : physicalGameState.getUnits()) {
            locationsTaken.add(u.getX() + u.getY() * physicalGameState.getWidth());
            if (u.getType() == workerType) {
                if (u.getPlayer() == player.getID()) {
                    workersAllay.add(u);
                    allayUnits.add(u);
                    if(gameState.getUnitAction(u) != null){
                        if(gameState.getUnitAction(u).getType() == UnitAction.TYPE_PRODUCE){
                            if(gameState.getUnitAction(u).getUnitType() == baseType)
                                basesUnderConstruction.add(u);
                            else if(gameState.getUnitAction(u).getUnitType() == barracksType){
                                barracksUnderConstruction.add(u);
                            }
                        }
                    }
                } else {
                    workersEnemy.add(u);
                    enemyUnits.add(u);
                }
            } else if (u.getType() == baseType) {
                if (u.getPlayer() == player.getID()) {
                    basesAllay.add(u);
                    allayUnits.add(u);
                } else {
                    basesEnemy.add(u);
                    enemyUnits.add(u);
                }
            } else if (u.getType() == barracksType) {
                if (u.getPlayer() == player.getID()) {
                    barracksAllay.add(u);
                    allayUnits.add(u);
                } else {
                    barracksEnemy.add(u);
                    enemyUnits.add(u);
                }
            } else if (u.getType() == heavyType) {
                if (u.getPlayer() == player.getID()) {
                    heavyAllay.add(u);
                    allayUnits.add(u);
                } else {
                    heavyEnemy.add(u);
                    enemyUnits.add(u);
                }
            } else if (u.getType() == lightType) {
                if (u.getPlayer() == player.getID()) {
                    lightAllay.add(u);
                } else {
                    lightEnemy.add(u);
                    enemyUnits.add(u);
                }
            } else if (u.getType() == rangedType) {
                if (u.getPlayer() == player.getID()) {
                    rangedAllay.add(u);
                    allayUnits.add(u);
                } else {
                    rangedEnemy.add(u);
                    enemyUnits.add(u);
                }
            } else if (u.getType() == resourceType) {
                resourcesAll.add(u);
            }
        }
        for(Unit resource : resourcesAll){
            // if resource is near any base, add it to resourcesReachable, range 4
            int range = w;
            for(Unit base : basesAllay){
                if(distance(resource, base) <= range){
                    resourcesReachable.add(resource);
                    break;
                }
            }
        }

        //maxbases is half of allayresources and at least 1
        maxBases = Math.max(resourcesAll.size() / 4, 1);
        //maxbarracks is same as maxbases
        maxBarracks = maxBases;
        maxHarvestWorkers = 2*basesAllay.size();
        if(w==8){
            maxBarracks = 0;
            maxWorkers = 100;
            if(workersAllay.size()>4){
                maxBarracks = basesAllay.size();
            }
            if(barracksAllay.size()>0){
                maxWorkers = 2*basesAllay.size();
            }
        }
        else if(w==9){
            maxBarracks = basesAllay.size();
            maxHarvestWorkers = 3;
            maxWorkers = 4;
        }
        else if(w==64){
            maxBarracks = basesAllay.size();
            maxBases = 3;
            maxHarvestWorkers = 4;
            maxWorkers = 2*basesAllay.size()+1;
        }
        else if(w==32){
            maxBarracks = basesAllay.size();
            maxHarvestWorkers = 3;
            maxWorkers = maxHarvestWorkers+1;
        }

        //if enemy don't have barracks, barracksunit for each barracks is light
        //if enemy have barracks,but don't have combat units, barracksunit for each barracks is ranged
        //for each barracks find closest enemy unit and if it is heavy change barracksunit to ranged
        //for each barracks find closest enemy unit and if it is ranged change barracksunit to light
        //for each barracks find closest enemy unit and if it is light change barracksunit to heavy
        if(barracksEnemy.isEmpty()){
            barracksUnits = new ArrayList<>();
            for(int i = 0; i < barracksAllay.size(); i++){
                if(w==8){
                    barracksUnits.add(heavyType);
                }
                else if(w==16){
                    barracksUnits.add(heavyType);
                }
                else if(w==32){
                    barracksUnits.add(heavyType);
                }
                else{
                    barracksUnits.add(rangedType);
                }
            }
        }
        if(w==8){
            barracksUnits = new ArrayList<>();
            for(int i = 0; i < barracksAllay.size(); i++){
                barracksUnits.add(heavyType);
            }
        }
        else{
            barracksUnits = new ArrayList<>();
            for(Unit barracks : barracksAllay){
                Unit closestEnemy = getClosestUnit(barracks, enemyUnits);
                if(closestEnemy != null){
                    if(closestEnemy.getType() == heavyType){
                        if(w==8){
                            barracksUnits.add(heavyType);
                        }
                        else if(w==16){
                            barracksUnits.add(heavyType);
                        }
                        else if(w==32){
                            barracksUnits.add(heavyType);
                        }
                        else{
                            barracksUnits.add(rangedType);
                        }
                    }
                    else if(closestEnemy.getType() == rangedType){
                        barracksUnits.add(lightType);
                    }
                    else if(closestEnemy.getType() == lightType){
                        barracksUnits.add(heavyType);
                    }
                    else{
                        barracksUnits.add(rangedType);
                    }
                }
                else{
                    if(w==8){
                        barracksUnits.add(heavyType);
                    }
                    else if(w==16){
                        barracksUnits.add(heavyType);
                    }
                    else if(w==32){
                        barracksUnits.add(heavyType);
                    }
                    else{
                        barracksUnits.add(rangedType);
                    }
                }
            }
        }

        actionDirs = new ArrayList<>();
        actionDirs.add(UnitAction.DIRECTION_UP);
        actionDirs.add(UnitAction.DIRECTION_DOWN);
        actionDirs.add(UnitAction.DIRECTION_LEFT);
        actionDirs.add(UnitAction.DIRECTION_RIGHT);

        resourcesUsed = 0;

        initTimeLimit();
    }

    boolean willEscapeAttack(Unit attacker, Unit runner) {
        UnitActionAssignment aa = gameState.getActionAssignment(runner);
        if (aa == null)
            return false;
        if (aa.action.getType() != UnitAction.TYPE_MOVE)
            return false;
        int eta = aa.action.ETA(runner) - (gameState.getTime() - aa.time);
        return eta <= attacker.getAttackTime();
    }

    boolean isInRange(Unit u, Unit target) {
        int d = Math.abs(u.getX() - target.getX()) + Math.abs(u.getY() - target.getY());
        return d <= u.getType().attackRange;
    }

    int distance(Unit u1, Unit u2) {
        return Math.abs(u1.getX() - u2.getX()) + Math.abs(u1.getY() - u2.getY());
    }

    int distance(Unit u, Pos pos) {
        return Math.abs(u.getX() - pos.getX()) + Math.abs(u.getY() - pos.getY());
    }

    int distance(Pos pos1, Pos pos2) {
        return Math.abs(pos1.getX() - pos2.getX()) + Math.abs(pos1.getY() - pos2.getY());
    }

    Unit getClosestUnit(Unit u, List<Unit> units) {
        if (units.isEmpty()) {
            return null;
        }
        Unit closestUnit = null;
        int closestDistance = 1000;
        for (Unit u2 : units) {
            int d = distance(u, u2);
            if(d == 0){
                continue;
            }

            if (closestUnit == null || d < closestDistance) {
                closestUnit = u2;
                closestDistance = d;
            }
        }
        return closestUnit;
    }

    Unit getCloseUnit(Pos pos, List<Unit> units){
        if(units.isEmpty()){
            return null;
        }
        Unit closestUnit = null;
        int closestDistance = 0;
        for(Unit u : units){
            int d = Math.abs(u.getX() - pos.getX()) + Math.abs(u.getY() - pos.getY());
            if(closestUnit == null || d < closestDistance){
                closestUnit = u;
                closestDistance = d;
            }
        }
        return closestUnit;
    }

    Pos findPosBuildBarracks(){
        List<Pos> possiblePos = new ArrayList<>();
        for(int i = 0; i < physicalGameState.getWidth(); i++){
            for(int j = 0; j < physicalGameState.getHeight(); j++){
                if(physicalGameState.getTerrain(i, j) == TERRAIN_WALL)
                    continue;
                possiblePos.add(new Pos(i, j));
            }
        }
        Pos bestPos = null;
        int bestScore = 0;
        for(Pos pos:possiblePos){
            int score = 0;
            //check if pos is taken
            int rasterPos = pos.getX() + pos.getY() * physicalGameState.getWidth();
            if(locationsTaken.contains(rasterPos)){
                score -= 10000;
            }
            else{
                //check if pos is near resource
                for(Unit resource : resourcesReachable){
                    int d = distance(resource, pos);
                    score -= 100/d;
                }
                //check if pos is near barracks
                for(Unit barracks : barracksAllay){
                    int d = distance(barracks, pos);
                    score -= 100/d;
                }
                //check if pos is near base
                for(Unit base : basesAllay){
                    int d = distance(base, pos);
                    if(d == 1){
                        score -= 200;
                    }
                    else{
                        score += 10/d;
                    }
                }
                //if pos is next to boundary, score -= 100, if pos in corner, score -= 200
                if(pos.getX() == 0 || pos.getX() == w-1 || pos.getY() == 0 || pos.getY() == h-1){
                    score -= 10;
                }
                if((pos.getX() == 0 && pos.getY() == 0) || (pos.getX() == 0 && pos.getY() == h-1) || (pos.getX() == w-1 && pos.getY() == 0) || (pos.getX() == w-1 && pos.getY() == h-1)){
                    score -= 200;
                }

                //check if pos is near enemy
                for(Unit enemy : enemyUnits){
                    int d = distance(enemy, pos);
                    if(d<=8)
                    {
                        score -= 100/d;
                    }
                    else
                    {
                        score -= 50/d;
                    }
                }

                Unit closestWorker = getCloseUnit(pos, workersAllay);
                score -= 10*distance(closestWorker, pos);
            }
            if(bestPos == null || score > bestScore){
                bestPos = pos;
                bestScore = score;
            }            
        }
        return bestPos;
    }

    Pos findPosBulidBase(){
        List<Pos> possiblePos = new ArrayList<>();
        //find all possible positions
        //possible positions need to be near any resource,range 4
        for(int i = 0; i < physicalGameState.getWidth(); i++){
            for(int j = 0; j < physicalGameState.getHeight(); j++){
                if(physicalGameState.getTerrain(i, j) == TERRAIN_WALL)
                    continue;
                Pos pos = new Pos(i, j);
                boolean nearResource = false;
                for(Unit resource : resourcesAll){
                    if(distance(resource, pos) <= 2){
                        nearResource = true;
                        break;
                    }
                }
                if(nearResource){
                    possiblePos.add(pos);
                }
            }
        }
        Pos bestPos = null;
        int bestScore = 0;
        for(Pos pos:possiblePos){
            int score = 0;
            //check if pos is taken
            int rasterPos = pos.getX() + pos.getY() * physicalGameState.getWidth();
            if(locationsTaken.contains(rasterPos)){
                score -= 10000;
            }
            else{
                //check if pos is near resource
                for(Unit resource : resourcesReachable){
                    int d = distance(resource, pos);
                    if(d == 1){
                        score -= 1000;
                    }
                    else{
                        score += 20/d;
                    }

                }
                //check if pos is near base
                for(Unit base : basesAllay){
                    if(distance(base, pos) <= 1){
                        score -= 10;
                    }
                }
                //check if pos is near barracks
                for(Unit barracks : barracksAllay){
                    if(distance(barracks, pos) <= 1){
                        score -= 10;
                    }
                }
                //check if pos is near enemy
                for(Unit enemy : enemyUnits){
                    int d = distance(enemy, pos);
                    score -= 200/d;
                }

                Unit closestWorker = getCloseUnit(pos, workersAllay);
                score -= 10*distance(closestWorker, pos);
            }
            if(bestPos == null || score > bestScore){
                bestPos = pos;
                bestScore = score;
            }            
        }
        return bestPos;
    }

    boolean busy(Unit u) {
        if(playerAction.getAction(u) != null)
            return true;
        UnitActionAssignment aa = gameState.getActionAssignment(u);
        return aa != null;
    }

    void initTimeLimit() {
        startCycleMilli = 0;
        latestTsMilli = 0;
        
        //kinda random, do not want to take time unnecessarily
        if (physicalGameState.getWidth() < 24 || physicalGameState.getUnits().size() < 24)
            return;
        
        startCycleMilli = System.currentTimeMillis();
        latestTsMilli = startCycleMilli;
    }

    long timeRemaining(boolean updateTs) {
        int perCycleTime = 100;
        if (startCycleMilli == 0)
            return perCycleTime;
        
        if (updateTs)
            latestTsMilli = System.currentTimeMillis();
        
        return perCycleTime - (latestTsMilli - startCycleMilli);
    }

    public static class Pos {
        int x;
        int y;

        public Pos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public List<Pos> adjacentPos() {
            return Stream.of(
                    new Pos(x, y + 1),
                    new Pos(x, y - 1),
                    new Pos(x + 1, y),
                    new Pos(x - 1, y)
            ).collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return "Position{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pos position = (Pos) o;
            return x == position.x &&
                    y == position.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    Pos futurePos(int x, int y, int dir) {
        int nx = x;
        int ny = y;
        if (dir == UnitAction.DIRECTION_DOWN)
            ny = (ny == physicalGameState.getHeight() - 1) ? ny : ny + 1;
        else if (dir == UnitAction.DIRECTION_UP)
            ny = (ny == 0) ? ny : ny - 1;
        else if (dir == UnitAction.DIRECTION_RIGHT)
            nx = (nx == physicalGameState.getWidth() - 1) ? nx : nx + 1;
        else if (dir == UnitAction.DIRECTION_LEFT)
            nx = (nx == 0) ? nx : nx - 1;
        return new Pos(nx, ny);
    }

    ResourceUsage fullResourceUse() {
        ResourceUsage ru = gameState.getResourceUsage().clone();
        ru.merge(gameState.getResourceUsage());
        
        for (Integer pos : locationsTaken) {
            int x = pos % physicalGameState.getWidth();
            int y = pos / physicalGameState.getWidth();
            Unit u = new Unit(0, utt.getUnitType("Worker"), x, y);
            UnitAction a = null;
            if (x > 0)
                a = new UnitAction(UnitAction.TYPE_MOVE, NoDirection);
            else
                a = new UnitAction(UnitAction.TYPE_MOVE, NoDirection);
            UnitActionAssignment uaa = new UnitActionAssignment(u, a, 0);
            ru.merge(uaa.action.resourceUsage(u, physicalGameState));
        }
        return ru;
    }

    boolean availablePos(Pos pos) {
        int rasterPos = pos.getX() + pos.getY() * physicalGameState.getWidth();
        if (pos.getX() < 0 || pos.getX() >= physicalGameState.getWidth() || pos.getY() < 0 || pos.getY() >= physicalGameState.getHeight())
            return false;
        if (physicalGameState.getTerrain(pos.getX(), pos.getY()) == TERRAIN_WALL)
            return false;
        if (locationsTaken.contains(rasterPos))
            return false;
        return true;
    }

    int scoreBSF(Pos startPos, Pos targetPos,int maxDepth) {
        int score = distance(startPos, targetPos);
        if(maxDepth>0){
            List<Pos> nextPos = startPos.adjacentPos();
            for(Pos fpos:nextPos){
                if(!availablePos(fpos)){
                    score += w+h+10;
                }
                else{
                    score += scoreBSF(fpos, targetPos, maxDepth-1);
                }
            }
        }
        return score;
    }

    int findMoveBFS(Pos startPos, Pos targetPos, int maxDepth) {
        List<Pos> nextPos = startPos.adjacentPos();
        Pos bestPos = null;
        int lowestScore = 10000;
        for(Pos fpos:nextPos){
            if(!availablePos(fpos)){
                continue;
            }
            int score = scoreBSF(fpos, targetPos, maxDepth);
            if(bestPos == null || score < lowestScore){
                bestPos = fpos;
                lowestScore = score;
            }
        }
        if(bestPos == null){
            return -1;
        }
        return getDir(startPos, bestPos);
    }

    int findMoveDir(Unit u, int pos_x, int pos_y) {
        int pos = pos_x + pos_y * physicalGameState.getWidth();
        UnitAction move = astarPath.findPathToAdjacentPosition(u, pos, gameState, fullResourceUse());
        boolean available = true;
        int dir = -1;
        if(move == null){
            available = false;
        }
        else{
            if (!gameState.isUnitActionAllowed(u, move)){
                available = false;
            }
            Pos futPos = futurePos(u.getX(), u.getY(), move.getDirection());
            int fPos = futPos.getX() + futPos.getY() * physicalGameState.getWidth();
            if (locationsTaken.contains(fPos)){
                available = false;
            }
            if(available){
                dir = move.getDirection();
                locationsTaken.add(fPos);
            }
        
        }
        // if not available try other directions
        if(!available){
            dir = findMoveBFS(new Pos(u.getX(), u.getY()), new Pos(pos_x, pos_y), 3);
            if(dir == -1){
                List<Integer> possibleDirs = new ArrayList<>();
                int newDir = 0;
                if(move != null)
                    dir = move.getDirection();
                for(int i = 0; i < 4; i++){
                    newDir = (newDir + 1) % 4;
                    UnitAction newMove = new UnitAction(UnitAction.TYPE_MOVE, newDir);
                    if (!gameState.isUnitActionAllowed(u, newMove))
                        continue;
                    Pos newFutPos = futurePos(u.getX(), u.getY(), newMove.getDirection());
                    int newFPos = newFutPos.getX() + newFutPos.getY() * physicalGameState.getWidth();
                    if (locationsTaken.contains(newFPos))
                        continue;
                    possibleDirs.add(newDir);
                }
                if(possibleDirs.isEmpty()){
                    return -1;
                }
                else{
                    //randomize possibleDirs
                    Collections.shuffle(possibleDirs);
                    dir = possibleDirs.get(0);
                    UnitAction newMove = new UnitAction(UnitAction.TYPE_MOVE, dir);
                    Pos newFutPos = futurePos(u.getX(), u.getY(), newMove.getDirection());
                    int newFPos = newFutPos.getX() + newFutPos.getY() * physicalGameState.getWidth();
                    locationsTaken.add(newFPos);
                }
            }
        }
        return dir;
    }

    //get dir if unit is adjacent to pos
    int getDir(Unit u, int pos_x, int pos_y) {
        int dx = u.getX() - pos_x;
        int dy = u.getY() - pos_y;
        if (dx == 0 && dy == 1)
            return UnitAction.DIRECTION_UP;
        else if (dx == 0 && dy == -1)
            return UnitAction.DIRECTION_DOWN;
        else if (dx == 1 && dy == 0)
            return UnitAction.DIRECTION_LEFT;
        else if (dx == -1 && dy == 0)
            return UnitAction.DIRECTION_RIGHT;
        return NoDirection;
    }

    int getDir(Pos pos1, Pos pos2) {
        int dx = pos1.getX() - pos2.getX();
        int dy = pos1.getY() - pos2.getY();
        if (dx == 0 && dy == 1)
            return UnitAction.DIRECTION_UP;
        else if (dx == 0 && dy == -1)
            return UnitAction.DIRECTION_DOWN;
        else if (dx == 1 && dy == 0)
            return UnitAction.DIRECTION_LEFT;
        else if (dx == -1 && dy == 0)
            return UnitAction.DIRECTION_RIGHT;
        return NoDirection;
    }

    boolean attackUnitInRange(Unit u){
        List<Unit> enemiesInRange = new ArrayList<>();
        for(Unit enemy : enemyUnits){
            if(isInRange(u, enemy)){
                if(willEscapeAttack(u, enemy)){
                    continue;
                }
                //if enemy in dyingEnemies, skip it
                if(dyingEnemies.containsKey(enemy.getID())){
                    continue;
                }
                enemiesInRange.add(enemy);
            }
        }
        if(enemiesInRange.isEmpty()){
            return false;
        }
        Unit lowestHPEnemy = null;
        int lowestHP = 1000;
        for(Unit enemy : enemiesInRange){
            if(enemy.getHitPoints() < lowestHP){
                lowestHP = enemy.getHitPoints();
                lowestHPEnemy = enemy;
            }
        }
        if(lowestHPEnemy != null){
            playerAction.addUnitAction(u, new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, lowestHPEnemy.getX(), lowestHPEnemy.getY()));
            if(lowestHPEnemy.getHitPoints() <= u.getMaxDamage()){
                dyingEnemies.put(lowestHPEnemy.getID(), lowestHPEnemy);
            }
            return true;
        }
        
        return false;
    }

    void workerAction(){
        if(workersAllay.isEmpty()){
            return;
        }

        myHarvestWorkers.clear();
        List<Unit> freeWorkers = new LinkedList<>();
        for(Unit worker : workersAllay){
            freeWorkers.add(worker);
        }

        for(int i = 0; i < workersAllay.size(); i++){
            Unit worker = workersAllay.get(i);
            if(busy(worker)){
                continue;
            }
            if(attackUnitInRange(worker)){
                //remove worker from freeWorkers
                freeWorkers.remove(worker);
                i--;
            }
        }

        //if an enemy is next to a base, send closest worker to attack it
        for(Unit base : basesAllay){
            for(Unit enemy : enemyUnits){
                if(distance(base, enemy) < 3){
                    Unit closestWorker = getClosestUnit(enemy, freeWorkers);
                    if(distance(enemy, base)>6){
                        continue;
                    }

                    if(closestWorker != null){
                        if(!attackUnitInRange(closestWorker)){
                            //move to enemy
                            int dir = findMoveDir(closestWorker, enemy.getX(), enemy.getY());
                            if(dir != -1){
                                playerAction.addUnitAction(closestWorker, new UnitAction(UnitAction.TYPE_MOVE, dir));
                                freeWorkers.remove(closestWorker);
                            }
                        }
                        else{
                            freeWorkers.remove(closestWorker);
                        }
                    }
                }
            }
        }

        //if resource is enough to bulid new barracks
        if(resourcesUsed + barracksType.cost <= player.getResources() && workersAllay.size() > 1 && freeWorkers.size() > 0){
            //if number of barracks is less than maxBarracks build new barracks
            if(barracksAllay.size()+barracksUnderConstruction.size() < maxBarracks){
                Pos pos = findPosBuildBarracks();
                Unit closestWorker = getCloseUnit(pos, freeWorkers);
                if(distance(closestWorker, pos)>1){
                    int dir = findMoveDir(closestWorker, pos.getX(), pos.getY());
                    if(dir != -1){
                        playerAction.addUnitAction(closestWorker, new UnitAction(UnitAction.TYPE_MOVE, dir));
                        freeWorkers.remove(closestWorker);
                    }
                }
                else{
                    int dir = getDir(closestWorker, pos.getX(), pos.getY());
                    playerAction.addUnitAction(closestWorker, new UnitAction(UnitAction.TYPE_PRODUCE, dir, barracksType));
                    freeWorkers.remove(closestWorker);
                    resourcesUsed += barracksType.cost;
                    barracksUnderConstruction.add(closestWorker);
                }
            }
        }

        //if resource is enough to bulid new base
        if(resourcesUsed + baseType.cost+10 <= player.getResources() && workersAllay.size() > 1 && freeWorkers.size() > 0){
            //if number of bases is less than maxBases build new base
            if(basesAllay.size()+basesUnderConstruction.size() < maxBases){
                Pos pos = findPosBulidBase();
                Unit closestWorker = getCloseUnit(pos, freeWorkers);
                if(distance(closestWorker, pos)>1){
                    int dir = findMoveDir(closestWorker, pos.getX(), pos.getY());
                    if(dir != -1){
                        playerAction.addUnitAction(closestWorker, new UnitAction(UnitAction.TYPE_MOVE, dir));
                        freeWorkers.remove(closestWorker);
                    }
                }
                else{
                    int dir = getDir(closestWorker, pos.getX(), pos.getY());
                    playerAction.addUnitAction(closestWorker, new UnitAction(UnitAction.TYPE_PRODUCE, dir, baseType));
                    freeWorkers.remove(closestWorker);
                    resourcesUsed += baseType.cost;
                    basesUnderConstruction.add(closestWorker);
                }
            }
        }

        //for each resouce find closet worker and add it to myHarvestWorkers until maxHarvestWorkers
        while(true){
            if(freeWorkers.isEmpty())
                break;
            if(resourcesReachable.isEmpty())
                break;
            if(myHarvestWorkers.size() >= maxHarvestWorkers)
                break;
            for(Unit resource : resourcesReachable){
                if(freeWorkers.isEmpty())
                    break;
                if(resourcesReachable.isEmpty())
                    break;
                if(myHarvestWorkers.size() >= maxHarvestWorkers)
                    break;
                Unit closestWorker = getClosestUnit(resource, freeWorkers);
                if(closestWorker != null){
                    myHarvestWorkers.add(closestWorker);
                    freeWorkers.remove(closestWorker);
                }
            }   
        }

        if(myHarvestWorkers.size()==0){
            //add worker carrying resources to harvest workers
            for(Unit worker : freeWorkers){
                if(worker.getResources() > 0){
                    myHarvestWorkers.add(worker);
                    freeWorkers.remove(worker);
                    break;
                }
            }
        }

        for(Unit harvestWorker : myHarvestWorkers){
            if(busy(harvestWorker)){
                continue;
            }
            // if worker carry resources move to base
            if(harvestWorker.getResources() > 0){
                Unit closestBase = getClosestUnit(harvestWorker, basesAllay);
                if(closestBase != null){
                    if(distance(closestBase, harvestWorker) > 1)
                    {
                        int dir = findMoveDir(harvestWorker, closestBase.getX(), closestBase.getY());
                        if(dir != -1){
                            playerAction.addUnitAction(harvestWorker, new UnitAction(UnitAction.TYPE_MOVE, dir));
                            continue;
                        }
                    }
                    else{
                        int dir = getDir(harvestWorker, closestBase.getX(), closestBase.getY());
                        playerAction.addUnitAction(harvestWorker, new UnitAction(UnitAction.TYPE_RETURN, dir));
                        continue;
                    }
                }
                
            }
            else{
                // if worker is not carrying resources find closest resource
                Unit closestResource = getClosestUnit(harvestWorker, resourcesReachable);
                if(closestResource != null){
                    if(distance(closestResource, harvestWorker) > 1)
                    {
                        int dir = findMoveDir(harvestWorker, closestResource.getX(), closestResource.getY());
                        if(dir != -1){
                            playerAction.addUnitAction(harvestWorker, new UnitAction(UnitAction.TYPE_MOVE, dir));
                            continue;
                        }
                    }
                    else{
                        int dir = getDir(harvestWorker, closestResource.getX(), closestResource.getY());
                        playerAction.addUnitAction(harvestWorker, new UnitAction(UnitAction.TYPE_HARVEST, dir));
                        continue;
                    }
                }
            }
        }

        //for rest of workers move to closest enemy unit and attack it
        for(Unit worker : freeWorkers){
            if(busy(worker)){
                continue;
            }
            Unit closestEnemy = getClosestUnit(worker, workersEnemy);
            if(closestEnemy != null){
                if(!attackUnitInRange(worker)){
                    //if not in range move towards closest enemy
                    int dir = findMoveDir(worker, closestEnemy.getX(), closestEnemy.getY());
                    if(dir != -1){
                        playerAction.addUnitAction(worker, new UnitAction(UnitAction.TYPE_MOVE, dir));
                        continue;
                    }
                }
                else{
                    freeWorkers.remove(worker);
                }
            }
            else{
                closestEnemy = getClosestUnit(worker, enemyUnits);
                if(closestEnemy != null){
                    if(!attackUnitInRange(worker)){
                        //if not in range move towards closest enemy
                        int dir = findMoveDir(worker, closestEnemy.getX(), closestEnemy.getY());
                        if(dir != -1){
                            playerAction.addUnitAction(worker, new UnitAction(UnitAction.TYPE_MOVE, dir));
                            continue;
                        }
                    }
                    else{
                        freeWorkers.remove(worker);
                    }
                }
            }
        }
    }

    void baseAction(){
        if(basesAllay.isEmpty()){
            return;
        }
        if(workersAllay.size()>=maxWorkers){
            return;
        }
        for(Unit base : basesAllay){
            if(busy(base)){
                continue;
            }
            if(resourcesUsed + workerType.cost <= player.getResources()){
                int highestScore = -100000;
                int highestScoreIndex = -1;
                List<UnitAction> possibleActions = new ArrayList<>();
                for(int i = 0; i < 4; i++){
                    int dir = i;
                    UnitAction possibelAction = new UnitAction(UnitAction.TYPE_PRODUCE, dir, workerType);
                    if(gameState.isUnitActionAllowed(base, possibelAction)){
                        possibleActions.add(possibelAction);
                    }
                }
                if(myHarvestWorkers.size() < maxHarvestWorkers){
                    for(UnitAction possibleAction:possibleActions){
                        //score for each possible action
                        int score = 0;
                        //check if pos is taken
                        Pos futPos = futurePos(base.getX(), base.getY(), possibleAction.getDirection());
                        int fPos = futPos.getX() + futPos.getY() * physicalGameState.getWidth();
                        if(locationsTaken.contains(fPos)){
                            score -= 10000;
                        }
                        else{
                            //check if pos is near resource
                            for(Unit resource : resourcesReachable){
                                int d = distance(resource, futPos);
                                score += 20/d;
                            }
                            //check if pos is near barracks
                            for(Unit barracks : barracksAllay){
                                if(distance(barracks, futPos) <= 1){
                                    score -= 10;
                                }
                            }
                            //check if pos is near worker
                            for(Unit worker : workersAllay){
                                if(distance(worker, futPos) <= 1){
                                    score -= 10;
                                }
                            }
                            //check if pos is near enemy
                            for(Unit enemy : enemyUnits){
                                int d = distance(enemy, futPos);
                                score -= 100/d;
                            }
                        }
                        if(score > highestScore){
                            highestScore = score;
                            highestScoreIndex = possibleActions.indexOf(possibleAction);
                        }
                    }
                    if(highestScoreIndex != -1){
                        playerAction.addUnitAction(base, possibleActions.get(highestScoreIndex));
                        resourcesUsed += workerType.cost;
                        continue;
                    }
                }
                else if(workersAllay.size()<maxWorkers){
                    for(UnitAction possibleAction:possibleActions){
                        //score for each possible action
                        int score = 0;
                        //check if pos is taken
                        Pos futPos = futurePos(base.getX(), base.getY(), possibleAction.getDirection());
                        int fPos = futPos.getX() + futPos.getY() * physicalGameState.getWidth();
                        if(locationsTaken.contains(fPos)){
                            score -= 10000;
                        }
                        else{
                            //check if pos is near resource
                            for(Unit resource : resourcesReachable){
                                int d = distance(resource, futPos);
                                score -= 20/d;
                            }
                            //check if pos is near barracks
                            for(Unit barracks : barracksAllay){
                                if(distance(barracks, futPos) <= 1){
                                    score -= 10;
                                }
                            }
                            //check if pos is near worker
                            for(Unit worker : workersAllay){
                                if(distance(worker, futPos) <= 1){
                                    score -= 10;
                                }
                            }
                            //check if pos is near enemy
                            for(Unit enemy : enemyUnits){
                                int d = distance(enemy, futPos);
                                score += 100/d;
                            }
                        }
                        if(score > highestScore){
                            highestScore = score;
                            highestScoreIndex = possibleActions.indexOf(possibleAction);
                        }
                    }
                    if(highestScoreIndex != -1){
                        playerAction.addUnitAction(base, possibleActions.get(highestScoreIndex));
                        resourcesUsed += workerType.cost;
                        continue;
                    }
                }
            }
        }
    }

    void barracksAction(){
        if(barracksAllay.isEmpty()){
            return;
        }
        for(int i = 0; i < barracksAllay.size(); i++){
            Unit barracks = barracksAllay.get(i);
            if(busy(barracks)){
                continue;
            }
            if(resourcesUsed + barracksUnits.get(i).cost <= player.getResources()){
                int highestScore = -100000;
                int highestScoreIndex = -1;
                List<UnitAction> possibleActions = new ArrayList<>();
                for(int j = 0; j < 4; j++){
                    int dir = j;
                    UnitAction possibelAction = new UnitAction(UnitAction.TYPE_PRODUCE, dir, barracksUnits.get(i));
                    if(gameState.isUnitActionAllowed(barracks, possibelAction)){
                        possibleActions.add(possibelAction);
                    }
                }
                for(UnitAction possibleAction:possibleActions){
                    //score for each possible action
                    int score = 0;
                    //check if pos is taken
                    Pos futPos = futurePos(barracks.getX(), barracks.getY(), possibleAction.getDirection());
                    int fPos = futPos.getX() + futPos.getY() * physicalGameState.getWidth();
                    if(locationsTaken.contains(fPos)){
                        score -= 10000;
                    }
                    else{
                        for(Unit resource : resourcesReachable){
                            int d = distance(resource, futPos);
                            score -= 20/d;
                        }
                        for(Unit base : basesAllay){
                            if(distance(base, futPos) <= 1){
                                score -= 10;
                            }
                        }
                        for(Unit worker : workersAllay){
                            if(distance(worker, futPos) <= 1){
                                score -= 10;
                            }
                        }
                        for(Unit enemy : enemyUnits){
                            int d = distance(enemy, futPos);
                            score += 100/d;
                        }
                    }
                    if(score > highestScore){
                        highestScore = score;
                        highestScoreIndex = possibleActions.indexOf(possibleAction);
                    }
                }
                if(highestScoreIndex != -1){
                    playerAction.addUnitAction(barracks, possibleActions.get(highestScoreIndex));
                    resourcesUsed += barracksUnits.get(i).cost;
                    continue;
                }
            }
        }
    }

    void rangedAction(){
        if(rangedAllay.isEmpty()){
            return;
        }
        for(Unit ranged:rangedAllay){
            if(busy(ranged)){
                continue;
            }
            if(attackUnitInRange(ranged)){
                continue;
            }


            Unit closestEenemy = getClosestUnit(ranged, enemyUnits);
            if(closestEenemy != null){
                if(distance(ranged, closestEenemy) <= 2){
                    //move to closest allay unit
                    //ally unit list beside this ranged
                    Unit closestAllay = getClosestUnit(ranged, allayUnits);
                    if(closestAllay != null){
                        int dir = findMoveDir(ranged, closestAllay.getX(), closestAllay.getY());
                        if(dir != -1){
                            playerAction.addUnitAction(ranged, new UnitAction(UnitAction.TYPE_MOVE, dir));
                            continue;
                        }
                    }
                }

                int dir = findMoveDir(ranged, closestEenemy.getX(), closestEenemy.getY());
                if(dir != -1){
                    playerAction.addUnitAction(ranged, new UnitAction(UnitAction.TYPE_MOVE, dir));
                }
            }
        }
    }

    void lightAction(){
        if(lightAllay.isEmpty()){
            return;
        }
        //move towards closest enemy and attack it
        for(Unit light:lightAllay){
            if(busy(light)){
                continue;
            }
            if(attackUnitInRange(light)){
                continue;
            }
            Unit closestRanged = getClosestUnit(light, rangedEnemy);
            if(closestRanged != null){
                int dir = findMoveDir(light, closestRanged.getX(), closestRanged.getY());
                if(dir != -1){
                    playerAction.addUnitAction(light, new UnitAction(UnitAction.TYPE_MOVE, dir));
                }
            }

            Unit closestWorker = getClosestUnit(light, workersEnemy);
            if(closestWorker != null){
                int dir = findMoveDir(light, closestWorker.getX(), closestWorker.getY());
                if(dir != -1){
                    playerAction.addUnitAction(light, new UnitAction(UnitAction.TYPE_MOVE, dir));
                }
            }

            Unit closestEenemy = getClosestUnit(light, enemyUnits);
            if(closestEenemy != null){
                int dir = findMoveDir(light, closestEenemy.getX(), closestEenemy.getY());
                if(dir != -1){
                    playerAction.addUnitAction(light, new UnitAction(UnitAction.TYPE_MOVE, dir));
                }
            }
        }
    }

    void heavyAction(){
        if(heavyAllay.isEmpty()){
            return;
        }
        //move towards closest enemy and attack it
        for(Unit heavy:heavyAllay){
            if(busy(heavy)){
                continue;
            }
            if(attackUnitInRange(heavy)){
                continue;
            }
            Unit closestEenemy = getClosestUnit(heavy, enemyUnits);
            if(closestEenemy != null){
                int dir = findMoveDir(heavy, closestEenemy.getX(), closestEenemy.getY());
                if(dir != -1){
                    playerAction.addUnitAction(heavy, new UnitAction(UnitAction.TYPE_MOVE, dir));
                    continue;
                }
            }
        }
    }

    


    @Override
    public void reset() {
        resetPathFind();
    }

    @Override
    public AI clone() {
        return new TestV2(utt);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    @Override
    public PlayerAction getAction(int p, GameState gs) throws Exception {
        gameState = gs;
        physicalGameState = gs.getPhysicalGameState();
        player = gs.getPlayer(p);
        enemyPlayer = gs.getPlayer(p == 0 ? 1 : 0);

        init();
        
        playerAction = new PlayerAction();

        workerAction();
        baseAction();
        barracksAction();
        lightAction();
        rangedAction();
        heavyAction();
        
        playerAction.fillWithNones(gs, p, 1);
        return playerAction;
    }

    
}
