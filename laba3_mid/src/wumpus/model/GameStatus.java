package wumpus.model;

public enum GameStatus {
    IN_PROGRESS,
    WON,                 // вышел с золотом
    ESCAPED,             // вышел без золота
    KILLED_BY_PIT,
    EATEN_BY_WUMPUS,
    STEP_LIMIT;

    public boolean isTerminal() {
        return this != IN_PROGRESS;
    }

    public String russian() {
        return switch (this) {
            case IN_PROGRESS    -> "Идёт игра";
            case WON            -> "Победа (золото)";
            case ESCAPED        -> "Выход без золота";
            case KILLED_BY_PIT  -> "Погиб в яме";
            case EATEN_BY_WUMPUS-> "Съеден Вампусом";
            case STEP_LIMIT     -> "Превышен лимит шагов";
        };
    }
}
