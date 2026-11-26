package wumpus.ui;

import wumpus.agent.*;
import wumpus.game.*;
import wumpus.model.*;
import wumpus.world.*;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/** Главное окно приложения. */
public class MainFrame extends JFrame {

    private GameSession session;
    private int currentSize = 4;
    private double pitProb = 0.2;
    private long seed = 42;
    private int risk = 0;

    private final JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 12, 1));
    private final JSpinner pitSpinner = new JSpinner(new SpinnerNumberModel(20, 0, 50, 5));
    private final JTextField seedField = new JTextField("42", 8);
    private final JComboBox<String> riskCombo = new JComboBox<>(new String[]{"0%", "25%", "50%", "75%", "100%"});
    private final JSlider delaySlider = new JSlider(0, 1000, 300);

    private final JButton newGameBtn = new JButton("Новая игра");
    private final JButton randomSeedBtn = new JButton("Случайный seed");
    private final JButton bookBtn = new JButton("Пример из книги");
    private final JButton stepBtn = new JButton("Шаг");
    private final JButton autoBtn = new JButton("Авто");
    private final JButton stopBtn = new JButton("Стоп");

    private final JTextArea journalArea = new JTextArea();
    private final JLabel statusLabel = new JLabel(" ");

    private GridPanel knowledgePanel;
    private GridPanel worldPanel;

    private Timer autoTimer;

    public MainFrame() {
        super("Мир Вампуса — Лабораторная работа № 3");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        buildUi();
        newGame(false);
    }

    private void buildUi() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("N:"));
        top.add(sizeSpinner);
        top.add(new JLabel("Ямы %:"));
        top.add(pitSpinner);
        top.add(new JLabel("Seed:"));
        top.add(seedField);
        top.add(randomSeedBtn);
        top.add(new JLabel("Риск:"));
        top.add(riskCombo);
        top.add(newGameBtn);
        top.add(bookBtn);

        JPanel control = new JPanel(new FlowLayout(FlowLayout.LEFT));
        control.add(stepBtn);
        control.add(autoBtn);
        control.add(stopBtn);
        control.add(new JLabel("  Задержка:"));
        delaySlider.setMajorTickSpacing(250);
        delaySlider.setPaintTicks(true);
        delaySlider.setPreferredSize(new Dimension(150, 40));
        control.add(delaySlider);

        knowledgePanel = new GridPanel(this::paintKnowledge);
        worldPanel = new GridPanel(this::paintWorld);

        JPanel maps = new JPanel(new GridLayout(1, 2, 8, 0));
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(new TitledBorder("Представление агента (база знаний)"));
        left.add(knowledgePanel, BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(new TitledBorder("Истинный мир (агент не видит)"));
        right.add(worldPanel, BorderLayout.CENTER);
        maps.add(left);
        maps.add(right);

        journalArea.setEditable(false);
        journalArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(journalArea);
        scroll.setBorder(new TitledBorder("Журнал рассуждений"));
        scroll.setPreferredSize(new Dimension(0, 180));

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.add(maps, BorderLayout.CENTER);
        center.add(scroll, BorderLayout.SOUTH);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(control, BorderLayout.NORTH);
        south.add(statusLabel, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        // Listeners
        newGameBtn.addActionListener(e -> newGame(false));
        randomSeedBtn.addActionListener(e -> {
            seedField.setText(String.valueOf(System.currentTimeMillis() % 100000));
            newGame(false);
        });
        bookBtn.addActionListener(e -> newGame(true));
        stepBtn.addActionListener(e -> doStep());
        autoBtn.addActionListener(e -> startAuto());
        stopBtn.addActionListener(e -> stopAuto());
    }

    private void newGame(boolean book) {
        stopAuto();
        currentSize = (Integer) sizeSpinner.getValue();
        pitProb = ((Integer) pitSpinner.getValue()) / 100.0;
        try {
            seed = Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException ex) {
            seed = 42;
            seedField.setText("42");
        }
        risk = riskCombo.getSelectedIndex() * 25;

        World world;
        if (book) {
            world = WorldGenerator.bookWorld();
            currentSize = 4;
            sizeSpinner.setValue(4);
            seedField.setText("book");
        } else {
            WorldConfig cfg = new WorldConfig(currentSize, pitProb, seed);
            world = WorldGenerator.generate(cfg);
        }

        session = new GameSession(world, risk);
        knowledgePanel.setSizeN(currentSize);
        worldPanel.setSizeN(currentSize);
        journalArea.setText("");
        updateStatus();
        repaintMaps();
    }

    private void doStep() {
        if (session == null || session.isFinished()) return;
        StepRecord rec = session.step();
        if (rec != null) {
            journalArea.append(rec + "\n\n");
            journalArea.setCaretPosition(journalArea.getDocument().getLength());
        }
        updateStatus();
        repaintMaps();
        if (session.isFinished()) {
            stopAuto();
            journalArea.append("=== Игра окончена: " + session.getWorld().getStatus().russian()
                    + ", очки = " + session.getWorld().getScore() + " ===\n");
        }
    }

    private void startAuto() {
        if (autoTimer != null && autoTimer.isRunning()) return;
        autoTimer = new Timer(delaySlider.getValue(), e -> doStep());
        autoTimer.start();
    }

    private void stopAuto() {
        if (autoTimer != null) {
            autoTimer.stop();
        }
    }

    private void updateStatus() {
        if (session == null) return;
        World w = session.getWorld();
        KnowledgeBasedAgent a = session.getAgent();
        statusLabel.setText(String.format(
                "Статус: %s   |   Очки: %d   |   Шагов: %d   |   Стрела: %s   |   Золото: %s   |   Риск: %d%%",
                w.getStatus().russian(),
                w.getScore(),
                w.getSteps(),
                a.hasArrow() ? "есть" : "нет",
                a.hasGold() ? "взято" : "нет",
                risk
        ));
    }

    private void repaintMaps() {
        knowledgePanel.repaint();
        worldPanel.repaint();
    }

    // --- Отрисовка карты знаний ---
    private void paintKnowledge(Graphics2D g, int x, int y, int px, int py, int cell) {
        if (session == null) return;
        KnowledgeBase kb = session.getAgent().getKb();
        CellKnowledge ck = kb.get(x, y);
        Position agentPos = session.getAgent().getPos();

        // Фон
        if (ck.visited) {
            g.setColor(new Color(220, 255, 220));
        } else if (ck.isDangerous()) {
            g.setColor(new Color(255, 200, 200));
        } else if (ck.isSafe()) {
            g.setColor(new Color(200, 230, 255));
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillRect(px + 1, py + 1, cell - 1, cell - 1);

        // Текст
        g.setColor(Color.BLACK);
        String mark = ck.mark();
        if (ck.visited) mark = (mark.isEmpty() ? "V" : "V " + mark);
        if (!mark.isEmpty()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, cell / 5)));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(mark);
            g.drawString(mark, px + (cell - tw) / 2, py + cell / 2 + fm.getAscent() / 2 - 2);
        }

        // Агент
        if (agentPos.x == x && agentPos.y == y) {
            g.setColor(Color.BLUE);
            int r = cell / 3;
            g.fillOval(px + cell / 2 - r / 2, py + cell / 2 - r / 2, r, r);
            g.setColor(Color.WHITE);
            String arrow = session.getAgent().getDir().arrow();
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, cell / 4)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(arrow, px + (cell - fm.stringWidth(arrow)) / 2, py + cell / 2 + fm.getAscent() / 2 - 2);
        }

        // Координаты мелко
        g.setColor(Color.GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(9, cell / 7)));
        g.drawString((x + 1) + "," + (y + 1), px + 3, py + cell - 4);
    }

    // --- Отрисовка истинного мира ---
    private void paintWorld(Graphics2D g, int x, int y, int px, int py, int cell) {
        if (session == null) return;
        World w = session.getWorld();
        Position p = new Position(x, y);

        g.setColor(Color.WHITE);
        g.fillRect(px + 1, py + 1, cell - 1, cell - 1);

        // Яма
        if (w.isPit(p)) {
            g.setColor(new Color(80, 80, 80));
            g.fillOval(px + cell / 4, py + cell / 4, cell / 2, cell / 2);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, cell / 5)));
            g.drawString("P", px + cell / 2 - 4, py + cell / 2 + 5);
        }
        // Вампус
        if (w.isWumpus(p)) {
            g.setColor(w.isWumpusAlive() ? Color.RED : Color.GRAY);
            g.fillRoundRect(px + cell / 5, py + cell / 5, cell * 3 / 5, cell * 3 / 5, 8, 8);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, cell / 5)));
            g.drawString(w.isWumpusAlive() ? "W" : "W†", px + cell / 2 - 6, py + cell / 2 + 5);
        }
        // Золото
        if (w.isGold(p) && !w.hasGold()) {
            g.setColor(new Color(255, 215, 0));
            int[] xs = {px + cell / 2, px + cell * 3 / 4, px + cell / 2, px + cell / 4};
            int[] ys = {py + cell / 4, py + cell / 2, py + cell * 3 / 4, py + cell / 2};
            g.fillPolygon(xs, ys, 4);
        }

        // Агент
        Position ap = w.getAgentPos();
        if (ap.x == x && ap.y == y) {
            g.setColor(Color.BLUE);
            int r = cell / 3;
            g.fillOval(px + cell / 2 - r / 2, py + cell / 2 - r / 2, r, r);
            g.setColor(Color.WHITE);
            String arrow = w.getAgentDir().arrow();
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, cell / 4)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(arrow, px + (cell - fm.stringWidth(arrow)) / 2, py + cell / 2 + fm.getAscent() / 2 - 2);
        }

        g.setColor(Color.GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(9, cell / 7)));
        g.drawString((x + 1) + "," + (y + 1), px + 3, py + cell - 4);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}
