package wumpus.model;

public enum Action {
    FORWARD,
    TURN_LEFT,
    TURN_RIGHT,
    SHOOT,
    GRAB,
    CLIMB;

    public String russian() {
        return switch (this) {
            case FORWARD    -> "Вперёд";
            case TURN_LEFT  -> "Поворот влево";
            case TURN_RIGHT -> "Поворот вправо";
            case SHOOT      -> "Выстрел";
            case GRAB       -> "Взять золото";
            case CLIMB      -> "Вылезти";
        };
    }
}
