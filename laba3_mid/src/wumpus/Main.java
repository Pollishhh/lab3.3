package wumpus;

import wumpus.test.WumpusSelfTest;
import wumpus.ui.MainFrame;

import javax.swing.*;

public final class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test")) {
            WumpusSelfTest.main(args);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}
