import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.Arrays;

/** The in-game construction menu. Game state and purchases belong to AsteroidGame. */
public final class ConstructPanel {
    public static final int FRONT_NOZZLES = 0;
    public static final int REAR_NOZZLES = 1;
    public static final int PIERCING = 2;
    public static final int BULLET_ENLARGEMENT = 3;
    public static final int TELEPORT = 0;
    public static final int DRONE = 1;
    public static final int NOVA = 2;
    public static final int BLACK_HOLE = 3;

    private static final Color ACCENT = new Color(130, 216, 232);
    private static final Color TEXT = new Color(235, 244, 250);
    private static final Color MUTED = new Color(165, 186, 200);
    private static final Color ERROR = new Color(255, 129, 139);
    private static final Font HEADING_FONT = new Font("SansSerif", Font.BOLD, 25);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font DETAIL_FONT = new Font("SansSerif", Font.PLAIN, 12);

    private static final String[] CATEGORY_NAMES = {
            "Utilities", "Weapons", "Abilities", "Return to Humanity"
    };
    private static final String[] CATEGORY_DESCRIPTIONS = {
            "Equip passive ship systems.",
            "Shape how your weapons fire.",
            "Learn to control the void.",
            "A path home awaits discovery."
    };
    private static final String[] WEAPON_NAMES = {
            "Front Nozzles", "Rear Nozzles", "Piercing", "Bullet Enlargement"
    };
    private static final String[] WEAPON_DESCRIPTIONS = {
            "Add two forward shots, angled wider each time.",
            "Add two rear shots, angled wider each time.",
            "Hit 3, 5, then unlimited targets per bullet.",
            "Larger bullets with a deeper purple color."
    };
    private static final String[][] WEAPON_LEVEL_STATS = {
            {"L1  3 forward shots (±15°)", "L2  5 forward shots (adds ±30°)",
                    "L3  7 forward shots (adds ±45°)"},
            {"L1  2 rear shots (±15°)", "L2  4 rear shots (adds ±30°)",
                    "L3  6 rear shots (adds ±45°)"},
            {"L1  Up to 3 targets per bullet", "L2  Up to 5 targets per bullet",
                    "L3  Unlimited targets per bullet"},
            {"L1  5 px bullet radius", "L2  7 px bullet radius",
                    "L3  9 px bullet radius"}
    };
    private static final String[] WEAPON_NOTES = {
            "Pairs also fire as Void lasers and from drones.",
            "Rear pairs also fire as Void lasers and from drones.",
            "Each bullet can hit more distinct targets.",
            "Base radius is 3 px; color deepens with level."
    };
    private static final String[] WEAPON_EXTRA = {
            "Each tier adds another pair 15° farther out.",
            "Each tier adds another pair 15° farther out.",
            "Each registered hit still deals normal bullet damage.",
            "Larger bullets keep their normal speed and lifetime."
    };
    private static final String[] ABILITY_NAMES = {
            "Teleport", "Drone", "Nova", "Black Hole"
    };
    private static final String[] ABILITY_IDS = {"teleport", "drone", "nova", "blackhole"};
    private static final String[] ABILITY_CONSTRUCT_ACTIONS = {
            "constructTeleport", "constructDrone", "constructNova", "constructBlackHole"
    };
    private static final String[] ABILITY_DESCRIPTIONS = {
            "Mark a safe destination, then jump.",
            "Deploy autonomous combat drones.",
            "Release an expanding energy blast.",
            "Charge and launch an absorbed black hole."
    };
    private static final String[][] ABILITY_LEVEL_STATS = {
            {"L1  10s cooldown", "L2  5s cooldown", "L3  2s cooldown"},
            {"L1  1 drone; fires every 5s", "L2  2 drones; fires every 3s",
                    "L3  3 drones; fires every 1s"},
            {"L1  30s cooldown", "L2  20s cooldown", "L3  10s cooldown"},
            {"L1  30s cooldown; absorb 1 hole", "L2  20s cooldown; absorb 2 holes",
                    "L3  10s cooldown; absorb 3 holes"}
    };
    private static final String[] ABILITY_NOTES = {
            "Safe jump in 0.5s; 0.25s arrival protection.",
            "One deploy charge every 12s; avoids black holes.",
            "1s charge; 1,000 px blast; clears Void energy.",
            "Hold to aim 80–820 px; purple holes resist lasers."
    };
    private static final String[] ABILITY_EXTRA = {
            "Landing stays clear of edges and obstacles when possible.",
            "Pursues within 450 px; fires within 165 px.",
            "Destroys asteroids, enemies, and black holes.",
            "Starts at 34 + 8 per absorbed hole, capped at 110."
    };
    private static final int[] ABILITY_ICONS = {7, 9, 10, 8};
    private static final String[] UTILITY_NAMES = {
            "Energy Modulator", "Swarm", "Metric Divergence Generator", "Shield"
    };
    private static final String[] UTILITY_DESCRIPTIONS = {
            "Actions drain Void energy above 50; absorb weak black holes at level 3.",
            "Spawn 2, 4, or 6 explosive tracking robots.",
            "Slow approaching hazards through visible space rings.",
            "A passive protective burst that recharges over time."
    };
    private static final String[][] UTILITY_LEVEL_STATS = {
            {"L1  1.25× action drain above 50", "L2  1.75× action drain above 50",
                    "L3  2.25× drain; absorb holes ≤32"},
            {"L1  2 bots every 10s", "L2  4 bots every 8s",
                    "L3  6 bots every 5s"},
            {"L1  3 rings; 0.8× speed per ring", "L2  4 rings; 0.5× speed per ring",
                    "L3  5 rings; 0.3× speed per ring"},
            {"L1  18s recharge; 160 px burst", "L2  12s recharge; 320 px burst",
                    "L3  7s recharge; full-screen burst"}
    };
    private static final String[] UTILITY_NOTES = {
            "Idle Void energy still rises; L3 absorbs small holes.",
            "85 px blasts; bots avoid black holes.",
            "260 px outer radius; asteroids and enemies only.",
            "Auto bursts near threats or at 100 Void energy."
    };
    private static final String[] UTILITY_EXTRA = {
            "In the Void, passive energy gain continues toward 100.",
            "First wave after 1s; each blast lasts 20 ticks.",
            "Bullets and Void lasers are unaffected.",
            "Triggers at 34 px or 100 Void energy; lasts 22 ticks."
    };

