import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public final class ConstructPanelRegressionTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static ConstructPanel.Model model(int essence, int pendingAbility) {
        return new ConstructPanel.Model(essence, new int[4], new int[4],
                new String[] {pendingAbility >= 0 ? "teleport" : null, null, null, null}, pendingAbility);
    }

    private static ConstructPanel.Action.Type key(ConstructPanel panel, int key,
                                                  ConstructPanel.Model model) {
        return panel.handleKey(key, model).type;
    }

    public static void main(String[] args) {
        ConstructPanel panel = new ConstructPanel();
        ConstructPanel.Model funded = model(20, -1);
        check(key(panel, KeyEvent.VK_1, funded) == ConstructPanel.Action.Type.NONE,
                "1 selects utilities from the category screen");
        ConstructPanel.Action modulator = panel.handleKey(KeyEvent.VK_1, funded);
        check(modulator.type == ConstructPanel.Action.Type.PURCHASE_UTILITY && modulator.index == 0,
                "Utility shortcut 1 requests the Energy Modulator");
        check(key(panel, KeyEvent.VK_ESCAPE, funded) == ConstructPanel.Action.Type.NONE,
                "Escape returns from utilities");
        check(key(panel, KeyEvent.VK_2, funded) == ConstructPanel.Action.Type.NONE,
                "2 selects weapons from the category screen");
        ConstructPanel.Action front = panel.handleKey(KeyEvent.VK_1, funded);
        check(front.type == ConstructPanel.Action.Type.PURCHASE_WEAPON && front.index == 0,
                "Weapon shortcut 1 requests front nozzles");
        ConstructPanel.Action piercing = panel.handleKey(KeyEvent.VK_3, funded);
        check(piercing.type == ConstructPanel.Action.Type.PURCHASE_WEAPON && piercing.index == 2,
                "Weapon shortcut 3 requests piercing");
        ConstructPanel.Action enlargement = panel.handleKey(KeyEvent.VK_4, funded);
        check(enlargement.type == ConstructPanel.Action.Type.PURCHASE_WEAPON && enlargement.index == 3,
                "Weapon shortcut 4 requests bullet enlargement");
        check(key(panel, KeyEvent.VK_3, model(5, -1)) == ConstructPanel.Action.Type.PURCHASE_WEAPON,
                "Piercing costs five incident essence");
        check(key(panel, KeyEvent.VK_4, model(5, -1)) == ConstructPanel.Action.Type.PURCHASE_WEAPON,
                "Bullet enlargement costs five incident essence");
        check(key(panel, KeyEvent.VK_4, model(4, -1)) == ConstructPanel.Action.Type.NONE,
                "A weapon purchase is blocked below five essence");
        ConstructPanel.Model maxedWeapons = new ConstructPanel.Model(20,
                new int[] {3, 3, 3, 3}, new int[4], new String[4], -1);
        check(key(panel, KeyEvent.VK_3, maxedWeapons) == ConstructPanel.Action.Type.NONE,
                "Piercing stops at three constructions");
        check(key(panel, KeyEvent.VK_4, maxedWeapons) == ConstructPanel.Action.Type.NONE,
                "Bullet enlargement stops at three constructions");
        check(key(panel, KeyEvent.VK_1, model(0, -1)) == ConstructPanel.Action.Type.NONE,
                "Insufficient essence prevents a purchase request");
        check(key(panel, KeyEvent.VK_ESCAPE, funded) == ConstructPanel.Action.Type.NONE,
                "Escape goes back from a category");
        check(key(panel, KeyEvent.VK_3, funded) == ConstructPanel.Action.Type.NONE,
                "3 selects abilities from the category screen");
        ConstructPanel.Action teleport = panel.handleKey(KeyEvent.VK_1, funded);
        check(teleport.type == ConstructPanel.Action.Type.PURCHASE_ABILITY && teleport.index == 0,
                "Ability shortcut 1 requests teleport");
        ConstructPanel.Action drone = panel.handleKey(KeyEvent.VK_2, funded);
        ConstructPanel.Action nova = panel.handleKey(KeyEvent.VK_3, funded);
        check(drone.type == ConstructPanel.Action.Type.PURCHASE_ABILITY && drone.index == 1
                        && nova.type == ConstructPanel.Action.Type.PURCHASE_ABILITY && nova.index == 2,
                "Drone and Nova follow Teleport in the ability menu");
        ConstructPanel.Model absorbed = new ConstructPanel.Model(20, new int[4], new int[4],
                new String[4], -1, new int[4], new String[4], -1, 1);
        ConstructPanel.Action blackHole = panel.handleKey(KeyEvent.VK_4, absorbed);
        check(blackHole.type == ConstructPanel.Action.Type.PURCHASE_ABILITY
                        && blackHole.index == 3,
                "Black Hole is fourth after an absorption");
        ControlConfig configurableKeys = new ControlConfig();
        check(configurableKeys.setControl("constructTeleport", KeyEvent.VK_Y),
                "Teleport construction key can be rebound");
        check(panel.handleKey(KeyEvent.VK_1, funded, configurableKeys).type
                        == ConstructPanel.Action.Type.NONE,
                "Old ability construction key stops purchasing after rebind");
        ConstructPanel.Action reboundTeleport = panel.handleKey(KeyEvent.VK_Y, funded,
                configurableKeys);
        check(reboundTeleport.type == ConstructPanel.Action.Type.PURCHASE_ABILITY
                        && reboundTeleport.index == 0,
                "Rebound ability construction key purchases Teleport");

        ConstructPanel.Model pending = model(10, 0);
        check(key(panel, KeyEvent.VK_1, pending) == ConstructPanel.Action.Type.NONE,
                "Occupied slot 1 is rejected while ability placement is pending");
        ConstructPanel.Action slot = panel.handleKey(KeyEvent.VK_2, pending);
        check(slot.type == ConstructPanel.Action.Type.ASSIGN_SLOT && slot.index == 1,
                "Open slot 2 accepts ability placement");
        check(key(panel, KeyEvent.VK_ESCAPE, pending) == ConstructPanel.Action.Type.NONE,
                "Pending placement stays open until a valid slot is chosen");

        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.draw(graphics, 960, 540, pending);
        graphics.dispose();
        check(image.getRGB(480, 270) != 0, "Pending layout renders at 960x540");
        check(panel.handleClick(520, 130, pending).type == ConstructPanel.Action.Type.NONE,
                "Clicking the highlighted ability detail does not assign a slot");
        ConstructPanel.Action clickedSlot = panel.handleClick(520, 190, pending);
        check(clickedSlot.type == ConstructPanel.Action.Type.ASSIGN_SLOT && clickedSlot.index == 1,
                "Highlighted slot card responds to mouse click");

        BufferedImage largeImage = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D largeGraphics = largeImage.createGraphics();
        panel.draw(largeGraphics, 1200, 700, pending);
        largeGraphics.dispose();
        check(largeImage.getRGB(600, 350) != 0, "Pending layout renders at 1200x700");

        panel.reset();
        check(key(panel, KeyEvent.VK_4, funded) == ConstructPanel.Action.Type.NONE,
                "4 opens the return category");
        check(key(panel, KeyEvent.VK_1, funded) == ConstructPanel.Action.Type.NONE,
                "Return category has no purchase action yet");
        check(key(panel, KeyEvent.VK_ESCAPE, funded) == ConstructPanel.Action.Type.NONE,
                "Escape returns from the return category");
        check(key(panel, KeyEvent.VK_ESCAPE, funded) == ConstructPanel.Action.Type.CLOSE,
                "Escape closes the construct panel from its root");
        System.out.println("ConstructPanelRegressionTest: " + checks + " checks passed");
    }
}
