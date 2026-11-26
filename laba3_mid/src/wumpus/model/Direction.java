package wumpus.model;

public enum Direction {
    NORTH(0, 1),
    EAST(1, 0),
    SOUTH(0, -1),
    WEST(-1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Direction turnLeft() {
        return values()[(ordinal() + 3) % 4];
    }

    public Direction turnRight() {
        return values()[(ordinal() + 1) % 4];
    }

    public Direction opposite() {
        return values()[(ordinal() + 2) % 4];
    }

    /** Стрелка для отображения. */
    public String arrow() {
        return switch (this) {
            case NORTH -> "↑";
            case EAST  -> "→";
            case SOUTH -> "↓";
            case WEST  -> "←";
        };
    }
}
