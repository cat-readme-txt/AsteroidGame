import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class DesktopLauncher {
    private static final String APP_TITLE = "Asteroid Game - Void Navigator";
    private static final String BROWSER_ARGUMENT = "--browser";
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 700;

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        boolean browserMode = isBrowserMode(args);
        SwingUtilities.invokeLater(() -> launch(browserMode));
    }

    static boolean isBrowserMode(String[] args) {
        if (args == null) {
            return false;
        }
        for (String argument : args) {
            if (BROWSER_ARGUMENT.equals(argument)) {
                return true;
            }
        }
        return false;
    }

    private static void launch(boolean browserMode) {
        installSystemLookAndFeel();

        JFrame frame = new JFrame(APP_TITLE);
        AsteroidGame game = new AsteroidGame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (browserMode) {
            // CheerpJ owns the surrounding browser surface, so a second title bar
            // only wastes viewport space and prevents a true edge-to-edge layout.
            frame.setUndecorated(true);
        }
        frame.setContentPane(game);
        frame.pack();
        if (frame.getWidth() <= 0 || frame.getHeight() <= 0) {
            frame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }

        if (browserMode) {
            // A maximized Swing window tracks the responsive CheerpJ display.
            // Keep the minimum tiny so narrow and mobile viewports may shrink.
            frame.setMinimumSize(new Dimension(1, 1));
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            frame.setMinimumSize(new Dimension(960, 540));
            frame.setLocationRelativeTo(null);
            frame.addKeyListener(new FullscreenToggleKeyListener(frame));
        }
        frame.setVisible(true);
        game.requestFocusInWindow();
    }

    private static void installSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // The game renders its own UI, so this is only a small desktop nicety.
        }
    }

    private static final class FullscreenToggleKeyListener extends KeyAdapter {
        private final JFrame frame;

        private FullscreenToggleKeyListener(JFrame frame) {
            this.frame = frame;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() != KeyEvent.VK_F11) {
                return;
            }

            frame.dispose();
            if (frame.isUndecorated()) {
                frame.setUndecorated(false);
                frame.setExtendedState(JFrame.NORMAL);
            } else {
                frame.setUndecorated(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            frame.setVisible(true);
        }
    }
}
