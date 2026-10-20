import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class DesktopLauncher {
    private static final String APP_TITLE = "Asteroid Game - Void Navigator";
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 700;

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DesktopLauncher::launch);
    }

    private static void launch() {
        installSystemLookAndFeel();

        JFrame frame = new JFrame(APP_TITLE);
        AsteroidGame game = new AsteroidGame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(game);
        frame.pack();
        if (frame.getWidth() <= 0 || frame.getHeight() <= 0) {
            frame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }

        frame.setMinimumSize(new Dimension(960, 540));
        frame.setLocationRelativeTo(null);
        frame.addKeyListener(new FullscreenToggleKeyListener(frame));
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
