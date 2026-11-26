package wumpus.world;

import wumpus.model.*;


import java.util.HashSet;
import java.util.Set;

/**
 * Истинный мир. Агент его не видит — только получает Percept.
 */
public final class World {
    public static final int SCORE_GOLD = 1000;
    public static final int SCORE_DEATH = -1000;
    public static final int SCORE_STEP = -1;
    public static final int SCORE_SHOOT = -10;

    public final int size;
    public final long seed;

    private final Position wumpusPos;
    private final Position goldPos;
    private final Set<Position> pits;

    private Position agentPos = new Position(0, 0);
    private Direction agentDir = Direction.EAST;
    private boolean hasArrow = true;
    private boolean hasGold = false;
    private boolean wumpusAlive = true;
    private int score = 0;
    private int steps = 0;
    private GameStatus status = GameStatus.IN_PROGRESS;

    public World(int size, Position wumpus, Position gold, Set<Position> pits, long seed) {
        this.size = size;
        this.wumpusPos = wumpus;
        this.goldPos = gold;
        this.pits = new HashSet<>(pits);
        this.seed = seed;
    }

    public Percept perceive() {
        if (status.isTerminal()) return Percept.NONE;

        boolean stench = false;
        boolean breeze = false;
        boolean glitter = agentPos.equals(goldPos) && !hasGold;
        boolean bump = false; // устанавливается только после неудачного FORWARD
        boolean scream = false; // устанавливается только после удачного SHOOT

        for (Position nb : agentPos.neighbors(size)) {
            if (wumpusAlive && nb.equals(wumpusPos)) stench = true;
            if (pits.contains(nb)) breeze = true;
        }
        // Если агент стоит на клетке с живым Вампусом — тоже запах (хотя он уже мёртв)
        if (wumpusAlive && agentPos.equals(wumpusPos)) stench = true;

        return new Percept(stench, breeze, glitter, bump, scream);
    }

    /** Выполнить действие и вернуть новый percept (с возможными Bump/Scream). */
    public Percept execute(Action action) {
        if (status.isTerminal()) return Percept.NONE;

        boolean bump = false;
        boolean scream = false;

        switch (action) {
            case FORWARD -> {
                Position next = agentPos.move(agentDir);
                if (!next.isValid(size)) {
                    bump = true;
                } else {
                    agentPos = next;
                    score += SCORE_STEP;
                    steps++;
                    // Проверка смерти
                    if (pits.contains(agentPos)) {
                        status = GameStatus.KILLED_BY_PIT;
                        score += SCORE_DEATH;
                    } else if (wumpusAlive && agentPos.equals(wumpusPos)) {
                        status = GameStatus.EATEN_BY_WUMPUS;
                        score += SCORE_DEATH;
                    }
                }
            }
            case TURN_LEFT -> {
                agentDir = agentDir.turnLeft();
                score += SCORE_STEP;
                steps++;
            }
            case TURN_RIGHT -> {
                agentDir = agentDir.turnRight();
                score += SCORE_STEP;
                steps++;
            }
            case SHOOT -> {
                score += SCORE_SHOOT;
                steps++;
                if (hasArrow) {
                    hasArrow = false;
                    // Стрела летит по прямой
                    Position arrow = agentPos;
                    while (true) {
                        arrow = arrow.move(agentDir);
                        if (!arrow.isValid(size)) break;
                        if (wumpusAlive && arrow.equals(wumpusPos)) {
                            wumpusAlive = false;
                            scream = true;
                            break;
                        }
                    }
                }
            }
            case GRAB -> {
                steps++;
                score += SCORE_STEP;
                if (agentPos.equals(goldPos) && !hasGold) {
                    hasGold = true;
                    score += SCORE_GOLD;
                }
            }
            case CLIMB -> {
                steps++;
                score += SCORE_STEP;
                if (agentPos.x == 0 && agentPos.y == 0) {
                    status = hasGold ? GameStatus.WON : GameStatus.ESCAPED;
                }
            }
        }

        // Формируем percept после действия
        boolean stench = false;
        boolean breeze = false;
        boolean glitter = agentPos.equals(goldPos) && !hasGold;

        for (Position nb : agentPos.neighbors(size)) {
            if (wumpusAlive && nb.equals(wumpusPos)) stench = true;
            if (pits.contains(nb)) breeze = true;
        }
        if (wumpusAlive && agentPos.equals(wumpusPos)) stench = true;

        return new Percept(stench, breeze, glitter, bump, scream);
    }

    // --- Геттеры для GUI и отладки ---

    public Position getAgentPos() { return agentPos; }
    public Direction getAgentDir() { return agentDir; }
    public boolean hasArrow() { return hasArrow; }
    public boolean hasGold() { return hasGold; }
    public boolean isWumpusAlive() { return wumpusAlive; }
    public int getScore() { return score; }
    public int getSteps() { return steps; }
    public GameStatus getStatus() { return status; }
    public Position getWumpusPos() { return wumpusPos; }
    public Position getGoldPos() { return goldPos; }
    public Set<Position> getPits() { return Set.copyOf(pits); }

    public boolean isPit(Position p) { return pits.contains(p); }
    public boolean isWumpus(Position p) { return p.equals(wumpusPos); }
    public boolean isGold(Position p) { return p.equals(goldPos); }
}
