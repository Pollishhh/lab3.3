package wumpus.agent;

import wumpus.model.*;


import java.util.*;

/** Простой BFS по безопасным клеткам. */
public final class PathFinder {

    public static final class Path {
        public final List<Position> cells;
        public final List<Direction> moves; // направление, в котором нужно идти из cells[i] в cells[i+1]

        public Path(List<Position> cells, List<Direction> moves) {
            this.cells = cells;
            this.moves = moves;
        }

        public boolean isEmpty() {
            return cells.size() < 2;
        }
    }

    /**
     * Найти путь из from в to только по клеткам, для которых safePredicate = true.
     * Возвращает null, если пути нет.
     */
    public static Path find(Position from, Position to, int size,
                            java.util.function.Predicate<Position> safePredicate) {
        if (from.equals(to)) {
            return new Path(List.of(from), List.of());
        }

        Queue<Position> queue = new ArrayDeque<>();
        Map<Position, Position> parent = new HashMap<>();
        Map<Position, Direction> dirUsed = new HashMap<>();

        queue.add(from);
        parent.put(from, null);

        while (!queue.isEmpty()) {
            Position cur = queue.poll();
            for (Direction d : Direction.values()) {
                Position next = cur.move(d);
                if (!next.isValid(size)) continue;
                if (parent.containsKey(next)) continue;
                if (!safePredicate.test(next) && !next.equals(to)) continue;
                // Разрешаем to даже если она не safe (для риска)
                parent.put(next, cur);
                dirUsed.put(next, d);
                if (next.equals(to)) {
                    return reconstruct(from, to, parent, dirUsed);
                }
                queue.add(next);
            }
        }
        return null;
    }

    private static Path reconstruct(Position from, Position to,
                                    Map<Position, Position> parent,
                                    Map<Position, Direction> dirUsed) {
        LinkedList<Position> cells = new LinkedList<>();
        LinkedList<Direction> moves = new LinkedList<>();
        Position cur = to;
        while (cur != null) {
            cells.addFirst(cur);
            if (parent.get(cur) != null) {
                moves.addFirst(dirUsed.get(cur));
            }
            cur = parent.get(cur);
        }
        return new Path(cells, moves);
    }
}
