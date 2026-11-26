package wumpus.world;

import wumpus.model.*;


import java.util.*;

/** Генерация случайного мира и мира из книги. */
public final class WorldGenerator {

    private WorldGenerator() {}

    public static World generate(WorldConfig config) {
        Random rnd = new Random(config.seed);
        int n = config.size;

        // Все клетки кроме старта [0,0]
        List<Position> candidates = new ArrayList<>();
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                if (x == 0 && y == 0) continue;
                candidates.add(new Position(x, y));
            }
        }
        Collections.shuffle(candidates, rnd);

        // Вампус
        Position wumpus = candidates.remove(0);

        // Золото — обязательно соседняя по стороне с Вампусом
        List<Position> goldCandidates = new ArrayList<>();
        for (Position nb : wumpus.neighbors(n)) {
            if (!(nb.x == 0 && nb.y == 0)) {
                goldCandidates.add(nb);
            }
        }
        // Если вдруг нет подходящих соседей (крайне редко на маленьких полях) — берём любую
        Position gold;
        if (!goldCandidates.isEmpty()) {
            gold = goldCandidates.get(rnd.nextInt(goldCandidates.size()));
            candidates.remove(gold);
        } else {
            gold = candidates.remove(0);
        }

        // Ямы
        Set<Position> pits = new HashSet<>();
        int pitsToPlace = Math.min(config.pitCount(), candidates.size());
        for (int i = 0; i < pitsToPlace; i++) {
            pits.add(candidates.remove(0));
        }

        return new World(n, wumpus, gold, pits, config.seed);
    }

    /** Классический мир из книги Рассела и Норвига (рис. 7.2), N=4. */
    public static World bookWorld() {
        // Координаты 0-based, [1,1] книги = (0,0)
        // Вампус в [1,3] книги → (0,2)
        // Ямы в [3,1] и [3,3] и [4,4] → (2,0), (2,2), (3,3)
        // Золото в [2,3] → (1,2)
        Set<Position> pits = new HashSet<>();
        pits.add(new Position(2, 0)); // [3,1]
        pits.add(new Position(2, 2)); // [3,3]
        pits.add(new Position(3, 3)); // [4,4]
        return new World(4,
                new Position(0, 2),   // Wumpus [1,3]
                new Position(1, 2),   // Gold   [2,3]
                pits,
                0L);
    }
}
