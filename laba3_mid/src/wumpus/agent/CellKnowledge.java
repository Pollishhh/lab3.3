package wumpus.agent;

/** Знания агента об одной клетке. */
public final class CellKnowledge {
    public enum Truth { UNKNOWN, YES, NO }

    public Truth pit = Truth.UNKNOWN;
    public Truth wumpus = Truth.UNKNOWN;
    public boolean visited = false;
    public boolean stench = false;
    public boolean breeze = false;
    public boolean safe = false;

    public boolean isSafe() {
        return safe || (pit == Truth.NO && wumpus == Truth.NO);
    }

    public boolean isDangerous() {
        return pit == Truth.YES || wumpus == Truth.YES;
    }

    public String mark() {
        if (pit == Truth.YES) return "P!";
        if (wumpus == Truth.YES) return "W!";
        if (isSafe()) return "OK";
        // Подозрение: неизвестная яма/Вампус при том, что другое уже исключено
        boolean pitMaybe = pit == Truth.UNKNOWN;
        boolean wumpusMaybe = wumpus == Truth.UNKNOWN;
        if (pitMaybe && wumpusMaybe) return ""; // полностью неизвестна — не шумим
        if (pitMaybe) return "P?";
        if (wumpusMaybe) return "W?";
        return "";
    }
}
