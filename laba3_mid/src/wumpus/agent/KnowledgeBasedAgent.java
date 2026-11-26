package wumpus.agent;

import wumpus.model.*;

import java.util.*;

/**
 * Knowledge-Based Agent.
 * Каждый вызов nextAction заново строит план (нет «застрявших» старых маршрутов).
 * В доказанные P! / W! не входит никогда.
 */
public final class KnowledgeBasedAgent {

    private final int size;
    private final KnowledgeBase kb;
    private final int riskPercent;

    private boolean hasGold = false;
    private boolean hasArrow = true;

    private String lastDecision = "";
    private List<String> lastInferences = List.of();

    private Position pos = new Position(0, 0);
    private Direction dir = Direction.EAST;

    public KnowledgeBasedAgent(int size, int riskPercent) {
        this.size = size;
        this.kb = new KnowledgeBase(size);
        this.riskPercent = Math.max(0, Math.min(100, riskPercent));
    }

    public KnowledgeBase getKb() { return kb; }
    public Position getPos() { return pos; }
    public Direction getDir() { return dir; }
    public boolean hasGold() { return hasGold; }
    public boolean hasArrow() { return hasArrow; }
    public String getLastDecision() { return lastDecision; }
    public List<String> getLastInferences() { return lastInferences; }
    public int getRiskPercent() { return riskPercent; }

    public void sync(Position realPos, Direction realDir, boolean realHasGold, boolean realHasArrow) {
        this.pos = realPos;
        this.dir = realDir;
        this.hasGold = realHasGold;
        this.hasArrow = realHasArrow;
    }

    public Action nextAction(Percept percept) {
        kb.tell(pos, percept);
        lastInferences = kb.getLastInferences();
        if (percept.scream) {
            hasArrow = false;
        }

        // 1. Блеск → взять золото
        if (percept.glitter && !hasGold) {
            lastDecision = "Блеск → GRAB";
            return Action.GRAB;
        }

        // 2. С золотом — домой
        if (hasGold) {
            if (isOrigin(pos)) {
                lastDecision = "Дома с золотом → CLIMB";
                return Action.CLIMB;
            }
            Action a = firstStepTo(new Position(0, 0), true);
            if (a != null) {
                lastDecision = "С золотом → домой (безопасно)";
                return a;
            }
            // Если безопасного пути нет — при риске пробуем осторожный обход
            if (riskPercent > 0) {
                a = firstStepTo(new Position(0, 0), false);
                if (a != null) {
                    lastDecision = "С золотом → домой (с риском)";
                    return a;
                }
            }
        }

        // 3. Безопасные непосещённые
        List<Position> safeTargets = kb.safeUnvisited();
        safeTargets.sort(Comparator.comparingInt(p -> manhattan(pos, p)));
        for (Position t : safeTargets) {
            Action a = firstStepTo(t, true);
            if (a != null) {
                lastDecision = "Иду в безопасную " + t.toDisplay(size);
                return a;
            }
        }

        // 4. Стрельба по доказанному Вампусу
        Position wumpus = kb.getKnownWumpusPos();
        if (hasArrow && wumpus != null && !kb.isWumpusDead()) {
            Action a = firstStepOfShoot(wumpus);
            if (a != null) {
                lastDecision = "Стреляю в Вампуса " + wumpus.toDisplay(size);
                return a;
            }
        }

        // 5. Риск — только минимально подозрительные клетки
        if (riskPercent > 0) {
            Action a = riskyStep();
            if (a != null) return a;
        }

        // 6. Выход
        if (isOrigin(pos)) {
            lastDecision = "Выход без золота";
            return Action.CLIMB;
        }
        Action home = firstStepTo(new Position(0, 0), true);
        if (home != null) {
            lastDecision = "Возвращаюсь и выхожу";
            return home;
        }
        // Если даже домой безопасного пути нет — один рискованный шаг к дому
        if (riskPercent > 0) {
            Action homeRisk = firstStepTo(new Position(0, 0), false);
            if (homeRisk != null) {
                lastDecision = "Возврат домой с риском";
                return homeRisk;
            }
        }

        lastDecision = "Запасной поворот";
        return Action.TURN_RIGHT;
    }

