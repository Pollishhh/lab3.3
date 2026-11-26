package wumpus.game;

import wumpus.agent.KnowledgeBasedAgent;
import wumpus.model.*;
import wumpus.world.World;

import java.util.ArrayList;
import java.util.List;

public final class GameSession {
    public static final int MAX_STEPS = 300;

    private final World world;
    private final KnowledgeBasedAgent agent;
    private final List<StepRecord> journal = new ArrayList<>();
    private int stepCount = 0;
    private Percept lastPercept = Percept.NONE;

    public GameSession(World world, int riskPercent) {
        this.world = world;
        this.agent = new KnowledgeBasedAgent(world.size, riskPercent);
        this.lastPercept = world.perceive();
    }

    public World getWorld() { return world; }
    public KnowledgeBasedAgent getAgent() { return agent; }
    public List<StepRecord> getJournal() { return journal; }
    public int getStepCount() { return stepCount; }

    public boolean isFinished() {
        return world.getStatus().isTerminal() || stepCount >= MAX_STEPS;
    }

    public StepRecord step() {
        if (isFinished()) return null;

        agent.sync(
                world.getAgentPos(),
                world.getAgentDir(),
                world.hasGold(),
                world.hasArrow()
        );

        Action action = agent.nextAction(lastPercept);
        lastPercept = world.execute(action);

        stepCount++;
        StepRecord rec = new StepRecord(
                stepCount,
                lastPercept,
                agent.getLastInferences(),
                agent.getLastDecision(),
                action,
                world.getScore(),
                world.getAgentPos().toDisplay(world.size),
                world.getAgentDir().arrow()
        );
        journal.add(rec);
        return rec;
    }

    public void runToEnd() {
        while (!isFinished()) {
            step();
        }
    }
}
