package wumpus.model;

import java.util.Objects;

/** Координаты клетки. Внутри 0-based, при выводе показываем 1-based [x,y], где [1,1] — левый нижний угол. */
public final class Position {
    public final int x; // column, 0-based
    public final int y; // row, 0-based

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position move(Direction dir) {
        return new Position(x + dir.dx, y + dir.dy);
    }

    public boolean isValid(int size) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    /** Соседи по стороне (4-connected). */
    public Position[] neighbors(int size) {
        Direction[] dirs = Direction.values();
        java.util.List<Position> list = new java.util.ArrayList<>(4);
        for (Direction d : dirs) {
            Position n = move(d);
            if (n.isValid(size)) list.add(n);
        }
        return list.toArray(new Position[0]);
    }

    /** Для вывода в стиле книги: [x,y] где x — столбец с 1, y — строка с 1 снизу. */
    public String toDisplay(int size) {
        return "[" + (x + 1) + "," + (y + 1) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
