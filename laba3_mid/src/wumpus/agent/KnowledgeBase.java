package wumpus.agent;

import wumpus.model.*;


import java.util.*;

/**
 * База знаний агента.
 * Правила R1–R6 применяются до неподвижной точки.
 */
public final class KnowledgeBase {

    private final int size;
    private final CellKnowledge[][] cells;
    private final List<String> inferences = new ArrayList<>();

    private boolean wumpusDead = false;
    private Position wumpusPos = null; // если доказан

    public KnowledgeBase(int size) {
        this.size = size;
        cells = new CellKnowledge[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                cells[x][y] = new CellKnowledge();
            }
        }
        // Стартовая клетка безопасна
        cells[0][0].pit = CellKnowledge.Truth.NO;
        cells[0][0].wumpus = CellKnowledge.Truth.NO;
        cells[0][0].safe = true;
    }

    public CellKnowledge get(Position p) {
        return cells[p.x][p.y];
    }

    public CellKnowledge get(int x, int y) {
        return cells[x][y];
    }

    public boolean isWumpusDead() {
        return wumpusDead;
    }

    public Position getKnownWumpusPos() {
        return wumpusPos;
    }

    public List<String> getLastInferences() {
        return List.copyOf(inferences);
    }

    /** Сообщить наблюдение в клетке. */
    public void tell(Position pos, Percept percept) {
        inferences.clear();
        CellKnowledge ck = get(pos);
        ck.visited = true;
        ck.stench = percept.stench;
        ck.breeze = percept.breeze;
        ck.pit = CellKnowledge.Truth.NO;
        ck.wumpus = CellKnowledge.Truth.NO;
        ck.safe = true;

        if (percept.scream) {
            wumpusDead = true;
            inferences.add("Scream → Вампус мёртв");
            // Все клетки с возможным Вампусом теперь безопасны по Вампусу
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (cells[x][y].wumpus != CellKnowledge.Truth.NO) {
                        cells[x][y].wumpus = CellKnowledge.Truth.NO;
                    }
                }
            }
            wumpusPos = null;
        }

        infer();
    }

    /** Применить правила логического вывода до стабилизации. */
    public void infer() {
        boolean changed;
        do {
            changed = false;
            changed |= ruleNoBreezeNoPit();
            changed |= ruleBreezeAtLeastOnePit();
            changed |= ruleNoStenchNoWumpus();
            changed |= ruleWumpusLocalization();
            changed |= ruleVisitedSafe();
            changed |= ruleDeadWumpusSafe();
        } while (changed);
    }

    // R1: нет ветерка ⇒ у всех соседей нет ямы
    private boolean ruleNoBreezeNoPit() {
        boolean changed = false;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellKnowledge ck = cells[x][y];
                if (!ck.visited || ck.breeze) continue;
                Position p = new Position(x, y);
                for (Position nb : p.neighbors(size)) {
                    CellKnowledge nck = get(nb);
                    if (nck.pit != CellKnowledge.Truth.NO) {
                        nck.pit = CellKnowledge.Truth.NO;
                        updateSafe(nb);
                        inferences.add("Нет ветерка в " + p.toDisplay(size) + " ⇒ нет ямы в " + nb.toDisplay(size));
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    // R2: есть ветерок ⇒ хотя бы один сосед — яма; если кандидат один — P!
    private boolean ruleBreezeAtLeastOnePit() {
        boolean changed = false;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellKnowledge ck = cells[x][y];
                if (!ck.visited || !ck.breeze) continue;
                Position p = new Position(x, y);
                List<Position> candidates = new ArrayList<>();
                for (Position nb : p.neighbors(size)) {
                    CellKnowledge nck = get(nb);
                    if (nck.pit != CellKnowledge.Truth.NO) {
                        candidates.add(nb);
                    }
                }
                if (candidates.size() == 1) {
                    Position only = candidates.get(0);
                    CellKnowledge nck = get(only);
                    if (nck.pit != CellKnowledge.Truth.YES) {
                        nck.pit = CellKnowledge.Truth.YES;
                        nck.safe = false;
                        inferences.add("Ветерок в " + p.toDisplay(size) + " + единственный кандидат ⇒ P! в " + only.toDisplay(size));
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    // R3: нет запаха ⇒ у всех соседей нет Вампуса
    private boolean ruleNoStenchNoWumpus() {
        boolean changed = false;
        if (wumpusDead) return false;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellKnowledge ck = cells[x][y];
                if (!ck.visited || ck.stench) continue;
                Position p = new Position(x, y);
                for (Position nb : p.neighbors(size)) {
                    CellKnowledge nck = get(nb);
                    if (nck.wumpus != CellKnowledge.Truth.NO) {
                        nck.wumpus = CellKnowledge.Truth.NO;
                        updateSafe(nb);
                        inferences.add("Нет запаха в " + p.toDisplay(size) + " ⇒ нет Вампуса в " + nb.toDisplay(size));
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    // R4: Вампус один → пересечение возможных мест; один кандидат — W!
    private boolean ruleWumpusLocalization() {
        boolean changed = false;
        if (wumpusDead || wumpusPos != null) return false;

        // Собираем все клетки, которые ещё могут содержать Вампуса
        List<Position> possible = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (cells[x][y].wumpus != CellKnowledge.Truth.NO && !cells[x][y].visited) {
                    possible.add(new Position(x, y));
                }
            }
        }

        // Если есть клетки с запахом — Вампус должен быть соседом каждой из них
        List<Position> stenchCells = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (cells[x][y].visited && cells[x][y].stench) {
                    stenchCells.add(new Position(x, y));
                }
            }
        }

        if (!stenchCells.isEmpty()) {
            List<Position> intersection = new ArrayList<>(possible);
            for (Position sc : stenchCells) {
                Set<Position> neighbors = new HashSet<>(Arrays.asList(sc.neighbors(size)));
                intersection.removeIf(p -> !neighbors.contains(p));
            }
            possible = intersection;
        }

        if (possible.size() == 1) {
            Position only = possible.get(0);
            CellKnowledge ck = get(only);
            if (ck.wumpus != CellKnowledge.Truth.YES) {
                ck.wumpus = CellKnowledge.Truth.YES;
                ck.safe = false;
                wumpusPos = only;
                inferences.add("Локализация Вампуса ⇒ W! в " + only.toDisplay(size));
                // Все остальные — не Вампус
                for (int x = 0; x < size; x++) {
                    for (int y = 0; y < size; y++) {
                        if (x == only.x && y == only.y) continue;
                        if (cells[x][y].wumpus != CellKnowledge.Truth.NO) {
                            cells[x][y].wumpus = CellKnowledge.Truth.NO;
                            updateSafe(new Position(x, y));
                        }
                    }
                }
                changed = true;
            }
        }
        return changed;
    }

    // R5: посещённая клетка безопасна
    private boolean ruleVisitedSafe() {
        boolean changed = false;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellKnowledge ck = cells[x][y];
                if (ck.visited && !ck.safe) {
                    ck.pit = CellKnowledge.Truth.NO;
                    ck.wumpus = CellKnowledge.Truth.NO;
                    ck.safe = true;
                    changed = true;
                }
            }
        }
        return changed;
    }

    // R6: мёртвый Вампус не опасен
    private boolean ruleDeadWumpusSafe() {
        if (!wumpusDead) return false;
        boolean changed = false;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellKnowledge ck = cells[x][y];
                if (ck.wumpus != CellKnowledge.Truth.NO) {
                    ck.wumpus = CellKnowledge.Truth.NO;
                    updateSafe(new Position(x, y));
                    changed = true;
                }
            }
        }
        return changed;
    }

    private void updateSafe(Position p) {
        CellKnowledge ck = get(p);
        if (ck.pit == CellKnowledge.Truth.NO && ck.wumpus == CellKnowledge.Truth.NO) {
            ck.safe = true;
        }
    }

    public boolean isSafe(Position p) {
        if (!p.isValid(size)) return false;
        return get(p).isSafe();
    }

    public boolean isDangerous(Position p) {
        if (!p.isValid(size)) return true;
        return get(p).isDangerous();
    }

    /** Все доказанно безопасные непосещённые клетки. */
    public List<Position> safeUnvisited() {
        List<Position> result = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellKnowledge ck = cells[x][y];
                if (ck.isSafe() && !ck.visited) {
                    result.add(new Position(x, y));
                }
            }
        }
        return result;
    }

    /** Клетки на границе исследованной области, которые ещё не доказаны (для риска). */
    public List<Position> frontierUnsafe() {
        List<Position> result = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Position p = new Position(x, y);
                CellKnowledge ck = cells[x][y];
                if (ck.visited || ck.isSafe() || ck.isDangerous()) continue;
                // Есть ли посещённый сосед?
                for (Position nb : p.neighbors(size)) {
                    if (get(nb).visited) {
                        result.add(p);
                        break;
                    }
                }
            }
        }
        return result;
    }
}
