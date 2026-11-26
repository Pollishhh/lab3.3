package wumpus.ui;


import javax.swing.*;
import java.awt.*;

/** Панель с сеткой N×N. */
public class GridPanel extends JPanel {
    private int size = 4;
    private CellPainter painter;

    public GridPanel(CellPainter painter) {
        this.painter = painter;
        setBackground(Color.WHITE);
    }

    public void setSizeN(int n) {
        this.size = n;
        repaint();
    }

    public void setPainter(CellPainter painter) {
        this.painter = painter;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (painter == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int cell = Math.min(w, h) / size;
        int offsetX = (w - cell * size) / 2;
        int offsetY = (h - cell * size) / 2;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // y=0 внизу
                int px = offsetX + x * cell;
                int py = offsetY + (size - 1 - y) * cell;
                painter.paint(g2, x, y, px, py, cell);
            }
        }

        // Сетка
        g2.setColor(Color.GRAY);
        for (int i = 0; i <= size; i++) {
            g2.drawLine(offsetX + i * cell, offsetY, offsetX + i * cell, offsetY + size * cell);
            g2.drawLine(offsetX, offsetY + i * cell, offsetX + size * cell, offsetY + i * cell);
        }
    }

    public interface CellPainter {
        void paint(Graphics2D g, int x, int y, int px, int py, int cellSize);
    }
}