    private Action riskyStep() {
        List<Position> frontier = kb.frontierUnsafe();
        frontier.removeIf(kb::isDangerous);
        if (frontier.isEmpty()) return null;

        frontier.sort(Comparator
                .comparingInt(this::suspicion)
                .thenComparingInt(p -> manhattan(pos, p))
                .thenComparingInt((Position p) -> -p.x)
                .thenComparingInt((Position p) -> -p.y));

        int minSusp = frontier.stream().mapToInt(this::suspicion).min().orElse(99);
        int maxSusp = switch (riskPercent) {
            case 25 -> 3;
            case 50 -> 4;
            case 75 -> 5;
            default -> 99;
        };
        if (minSusp > maxSusp) return null;

        for (Position t : frontier) {
            if (suspicion(t) != minSusp) continue;
            // Не ходим в клетку, если она уже единственный кандидат P! — это isDangerous
            Action a = firstStepTo(t, false);
            if (a != null) {
                lastDecision = "Риск " + riskPercent + "% → " + t.toDisplay(size)
                        + " (подозрение " + minSusp + ")";
                return a;
            }
        }
        return null;
    }

    /** Первый шаг по пути к target. onlySafe=true — только OK-клетки. */
    private Action firstStepTo(Position target, boolean onlySafe) {
        if (pos.equals(target)) return null;
        PathFinder.Path path = PathFinder.find(pos, target, size, p -> {
            if (p.equals(target) && !onlySafe) {
                return !kb.isDangerous(p);
            }
            // промежуточные всегда только безопасные; при !onlySafe разрешаем target
            if (!onlySafe && p.equals(target)) return !kb.isDangerous(p);
            return kb.isSafe(p);
        });
        if (path == null || path.isEmpty()) return null;
        return firstActionOfPath(path);
    }

    private Action firstStepOfShoot(Position wumpus) {
        List<Position> stands = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            Position p = new Position(x, wumpus.y);
            if (!p.equals(wumpus) && kb.isSafe(p)) stands.add(p);
        }
        for (int y = 0; y < size; y++) {
            Position p = new Position(wumpus.x, y);
            if (!p.equals(wumpus) && kb.isSafe(p)) stands.add(p);
        }
        stands.sort(Comparator.comparingInt(p -> manhattan(pos, p)));

        for (Position stand : stands) {
            // Уже стоим на линии и смотрим на Вампуса
            if (pos.equals(stand)) {
                Direction need = facingFromTo(stand, wumpus);
                if (dir == need) {
                    return Action.SHOOT;
                }
                return turnToward(need);
            }
            PathFinder.Path path = PathFinder.find(pos, stand, size, kb::isSafe);
            if (path == null || path.isEmpty()) continue;
            return firstActionOfPath(path);
        }
        return null;
    }

    private Action firstActionOfPath(PathFinder.Path path) {
        Direction need = path.moves.get(0);
        if (dir == need) {
            Position next = pos.move(dir);
            // Финальная страховка
            if (!next.isValid(size) || kb.isDangerous(next)) return null;
            if (riskPercent == 0 && !kb.isSafe(next)) return null;
            return Action.FORWARD;
        }
        return turnToward(need);
    }

    private Action turnToward(Direction need) {
        int rights = 0;
        Direction t = dir;
        while (t != need) {
            t = t.turnRight();
            rights++;
        }
        int lefts = (4 - rights) % 4;
        if (lefts > 0 && lefts < rights) return Action.TURN_LEFT;
        return Action.TURN_RIGHT;
    }

    private static Direction facingFromTo(Position from, Position to) {
        if (from.x == to.x) {
            return from.y < to.y ? Direction.NORTH : Direction.SOUTH;
        }
        return from.x < to.x ? Direction.EAST : Direction.WEST;
    }

    private int suspicion(Position p) {
        CellKnowledge ck = kb.get(p);
        int s = 0;
        if (ck.pit == CellKnowledge.Truth.UNKNOWN) s += 1;
        if (ck.wumpus == CellKnowledge.Truth.UNKNOWN) s += 1;
        for (Position nb : p.neighbors(size)) {
            CellKnowledge n = kb.get(nb);
            if (n.visited && n.breeze) s += 1;
            if (n.visited && n.stench) s += 1;
        }
        return s;
    }

    private static boolean isOrigin(Position p) {
        return p.x == 0 && p.y == 0;
    }

    private static int manhattan(Position a, Position b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }
}
