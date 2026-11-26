package wumpus.model;

/** Показания пяти датчиков. */
public final class Percept {
    public final boolean stench;
    public final boolean breeze;
    public final boolean glitter;
    public final boolean bump;
    public final boolean scream;

    public Percept(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        this.stench = stench;
        this.breeze = breeze;
        this.glitter = glitter;
        this.bump = bump;
        this.scream = scream;
    }

    public static final Percept NONE = new Percept(false, false, false, false, false);

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        if (stench)  sb.append("Stench,");
        if (breeze)  sb.append("Breeze,");
        if (glitter) sb.append("Glitter,");
        if (bump)    sb.append("Bump,");
        if (scream)  sb.append("Scream,");
        if (sb.length() == 1) return "(none)";
        sb.setLength(sb.length() - 1);
        sb.append(")");
        return sb.toString();
    }
}