    public static final class Model {
        public final int essence;
        public final int[] weaponLevels;
        public final int[] abilityLevels;
        public final String[] slots;
        public final int pendingAbility;
        public final int[] utilityLevels;
        public final String[] utilitySlots;
        public final int pendingUtility;
        public final int absorbedBlackHoles;

        public Model(int essence, int[] weaponLevels, int[] abilityLevels,
                     String[] slots, int pendingAbility) {
            this(essence, weaponLevels, abilityLevels, slots, pendingAbility,
                    new int[4], new String[4], -1, 0);
        }

        public Model(int essence, int[] weaponLevels, int[] abilityLevels,
                     String[] slots, int pendingAbility, int[] utilityLevels,
                     String[] utilitySlots, int pendingUtility, int absorbedBlackHoles) {
            this.essence = Math.max(0, essence);
            this.weaponLevels = Arrays.copyOf(weaponLevels, 4);
            this.abilityLevels = Arrays.copyOf(abilityLevels, 4);
            this.slots = Arrays.copyOf(slots, 4);
            this.pendingAbility = pendingAbility;
            this.utilityLevels = Arrays.copyOf(utilityLevels, 4);
            this.utilitySlots = Arrays.copyOf(utilitySlots, 4);
            this.pendingUtility = pendingUtility;
            this.absorbedBlackHoles = Math.max(0, absorbedBlackHoles);
        }
    }

    public static final class Action {
        public enum Type {
            NONE, CLOSE, PURCHASE_WEAPON, PURCHASE_ABILITY, ASSIGN_SLOT,
            PURCHASE_UTILITY, ASSIGN_UTILITY_SLOT
        }

        public static final Action NONE = new Action(Type.NONE, -1);
        public static final Action CLOSE = new Action(Type.CLOSE, -1);
        public final Type type;
        public final int index;

        private Action(Type type, int index) {
            this.type = type;
            this.index = index;
        }

        private static Action weapon(int index) {
            return new Action(Type.PURCHASE_WEAPON, index);
        }

        private static Action ability(int index) {
            return new Action(Type.PURCHASE_ABILITY, index);
        }

        private static Action slot(int index) {
            return new Action(Type.ASSIGN_SLOT, index);
        }
        private static Action utility(int index) {
            return new Action(Type.PURCHASE_UTILITY, index);
        }
        private static Action utilitySlot(int index) {
            return new Action(Type.ASSIGN_UTILITY_SLOT, index);
        }
    }

    private enum View { CATEGORIES, UTILITIES, WEAPONS, ABILITIES, RETURN }

    private View view = View.CATEGORIES;
    private final Rectangle[] categoryBounds = new Rectangle[4];
    private final Rectangle[] optionBounds = new Rectangle[4];
    private Rectangle panelBounds = new Rectangle();
    private Rectangle closeBounds = new Rectangle();
    private Rectangle backBounds = new Rectangle();
    private Rectangle selectedAbilityBounds = new Rectangle();
    private int lastWidth = 960;
    private int lastHeight = 540;
    private String prompt = "";
    private boolean errorPrompt;

    public ConstructPanel() {
        for (int i = 0; i < categoryBounds.length; i++) categoryBounds[i] = new Rectangle();
        for (int i = 0; i < optionBounds.length; i++) optionBounds[i] = new Rectangle();
    }

