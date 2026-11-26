package wumpus.game;

import wumpus.model.Action;
import wumpus.model.Percept;

import java.util.List;

public final class StepRecord {
    public final int stepNumber;
    public final Percept percept;
    public final List<String> inferences;
    public final String decision;
    public final Action action;
    public final int score;
    public final String agentPos;
    public final String agentDir;

    public StepRecord(int stepNumber, Percept percept, List<String> inferences,
                      String decision, Action action, int score,
                      String agentPos, String agentDir) {
        this.stepNumber = stepNumber;
        this.percept = percept;
        this.inferences = inferences;
        this.decision = decision;
        this.action = action;
        this.score = score;
        this.agentPos = agentPos;
        this.agentDir = agentDir;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Шаг ").append(stepNumber).append("\n");
        sb.append("  Измерение: ").append(percept).append("\n");
        if (!inferences.isEmpty()) {
            sb.append("  Выводы:\n");
            for (String inf : inferences) {
                sb.append("    • ").append(inf).append("\n");
            }
        }
        sb.append("  Решение: ").append(decision).append("\n");
        sb.append("  Действие: ").append(action.russian()).append("\n");
        sb.append("  Позиция: ").append(agentPos).append(" ").append(agentDir)
          .append("  Очки: ").append(score);
        return sb.toString();
    }
}
