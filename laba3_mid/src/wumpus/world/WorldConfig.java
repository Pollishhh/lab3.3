package wumpus.world;

import wumpus.model.*;

/** Параметры генерации мира. */
public final class WorldConfig {
    public final int size;
    public final double pitProbability; // доля ям относительно (N² - 1)
    public final long seed;

    public WorldConfig(int size, double pitProbability, long seed) {
        if (size < 2) throw new IllegalArgumentException("size must be >= 2");
        this.size = size;
        this.pitProbability = Math.max(0, Math.min(1, pitProbability));
        this.seed = seed;
    }

    public int pitCount() {
        // Количество ям пропорционально числу комнат (как в задании)
        int rooms = size * size - 1; // кроме стартовой
        return Math.max(1, (int) Math.round(pitProbability * rooms));
    }

    public static WorldConfig defaultConfig(int size) {
        return new WorldConfig(size, 0.2, System.currentTimeMillis());
    }
}