    public void showError(String message) {
        prompt = message == null ? "" : message;
        errorPrompt = true;
    }

    public void showNotice(String message) {
        prompt = message == null ? "" : message;
        errorPrompt = false;
    }

    public void reset() {
        view = View.CATEGORIES;
        prompt = "";
    }

    public void showWeapons() {
        view = View.WEAPONS;
        prompt = "";
    }

    public void showUtilities() {
        view = View.UTILITIES;
        prompt = "";
    }

    public void showAbilities() {
        view = View.ABILITIES;
        prompt = "";
    }

    public Action handleKey(int keyCode, Model model) {
        return handleKey(keyCode, model, null);
    }

    public Action handleKey(int keyCode, Model model, ControlConfig controls) {
        enforcePendingView(model);
        if (keyCode == KeyEvent.VK_ESCAPE) return back(model);
        int number = keyCode - KeyEvent.VK_1;
        if (model.pendingUtility >= 0) {
            return number >= 0 && number < 4 ? chooseUtilitySlot(number, model) : Action.NONE;
        }
        if (model.pendingAbility >= 0) {
            return number >= 0 && number < 4 ? chooseSlot(number, model) : Action.NONE;
        }
        if (view == View.ABILITIES) {
            for (int i = 0; i < ABILITY_CONSTRUCT_ACTIONS.length; i++) {
                int optionKey = controls == null ? KeyEvent.VK_1 + i
                        : controls.getKey(ABILITY_CONSTRUCT_ACTIONS[i]);
                if (keyCode == optionKey) return chooseAbility(i, model);
            }
            return Action.NONE;
        }
        if (number < 0 || number > 3) return Action.NONE;
        if (view == View.CATEGORIES) {
            selectCategory(number);
            return Action.NONE;
        }
        if (view == View.UTILITIES) return chooseUtility(number, model);
        if (view == View.WEAPONS) return chooseWeapon(number, model);
        return Action.NONE;
    }

    public Action handleClick(int x, int y, Model model) {
        enforcePendingView(model);
        layout(lastWidth, lastHeight, model.pendingAbility >= 0 || model.pendingUtility >= 0);
        if (closeBounds.contains(x, y)) return back(model);
        if (view != View.CATEGORIES && backBounds.contains(x, y)) return back(model);
        if (model.pendingAbility >= 0 || model.pendingUtility >= 0) {
            for (int i = 0; i < optionBounds.length; i++) {
                if (optionBounds[i].contains(x, y)) return model.pendingUtility >= 0
                        ? chooseUtilitySlot(i, model) : chooseSlot(i, model);
            }
            return Action.NONE;
        }
        if (view == View.CATEGORIES) {
            for (int i = 0; i < categoryBounds.length; i++) {
                if (categoryBounds[i].contains(x, y)) {
                    selectCategory(i);
                    return Action.NONE;
                }
            }
        } else if (view == View.UTILITIES || view == View.WEAPONS || view == View.ABILITIES) {
            for (int i = 0; i < optionBounds.length; i++) {
                if (optionBounds[i].contains(x, y)) {
                    return view == View.UTILITIES ? chooseUtility(i, model)
                            : view == View.WEAPONS ? chooseWeapon(i, model)
                            : chooseAbility(i, model);
                }
            }
        }
        return Action.NONE;
    }

    public void draw(Graphics2D graphics, int width, int height, Model model) {
        draw(graphics, width, height, model, null);
    }

