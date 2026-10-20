import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;

public final class SettingsPanelRegressionTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Rectangle> bounds(SettingsPanel panel, String fieldName)
            throws ReflectiveOperationException {
        Field field = SettingsPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<String, Rectangle>) field.get(panel);
    }

    private static int scroll(SettingsPanel panel) throws ReflectiveOperationException {
        Field field = SettingsPanel.class.getDeclaredField("middleScrollOffset");
        field.setAccessible(true);
        return field.getInt(panel);
    }

    private static BufferedImage render(SettingsPanel panel, ControlConfig controls) {
        BufferedImage image = new BufferedImage(1000, 430, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            panel.draw(graphics, controls);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        ControlConfig controls = new ControlConfig();
        SettingsPanel panel = new SettingsPanel(1000, 430, controls);
        check(controls.getKey("void") == KeyEvent.VK_V, "dedicated Void key defaults to V");
        check(controls.getKey("constructTeleport") == KeyEvent.VK_1
                        && controls.getKey("constructDrone") == KeyEvent.VK_2
                        && controls.getKey("constructNova") == KeyEvent.VK_3
                        && controls.getKey("constructBlackHole") == KeyEvent.VK_4,
                "Ability construction defaults to keys 1 through 4");
        render(panel, controls);

        Rectangle category = bounds(panel, "categoryBounds").get("Key bindings");
        panel.handleClick(category.x + 10, category.y + 10, null);
        render(panel, controls);
        panel.handleScroll(20, 500, 0, 0, 1000, 430);
        BufferedImage keyImage = render(panel, controls);
        int offset = scroll(panel);
        check(offset > 0, "Key bindings content scrolls in a short window");
        Rectangle shoot = bounds(panel, "middleOptionBounds").get("shoot");
        check(shoot.x + shoot.width == 940, "Key binding cards leave room for scrollbar padding");
        check((keyImage.getRGB(960, 100) & 0xFFFFFF) != (keyImage.getRGB(950, 100) & 0xFFFFFF),
                "Scrollbar renders 20 pixels after key binding cards");

        Rectangle hyper = bounds(panel, "keyConfigBounds").get("hyper");
        int hyperY = hyper.y - offset + hyper.height / 2;
        check(hyperY >= 20 && hyperY < 410, "Hyper binding is visible after scrolling");
        panel.handleClick(hyper.x + 10, hyperY, null);
        panel.handleKeyPress(KeyEvent.VK_G);
        check(controls.getKey("hyper") == KeyEvent.VK_G,
                "Clicking a visible scrolled binding edits that binding");

        Rectangle droneConstruction = bounds(panel, "keyConfigBounds").get("constructDrone");
        int droneY = droneConstruction.y - offset + droneConstruction.height / 2;
        check(droneY >= 20 && droneY < 410,
                "Drone construction binding is visible after scrolling");
        panel.handleClick(droneConstruction.x + 10, droneY, null);
        panel.handleKeyPress(KeyEvent.VK_G);
        check(controls.getKey("constructDrone") == KeyEvent.VK_2,
                "A key already used by Hyper cannot be assigned to construction");
        panel.handleKeyPress(KeyEvent.VK_T);
        check(controls.getKey("constructDrone") == KeyEvent.VK_2,
                "The Developer terminal key is reserved");
        panel.handleKeyPress(KeyEvent.VK_C);
        check(controls.getKey("constructDrone") == KeyEvent.VK_2,
                "The Construct toggle key cannot be assigned to construction");
        panel.handleKeyPress(KeyEvent.VK_Y);
        check(controls.getKey("constructDrone") == KeyEvent.VK_Y,
                "A free key can be assigned to Drone construction");
        check(controls.setControl("constructDrone", KeyEvent.VK_Y),
                "Re-entering the current key keeps its binding");
        check(!controls.setControl("constructNova", KeyEvent.VK_C),
                "ControlConfig reserves C even outside the settings UI");
        check(!controls.setControl("constructNova", KeyEvent.VK_T),
                "ControlConfig reserves T even outside the settings UI");

        check(!bounds(panel, "categoryBounds").containsKey("Ability slots"),
                "obsolete Void slot placement section is removed");
        check(bounds(panel, "keyConfigBounds").containsKey("void"),
                "Void has a configurable key in Key bindings");

        Rectangle developerCategory = bounds(panel, "categoryBounds").get("Developer");
        panel.handleClick(developerCategory.x + 10, developerCategory.y + 10, null);
        render(panel, controls);
        Rectangle terminalToggle = bounds(panel, "middleOptionBounds").get("Enable terminal");
        panel.handleClick(terminalToggle.x + 10, terminalToggle.y + 10, null);
        check(controls.isDeveloperTerminalEnabled(), "Developer terminal toggles on in settings");
        panel.handleScroll(20, 100, 0, 0, 1000, 430);
        render(panel, controls);
        Field left = SettingsPanel.class.getDeclaredField("leftScrollOffset");
        left.setAccessible(true);
        int leftOffset = left.getInt(panel);
        Rectangle credits = bounds(panel, "categoryBounds").get("Credits");
        panel.handleClick(credits.x + 10, credits.y - leftOffset + 10, null);
        Field selected = SettingsPanel.class.getDeclaredField("selectedCategory");
        selected.setAccessible(true);
        check("Credits".equals(selected.get(panel)),
                "Sidebar category clicks remain aligned after scrolling");

        System.out.println("Settings panel regression checks passed: " + checks);
    }
}
