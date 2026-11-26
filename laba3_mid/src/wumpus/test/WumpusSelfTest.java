package wumpus.test;

import wumpus.agent.*;
import wumpus.game.*;
import wumpus.model.*;
import wumpus.world.*;


import java.util.*;

/**
 * Автоматические проверки + статистика на 1000 мирах.
 */
public final class WumpusSelfTest {

    public static void main(String[] args) {
        System.out.println("=== Wumpus Self-Test ===\n");

        testBookWorld();
        testNoDeathOnSafeCells();
        runStatistics(1000);

        System.out.println("\nAll tests finished.");
    }

    private static void testBookWorld() {
        System.out.println("--- Book world (Russell & Norvig fig. 7.2) ---");
        World world = WorldGenerator.bookWorld();
        GameSession session = new GameSession(world, 0);
        session.runToEnd();

        GameStatus status = world.getStatus();
        System.out.println("Result: " + status + ", score = " + world.getScore()
                + ", steps = " + world.getSteps());

        KnowledgeBase kb = session.getAgent().getKb();
        // Pit at [3,1] = (2,0) should be P!
        CellKnowledge ck = kb.get(2, 0);
        boolean pitOk = ck.pit == CellKnowledge.Truth.YES;
        System.out.println("  P! at [3,1]: " + (pitOk ? "OK" : "FAIL (" + ck.mark() + ")"));
        System.out.println("Book world test: " +
                (status == GameStatus.WON ? "WIN" : status.toString()));
        System.out.println();
    }

    private static void testNoDeathOnSafeCells() {
        System.out.println("--- Risk 0% => 0 deaths ---");
        int deaths = 0;
        int total = 200;
        Random rnd = new Random(42);
        for (int i = 0; i < total; i++) {
            int size = 4 + rnd.nextInt(3);
            WorldConfig cfg = new WorldConfig(size, 0.2, rnd.nextLong());
            World world = WorldGenerator.generate(cfg);
            GameSession session = new GameSession(world, 0);
            session.runToEnd();
            GameStatus st = world.getStatus();
            if (st == GameStatus.KILLED_BY_PIT || st == GameStatus.EATEN_BY_WUMPUS) {
                deaths++;
            }
        }
        System.out.println("Deaths on " + total + " worlds at risk 0%: " + deaths);
        System.out.println(deaths == 0 ? "OK — zero deaths" : "WARNING: deaths occurred");
        System.out.println();
    }

    private static void runStatistics(int worldsPerRisk) {
        System.out.println("--- Statistics: " + worldsPerRisk + " worlds per risk level ---\n");

        int[] risks = {0, 25, 50, 75, 100};
        int[] sizes = {4, 5, 6};

        System.out.printf("%-8s | %-10s | %-18s | %-10s | %-8s%n",
                "Risk", "Wins", "Escape (no gold)", "Death", "Total");
        System.out.println("---------|------------|--------------------|------------|---------");

        for (int risk : risks) {
            int wins = 0, escaped = 0, dead = 0, other = 0;
            Random rnd = new Random(12345 + risk);

            for (int i = 0; i < worldsPerRisk; i++) {
                int size = sizes[i % sizes.length];
                WorldConfig cfg = new WorldConfig(size, 0.2, rnd.nextLong());
                World world = WorldGenerator.generate(cfg);
                GameSession session = new GameSession(world, risk);
                session.runToEnd();

                switch (world.getStatus()) {
                    case WON -> wins++;
                    case ESCAPED -> escaped++;
                    case KILLED_BY_PIT, EATEN_BY_WUMPUS -> dead++;
                    default -> other++;
                }
            }
            int total = wins + escaped + dead + other;

            System.out.printf("%-8s | %6.1f%%    | %8.1f%%          | %6.1f%%    | %d%n",
                    risk + "%",
                    100.0 * wins / total,
                    100.0 * escaped / total,
                    100.0 * dead / total,
                    total);
        }

        System.out.println("\n(Table ready for the report)");
    }
}