    public void draw(Graphics2D graphics, int width, int height, Model model,
                     ControlConfig controls) {
        enforcePendingView(model);
        layout(width, height, model.pendingAbility >= 0 || model.pendingUtility >= 0);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(3, 8, 19, 138));
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(15, 27, 44, 230));
            g.fillRoundRect(panelBounds.x, panelBounds.y,
                    panelBounds.width, panelBounds.height, 20, 20);
            g.setColor(new Color(95, 145, 170, 185));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(panelBounds.x, panelBounds.y,
                    panelBounds.width, panelBounds.height, 20, 20);
            drawHeader(g, model);
            if (model.pendingAbility >= 0 || model.pendingUtility >= 0) drawSlotSelection(g, model);
            else if (view == View.CATEGORIES) drawCategories(g);
            else if (view == View.UTILITIES) drawUtilityOptions(g, model);
            else if (view == View.WEAPONS) drawOptions(g, model, true, controls);
            else if (view == View.ABILITIES) drawOptions(g, model, false, controls);
            else drawReturn(g);
            drawFooter(g, model);
        } finally {
            g.dispose();
        }
    }

    private void layout(int width, int height, boolean placingAbility) {
        lastWidth = width;
        lastHeight = height;
        int marginX = Math.max(14, Math.min(30, width / 32));
        int marginY = Math.max(12, Math.min(24, height / 28));
        panelBounds.setBounds(marginX, marginY, width - 2 * marginX, height - 2 * marginY);
        closeBounds.setBounds(panelBounds.x + panelBounds.width - 48, panelBounds.y + 13, 32, 32);
        backBounds.setBounds(panelBounds.x + 15, panelBounds.y + 16, 65, 28);
        int contentX = panelBounds.x + 26;
        int contentWidth = panelBounds.width - 52;
        selectedAbilityBounds.setBounds(contentX, panelBounds.y + 91, contentWidth, 62);
        int contentY = panelBounds.y + (placingAbility ? 166 : 99);
        int contentHeight = Math.max(110, panelBounds.height - (placingAbility ? 218 : 151));
        int gap = 14;
        int categoryWidth = (contentWidth - 3 * gap) / 4;
        for (int i = 0; i < 4; i++) {
            categoryBounds[i].setBounds(contentX + i * (categoryWidth + gap),
                    contentY, categoryWidth, contentHeight);
        }
        int optionWidth = (contentWidth - gap) / 2;
        int optionHeight = (contentHeight - gap) / 2;
        for (int i = 0; i < 4; i++) {
            optionBounds[i].setBounds(contentX + (i % 2) * (optionWidth + gap),
                    contentY + (i / 2) * (optionHeight + gap), optionWidth, optionHeight);
        }
    }

    private void drawHeader(Graphics2D g, Model model) {
        int x = panelBounds.x;
        int y = panelBounds.y;
        if (view != View.CATEGORIES && model.pendingAbility < 0 && model.pendingUtility < 0) {
            g.setColor(MUTED);
            g.setFont(SMALL_FONT);
            g.drawString("< BACK", backBounds.x + 8, backBounds.y + 19);
        }
        String title = model.pendingUtility >= 0 ? "ASSIGN " + UTILITY_NAMES[model.pendingUtility].toUpperCase()
                : model.pendingAbility >= 0 ? "ASSIGN " + ABILITY_NAMES[model.pendingAbility].toUpperCase()
                : view == View.CATEGORIES ? "CONSTRUCT"
                : view == View.UTILITIES ? "UTILITIES"
                : view == View.WEAPONS ? "ARMORY"
                : view == View.ABILITIES ? "ABILITIES" : "RETURN TO HUMANITY";
        g.setFont(HEADING_FONT);
        g.setColor(TEXT);
        drawCentered(g, title, x + panelBounds.width / 2, y + 42);
        g.setFont(BODY_FONT);
        g.setColor(MUTED);
        String subtitle = model.pendingUtility >= 0
                ? "Choose an empty utility slot with 1-4 or click it."
                : model.pendingAbility >= 0
                ? "Choose an empty ability slot with 1-4 or click it."
                : view == View.CATEGORIES ? "Spend incident essence to construct your path forward."
                : view == View.RETURN ? "This construction will be specified later."
                : view == View.ABILITIES
                    ? "Ctrl+empty slot opens abilities; use construct keys or click."
                : view == View.UTILITIES ? "Passive systems: choose with 1-4 or click."
                    : "Ctrl+A opens armory; select with 1-4 or click.";
        drawCentered(g, subtitle, x + panelBounds.width / 2, y + 64);
        g.setColor(new Color(105, 79, 142, 205));
        g.fillRoundRect(x + panelBounds.width - 198, y + 58, 177, 28, 10, 10);
        g.setColor(new Color(222, 192, 248));
        g.setFont(SMALL_FONT);
        g.drawString("INCIDENT ESSENCE  " + model.essence,
                x + panelBounds.width - 187, y + 77);
        g.setColor(MUTED);
        g.setFont(TITLE_FONT);
        drawCentered(g, "×", closeBounds.x + 16, closeBounds.y + 23);
    }

    private void drawCategories(Graphics2D g) {
        for (int i = 0; i < 4; i++) {
            Rectangle r = categoryBounds[i];
            drawCard(g, r, i == 3, false);
            int iconY = r.y + r.height / 2 - 58;
            drawIcon(g, i == 0 ? 14 : i == 1 ? 0 : i == 2 ? 1 : 2,
                    r.x + r.width / 2, iconY, 29);
            g.setColor(TEXT);
            g.setFont(TITLE_FONT);
            drawCentered(g, CATEGORY_NAMES[i], r.x + r.width / 2, iconY + 57);
            g.setColor(MUTED);
            g.setFont(BODY_FONT);
            drawWrappedCentered(g, CATEGORY_DESCRIPTIONS[i], r.x + r.width / 2,
                    iconY + 84, r.width - 34, 2);
            g.setColor(ACCENT);
            g.setFont(SMALL_FONT);
            drawCentered(g, "[" + (i + 1) + "]", r.x + r.width / 2, r.y + r.height - 23);
        }
    }

    private void drawOptions(Graphics2D g, Model model, boolean weapons,
                             ControlConfig controls) {
        for (int i = 0; i < 4; i++) {
            Rectangle r = optionBounds[i];
            int level = weapons ? model.weaponLevels[i] : model.abilityLevels[i];
            int cost = weapons ? 5 : 10;
            boolean maxed = level >= 3;
            drawCard(g, r, maxed, false);
            int iconX = r.x + 38;
            int iconY = r.y + 32;
            drawIcon(g, weapons ? 3 + i : ABILITY_ICONS[i], iconX, iconY, 16);
            g.setFont(TITLE_FONT);
            g.setColor(TEXT);
            String shortcut = weapons ? Integer.toString(i + 1)
                    : controls == null ? Integer.toString(i + 1)
                    : controls.getKeyName(controls.getKey(ABILITY_CONSTRUCT_ACTIONS[i]));
            String equippedShortcut = null;
            if (!weapons && controls != null) {
                for (int slot = 0; slot < model.slots.length; slot++) {
                    if (level > 0 && ABILITY_IDS[i].equalsIgnoreCase(model.slots[slot])) {
                        String key = controls.getKeyName(controls.getKey("ability" + (slot + 1)));
                        equippedShortcut = "CTRL+" + key;
                        break;
                    }
                }
            }
            String heading = shortcut + "  " + (weapons ? WEAPON_NAMES[i] : ABILITY_NAMES[i]);
            drawFitted(g, heading, r.x + 70, r.y + 40,
                    r.width - (equippedShortcut == null ? 88 : 185));
            if (equippedShortcut != null) {
                g.setFont(SMALL_FONT);
                g.setColor(ACCENT);
                drawFittedRight(g, equippedShortcut, r.x + r.width - 18,
                        r.y + 38, Math.min(100, r.width / 4));
            }
            int holesNeeded = !weapons && i == BLACK_HOLE ? level + 1 : 0;
            String requirement = maxed ? "MAXED  ·  LEVEL 3/3"
                    : "NEXT L" + (level + 1) + "  ·  " + cost + " ESS"
                    + (holesNeeded > 0 ? "  ·  HOLES " + model.absorbedBlackHoles
                            + "/" + holesNeeded : "")
                    + (!weapons && level == 0 ? "  ·  OPEN SLOT" : "");
            boolean ready = model.essence >= cost
                    && (holesNeeded == 0 || model.absorbedBlackHoles >= holesNeeded)
                    && (weapons || level > 0 || hasOpenSlot(model));
            drawLevelDetails(g, r, level,
                    weapons ? WEAPON_LEVEL_STATS[i] : ABILITY_LEVEL_STATS[i],
                    weapons ? WEAPON_NOTES[i] : ABILITY_NOTES[i],
                    weapons ? WEAPON_EXTRA[i] : ABILITY_EXTRA[i], requirement,
                    maxed || ready);
        }
    }

    private void drawUtilityOptions(Graphics2D g, Model model) {
        for (int i = 0; i < 4; i++) {
            Rectangle r = optionBounds[i];
            int level = model.utilityLevels[i];
            boolean maxed = level >= 3;
            drawCard(g, r, maxed, false);
            drawIcon(g, 15 + i, r.x + 38, r.y + 32, 16);
            g.setFont(TITLE_FONT);
            g.setColor(TEXT);
            drawFitted(g, (i + 1) + "  " + UTILITY_NAMES[i],
                    r.x + 70, r.y + 40, r.width - 88);
            String requirement = maxed ? "MAXED  ·  LEVEL 3/3"
                    : "NEXT L" + (level + 1) + "  ·  10 ESS"
                    + (level == 0 ? "  ·  OPEN SLOT" : "");
            boolean ready = model.essence >= 10
                    && (level > 0 || hasOpenSlot(model.utilitySlots));
            drawLevelDetails(g, r, level, UTILITY_LEVEL_STATS[i], UTILITY_NOTES[i],
                    UTILITY_EXTRA[i],
                    requirement, maxed || ready);
        }
    }

    private static void drawLevelDetails(Graphics2D g, Rectangle r, int level,
                                         String[] stats, String note, String extra,
                                         String requirement, boolean ready) {
        int x = r.x + 20;
        int width = r.width - 40;
        g.setColor(new Color(100, 145, 167, 95));
        g.drawLine(x, r.y + 53, r.x + r.width - 20, r.y + 53);
        g.setFont(DETAIL_FONT);
        for (int tier = 0; tier < 3; tier++) {
            g.setColor(level > tier ? ACCENT : level == tier ? TEXT : MUTED);
            drawFitted(g, stats[tier], x, r.y + 72 + tier * 18, width);
        }
        if (r.height >= 190) {
            g.setColor(MUTED);
            drawFitted(g, note, x, r.y + 145, width);
        }
        if (r.height >= 215) {
            g.setColor(MUTED);
            drawFitted(g, extra, x, r.y + 165, width);
        }
        g.setFont(SMALL_FONT);
        g.setColor(ready ? ACCENT : ERROR);
        drawFitted(g, requirement, x, r.y + r.height - 17, width);
    }

    private void drawSlotSelection(Graphics2D g, Model model) {
        Rectangle selected = selectedAbilityBounds;
        drawCard(g, selected, false, true);
        boolean utility = model.pendingUtility >= 0;
        int pending = utility ? model.pendingUtility : model.pendingAbility;
        String[] names = utility ? UTILITY_NAMES : ABILITY_NAMES;
        String[] descriptions = utility ? UTILITY_DESCRIPTIONS : ABILITY_DESCRIPTIONS;
        String[] slots = utility ? model.utilitySlots : model.slots;
        drawIcon(g, utility ? 15 + pending : ABILITY_ICONS[pending],
                selected.x + 36, selected.y + selected.height / 2, 17);
        g.setColor(TEXT);
        g.setFont(TITLE_FONT);
        g.drawString(names[pending], selected.x + 70, selected.y + 26);
        g.setColor(ACCENT);
        g.setFont(BODY_FONT);
        drawWrapped(g, descriptions[pending], selected.x + 70,
                selected.y + 47, selected.width - 90, 2);

        for (int i = 0; i < 4; i++) {
            Rectangle r = optionBounds[i];
            String occupant = slots[i];
            boolean available = occupant == null || occupant.isBlank();
            drawCard(g, r, !available, available);
            drawIcon(g, 11 + i, r.x + 39, r.y + Math.min(45, r.height / 3), 18);
            g.setColor(TEXT);
            g.setFont(TITLE_FONT);
            g.drawString("SLOT " + (i + 1), r.x + 69, r.y + 40);
            g.setColor(available ? ACCENT : MUTED);
            g.setFont(BODY_FONT);
            g.drawString(available ? "Available" : "Occupied by " + titleCase(occupant),
                    r.x + 22, r.y + Math.min(81, r.height / 2 + 5));
            g.setFont(SMALL_FONT);
            g.drawString(available ? "CLICK OR PRESS " + (i + 1) : "UNAVAILABLE",
                    r.x + 22, r.y + r.height - 17);
        }
    }

    private void drawReturn(Graphics2D g) {
        int centerX = panelBounds.x + panelBounds.width / 2;
        int centerY = panelBounds.y + panelBounds.height / 2 + 4;
        drawIcon(g, 2, centerX, centerY - 38, 34);
        g.setColor(TEXT);
        g.setFont(TITLE_FONT);
        drawCentered(g, "Cosmic Teleportation Device", centerX, centerY + 25);
        g.setColor(MUTED);
        g.setFont(BODY_FONT);
        drawCentered(g, "Construction details will be available later.", centerX, centerY + 50);
    }

    private void drawFooter(Graphics2D g, Model model) {
        int centerX = panelBounds.x + panelBounds.width / 2;
        int baseline = panelBounds.y + panelBounds.height - 17;
        String text = prompt.isEmpty()
                ? model.pendingAbility >= 0 || model.pendingUtility >= 0
                    ? "Choose an available slot to finish construction."
                : view == View.CATEGORIES
                    ? "1 UTILITIES     2 WEAPONS     3 ABILITIES     4 RETURN"
                : "ESC BACK     C CLOSE"
                : prompt;
        g.setColor(errorPrompt && !prompt.isEmpty() ? ERROR : MUTED);
        g.setFont(SMALL_FONT);
        drawCentered(g, text, centerX, baseline);
    }

    private static void drawCard(Graphics2D g, Rectangle r, boolean muted, boolean highlighted) {
        g.setColor(highlighted ? new Color(30, 68, 75, 218)
                : muted ? new Color(24, 37, 52, 190) : new Color(31, 49, 67, 205));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(highlighted ? ACCENT : muted
                ? new Color(95, 125, 142, 125) : new Color(105, 153, 177, 175));
        g.setStroke(new BasicStroke(highlighted ? 2.2f : 1.1f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
    }

    private static void drawIcon(Graphics2D g, int shape, int x, int y, int radius) {
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (shape) {
            case 0 -> { // Weapons: crosshair.
                g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
                g.drawLine(x - radius, y, x - radius / 2 - 3, y);
                g.drawLine(x + radius / 2 + 3, y, x + radius, y);
                g.drawLine(x, y - radius, x, y - radius / 2 - 3);
                g.drawLine(x, y + radius / 2 + 3, x, y + radius);
            }
            case 1 -> { // Abilities: four-pointed star.
                g.drawPolygon(new int[] {x, x + radius / 3, x + radius, x + radius / 3,
                        x, x - radius / 3, x - radius, x - radius / 3},
                        new int[] {y - radius, y - radius / 3, y, y + radius / 3,
                                y + radius, y + radius / 3, y, y - radius / 3}, 8);
            }
            case 2 -> { // Return: portal.
                g.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
                g.drawOval(x - radius / 2, y - radius, radius, 2 * radius);
                g.drawLine(x - radius + 3, y, x + radius - 3, y);
            }
            case 3 -> { // Forward nozzles.
                g.drawLine(x - radius / 2, y + radius, x - radius / 2, y - radius);
                g.drawLine(x + radius / 2, y + radius, x + radius / 2, y - radius);
                g.drawLine(x - radius / 2 - 4, y - radius + 5, x - radius / 2, y - radius);
                g.drawLine(x + radius / 2 + 4, y - radius + 5, x + radius / 2, y - radius);
            }
            case 4 -> { // Rear nozzles.
                g.drawLine(x - radius / 2, y - radius, x - radius / 2, y + radius);
                g.drawLine(x + radius / 2, y - radius, x + radius / 2, y + radius);
                g.drawLine(x - radius / 2 - 4, y + radius - 5, x - radius / 2, y + radius);
                g.drawLine(x + radius / 2 + 4, y + radius - 5, x + radius / 2, y + radius);
            }
            case 5 -> { // Piercing: arrow through a ring.
                g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
                g.drawLine(x - radius, y, x + radius, y);
                g.drawLine(x + radius - 6, y - 6, x + radius, y);
                g.drawLine(x + radius - 6, y + 6, x + radius, y);
            }
            case 6 -> { // Bullet enlargement: nested growing rounds.
                g.fillOval(x - 3, y - 3, 6, 6);
                g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
                g.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
            }
            case 7 -> { // Teleport.
                g.drawPolygon(new int[] {x, x + radius, x, x - radius},
                        new int[] {y - radius, y, y + radius, y}, 4);
                g.fillOval(x - 2, y - 2, 4, 4);
            }
            case 8 -> { // Black Hole.
                g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
                g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
                g.fillOval(x - 3, y - 3, 6, 6);
            }
            case 9 -> { // Drone.
                g.drawPolygon(new int[] {x, x + radius, x - radius},
                        new int[] {y - radius, y + radius, y + radius}, 3);
                g.drawOval(x - 3, y, 6, 6);
            }
            case 10 -> { // Nova.
                g.drawOval(x - radius / 3, y - radius / 3, 2 * radius / 3, 2 * radius / 3);
                for (int a = 0; a < 8; a++) {
                    double angle = a * Math.PI / 4;
                    g.drawLine(x + (int) (radius * 0.55 * Math.cos(angle)),
                            y + (int) (radius * 0.55 * Math.sin(angle)),
                            x + (int) (radius * Math.cos(angle)),
                            y + (int) (radius * Math.sin(angle)));
                }
            }
            case 14, 15 -> { // Utility hub and energy modulator.
                g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
                g.fillOval(x - 3, y - 3, 6, 6);
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI / 2;
                    g.drawLine(x + (int) (radius * 0.6 * Math.cos(angle)),
                            y + (int) (radius * 0.6 * Math.sin(angle)),
                            x + (int) (radius * Math.cos(angle)),
                            y + (int) (radius * Math.sin(angle)));
                }
            }
            case 16 -> { // Swarm.
                g.drawOval(x - 4, y - radius, 8, 8);
                g.drawOval(x - radius, y + 3, 8, 8);
                g.drawOval(x + radius - 8, y + 3, 8, 8);
            }
            case 17 -> { // Metric rings.
                g.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
                g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
                g.fillOval(x - 2, y - 2, 4, 4);
            }
            case 18 -> { // Shield utility.
                g.drawPolygon(new int[] {x, x + radius, x + radius - 3,
                        x, x - radius + 3, x - radius},
                        new int[] {y - radius, y - radius / 2, y + radius / 2,
                                y + radius, y + radius / 2, y - radius / 2}, 6);
            }
            default -> {
                g.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
                g.drawString(Integer.toString(shape - 10), x - 4, y + 5);
            }
        }
    }

    private Action chooseWeapon(int index, Model model) {
        int max = 3;
        if (model.weaponLevels[index] >= max) {
            showError("This weapon is already fully constructed.");
            return Action.NONE;
        }
        int cost = 5;
        if (model.essence < cost) {
            showError("Need " + cost + " incident essence.");
            return Action.NONE;
        }
        prompt = "";
        return Action.weapon(index);
    }

    private Action chooseAbility(int index, Model model) {
        if (model.abilityLevels[index] >= 3) {
            showError("This ability is already fully constructed.");
            return Action.NONE;
        }
        if (model.essence < 10) {
            showError("Need 10 incident essence.");
            return Action.NONE;
        }
        if (index == BLACK_HOLE && model.absorbedBlackHoles < model.abilityLevels[index] + 1) {
            showError("Absorb " + (model.abilityLevels[index] + 1)
                    + " black holes before constructing this level.");
            return Action.NONE;
        }
        if (model.abilityLevels[index] == 0 && !hasOpenSlot(model)) {
            showError("All four ability slots are occupied.");
            return Action.NONE;
        }
        prompt = "";
        return Action.ability(index);
    }

    private Action chooseUtility(int index, Model model) {
        if (model.utilityLevels[index] >= 3) {
            showError("This utility is fully constructed.");
            return Action.NONE;
        }
        if (model.essence < 10) {
            showError("Need 10 incident essence.");
            return Action.NONE;
        }
        if (model.utilityLevels[index] == 0 && !hasOpenSlot(model.utilitySlots)) {
            showError("All four utility slots are occupied.");
            return Action.NONE;
        }
        prompt = "";
        return Action.utility(index);
    }

    private Action chooseSlot(int index, Model model) {
        if (index < 0 || index >= 4) return Action.NONE;
        String occupant = model.slots[index];
        if (occupant != null && !occupant.isBlank()) {
            showError("Slot " + (index + 1) + " is occupied. Choose a highlighted slot.");
            return Action.NONE;
        }
        prompt = "";
        return Action.slot(index);
    }

    private Action chooseUtilitySlot(int index, Model model) {
        if (index < 0 || index >= 4) return Action.NONE;
        if (model.utilitySlots[index] != null && !model.utilitySlots[index].isBlank()) {
            showError("Utility slot " + (index + 1) + " is occupied.");
            return Action.NONE;
        }
        prompt = "";
        return Action.utilitySlot(index);
    }

    private Action back(Model model) {
        if (model.pendingAbility >= 0 || model.pendingUtility >= 0) {
            showError("Choose a highlighted slot to finish construction.");
            return Action.NONE;
        }
        prompt = "";
        if (view == View.CATEGORIES) return Action.CLOSE;
        view = View.CATEGORIES;
        return Action.NONE;
    }

    private void selectCategory(int index) {
        view = index == 0 ? View.UTILITIES : index == 1 ? View.WEAPONS
                : index == 2 ? View.ABILITIES : View.RETURN;
        prompt = "";
    }

    private void enforcePendingView(Model model) {
        if (model.pendingAbility >= 0) view = View.ABILITIES;
        if (model.pendingUtility >= 0) view = View.UTILITIES;
    }

    private static boolean hasOpenSlot(Model model) { return hasOpenSlot(model.slots); }

    private static boolean hasOpenSlot(String[] slots) {
        for (String slot : slots) if (slot == null || slot.isBlank()) return true;
        return false;
    }

    private static String titleCase(String text) {
        if (text == null || text.isEmpty()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    private static void drawFitted(Graphics2D g, String text, int x, int baseline, int width) {
        drawFittedLine(g, text, x, baseline, width, false);
    }

    private static void drawFittedRight(Graphics2D g, String text, int right,
                                        int baseline, int width) {
        drawFittedLine(g, text, right, baseline, width, true);
    }

    private static void drawFittedLine(Graphics2D g, String text, int anchor,
                                       int baseline, int width, boolean alignRight) {
        Font original = g.getFont();
        float size = original.getSize2D();
        while (g.getFontMetrics().stringWidth(text) > width && size > 10) {
            size--;
            g.setFont(original.deriveFont(size));
        }
        String visible = text;
        int length = text.length();
        while (g.getFontMetrics().stringWidth(visible) > width && length > 0)
            visible = text.substring(0, --length) + "…";
        int x = alignRight ? anchor - g.getFontMetrics().stringWidth(visible) : anchor;
        g.drawString(visible, x, baseline);
        g.setFont(original);
    }

    private static void drawWrappedCentered(Graphics2D g, String text, int centerX,
                                            int baseline, int width, int maxLines) {
        String[] lines = wrap(g.getFontMetrics(), text, width, maxLines);
        for (int i = 0; i < lines.length; i++) drawCentered(g, lines[i], centerX, baseline + i * 18);
    }

    private static void drawWrapped(Graphics2D g, String text, int x,
                                    int baseline, int width, int maxLines) {
        String[] lines = wrap(g.getFontMetrics(), text, width, maxLines);
        for (int i = 0; i < lines.length; i++) g.drawString(lines[i], x, baseline + i * 18);
    }

    private static String[] wrap(FontMetrics metrics, String text, int width, int maxLines) {
        String[] words = text.split(" ");
        String[] lines = new String[maxLines];
        int lineCount = 0;
        String current = "";
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && metrics.stringWidth(candidate) > width && lineCount < maxLines - 1) {
                lines[lineCount++] = current;
                current = word;
            } else {
                current = candidate;
            }
        }
        lines[lineCount++] = current;
        return Arrays.copyOf(lines, lineCount);
    }
}
