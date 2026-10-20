import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/** In-game developer console. All mutations go through the game backend. */
public final class DeveloperTerminal {
    public interface Backend {
        String set(String target, int value);
        String reset(String target);
        String assignSlot(int slot, int abilityId, boolean unlock);
        boolean isAbilityLocked(int abilityId);
        List<Integer> availableSlots();
        String constructAbility(String ability, int level, int slot);
        String clear(String target);
        String spawn(String target, String variant, int count);
        Map<String, String> status();
    }

    private static final String[] ABILITIES = {"void", "teleport", "drone", "nova", "blackhole"};
    private final Backend backend;
    private final List<String> output = new ArrayList<>();
    private final StringBuilder input = new StringBuilder();
    private int scrollLines;
    private int pendingSlot = -1;
    private int pendingAbility = -1;
    private String pendingConstruction;
    private int pendingConstructionLevel;
    private int pendingAbsorptionLevel;
    private int pendingAbsorptionSlot = -1;

    public DeveloperTerminal(Backend backend) {
        this.backend = backend;
        print("DEVELOPER TERMINAL  |  type help for commands  |  T or Esc to close");
    }

    public void type(char c) {
        if (c >= 32 && c != 127 && input.length() < 180) input.append(c);
    }

    public void keyPressed(int code) {
        if (code == KeyEvent.VK_BACK_SPACE && input.length() > 0) input.deleteCharAt(input.length() - 1);
        else if (code == KeyEvent.VK_ENTER) {
            executeLine(input.toString());
            input.setLength(0);
        } else if (code == KeyEvent.VK_PAGE_UP) scrollLines += 10;
        else if (code == KeyEvent.VK_PAGE_DOWN) scrollLines = Math.max(0, scrollLines - 10);
    }

    public List<String> lines() { return List.copyOf(output); }
    public void scroll(int wheelRotation) {
        scrollLines = Math.max(0, scrollLines + wheelRotation * -4);
    }

    public void executeLine(String line) {
        String source = line.trim();
        if (source.isEmpty()) {
            if (pendingAbsorptionLevel > 0) {
                pendingAbsorptionLevel = 0;
                print("Black hole construction canceled.");
            }
            if (pendingConstruction != null) {
                pendingConstruction = null;
                pendingConstructionLevel = 0;
                print("No slot entered. Construction canceled; no level or slot changed.");
            }
            return;
        }
        print("> " + source);
        if (pendingAbsorptionLevel > 0) {
            int level = pendingAbsorptionLevel;
            if (source.equalsIgnoreCase("yes") || source.equalsIgnoreCase("y")) {
                pendingAbsorptionLevel = 0;
                print(backend.set("black-holes-absorbed", 1));
                if (pendingAbsorptionSlot >= 0) {
                    pendingSlot = pendingAbsorptionSlot;
                    pendingAbility = 4;
                    pendingAbsorptionSlot = -1;
                    print("Enable Black Hole before placing in slot " + pendingSlot + "? yes/no");
                } else if (level > 1) {
                    print("Level " + level + " requires " + level
                            + " absorbed black holes. Set the remaining count before constructing it.");
                } else beginConstruction("blackhole", level);
            } else if (source.equalsIgnoreCase("no") || source.equalsIgnoreCase("n")) {
                pendingAbsorptionLevel = 0;
                pendingAbsorptionSlot = -1;
                print("Black hole construction canceled.");
            } else print("Reply yes or no. Yes sets black holes absorbed to 1.");
            return;
        }
        if (pendingConstruction != null) {
            String ability = pendingConstruction;
            int level = pendingConstructionLevel;
            pendingConstruction = null;
            pendingConstructionLevel = 0;
            int slot;
            try { slot = Integer.parseInt(source); }
            catch (NumberFormatException exception) { print("Invalid slot. Construction canceled; no level or slot changed."); return; }
            if (!backend.availableSlots().contains(slot)) {
                print("Invalid or occupied slot " + slot + ". Construction canceled; no level or slot changed.");
                return;
            }
            print(backend.constructAbility(ability, level, slot));
            return;
        }
        if (pendingSlot >= 0) {
            if (source.equalsIgnoreCase("yes") || source.equalsIgnoreCase("y")) {
                print(backend.assignSlot(pendingSlot, pendingAbility, true));
                pendingSlot = pendingAbility = -1;
            } else if (source.equalsIgnoreCase("no") || source.equalsIgnoreCase("n")) {
                print(backend.assignSlot(pendingSlot, pendingAbility, false));
                pendingSlot = pendingAbility = -1;
            } else print("Reply yes or no. Yes unlocks it; no slots it unavailable (gray).");
            return;
        }
        String[] args = source.split("\\s+");
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> print("Commands: set, reset, ls, clear, spawn, status. Use COMMAND --help for syntax and examples.");
            case "set" -> runSet(args);
            case "reset" -> runReset(args);
            case "ls" -> runLs(args);
            case "clear" -> runClear(args);
            case "spawn" -> runSpawn(args);
            case "status" -> runStatus(args);
            default -> print("Unknown command '" + args[0] + "'. Type help.");
        }
    }

    private void runSet(String[] args) {
        if (args.length == 1 || (args.length == 2 && args[1].equals("--help"))) {
            print("Usage: set [-f|--force] <target> <integer>. Targets include black-holes-absorbed, utility.<name>.level, weapon.<name>.level, ability.<name>.level|cooldown, slot.1..4.");
            print("Use set <target> --help for that target's limits. Force only changes locked-slot assignment; it never bypasses value checks.");
            for (String target : new String[]{"level", "time", "cosmic-weight", "void-weight",
                    "essence", "fuel", "void-energy", "ship.x", "ship.y",
                    "weapon.front.level", "weapon.rear.level", "weapon.piercing.level",
                    "weapon.enlargement.level", "ability.teleport.level",
                    "ability.teleport.cooldown", "slot.1"}) print(setUsage(target));
            print("The same construction rules apply to blackhole, drone, and nova; Void is permanent in its own slot.");
            return;
        }
        int index = 1;
        boolean force = false;
        if (args[index].equals("-f") || args[index].equals("--force") || args[index].equals("-force")) {
            force = true;
            index++;
        }
        if (index >= args.length) { print("Usage: set [-f|--force] <target> <integer>"); return; }
        String target = args[index].toLowerCase(Locale.ROOT);
        String usage = setUsage(target);
        if (usage == null) { print("Unknown set target '" + target + "'. Use ls --targets."); return; }
        if (args.length == index + 2 && args[index + 1].equals("--help")) { print(usage); return; }
        if (args.length != index + 2) { print("Missing or extra value. " + usage); return; }
        int value;
        try { value = Integer.parseInt(args[index + 1]); }
        catch (NumberFormatException exception) { print("Value must be an integer. " + usage); return; }
        if (!validValue(target, value)) { print("Invalid value '" + value + "'. " + usage); return; }
        if (target.matches("ability\\.(teleport|blackhole|drone|nova)\\.level") && value > 0
                && "0".equals(backend.status().get(target))) {
            if (backend.availableSlots().isEmpty()) {
                print("No available ability slots. Construction canceled.");
                return;
            }
            if (target.equals("ability.blackhole.level")
                    && "0".equals(backend.status().get("black-holes-absorbed"))) {
                pendingAbsorptionLevel = value;
                print("No black hole has been absorbed. Set black holes absorbed to 1? yes/no");
                return;
            }
            beginConstruction(target.split("\\.")[1], value);
            return;
        }
        if (target.startsWith("slot.")) {
            int slot = target.charAt(5) - '0';
            if (backend.isAbilityLocked(value) && !force) {
                print("Ability " + ABILITIES[value] + " is locked. No change. Use set -f " + target + " " + value + " to choose whether to unlock it.");
                return;
            }
            if (backend.isAbilityLocked(value)) {
                if (value == 4 && "0".equals(backend.status().get("black-holes-absorbed"))) {
                    pendingAbsorptionLevel = 1;
                    pendingAbsorptionSlot = slot;
                    print("No black hole has been absorbed. Set black holes absorbed to 1? yes/no");
                    return;
                }
                pendingSlot = slot;
                pendingAbility = value;
                print("Ability " + ABILITIES[value] + " is locked. Enable it before placing in slot " + slot + "? yes/no");
                return;
            }
            print(backend.assignSlot(slot, value, false));
            return;
        }
        print(backend.set(target, value));
    }

    private void beginConstruction(String ability, int level) {
        List<Integer> slots = backend.availableSlots();
        if (slots.isEmpty()) { print("No available ability slots. Construction canceled."); return; }
        pendingConstruction = ability;
        pendingConstructionLevel = level;
        print("Choose an available slot " + slots + " for " + ability
                + ". Enter one number; any invalid entry cancels without changing the ability.");
    }

    private static String setUsage(String target) {
        return switch (target) {
            case "level" -> "Usage: set level <0..999>. Sets current level and updates spawns.";
            case "time" -> "Usage: set time <0..30>. Seconds elapsed in the current level.";
            case "cosmic-weight", "void-weight" -> "Usage: set " + target + " <0..10>. Immediately adjusts spawns and freezes this weight across later levels.";
            case "essence" -> "Usage: set essence <1..999999>. Sets Incident Essence; zero and negative values are invalid.";
            case "fuel" -> "Usage: set fuel <0..1000>. Sets current ship M-AM reserve.";
            case "void-energy" -> "Usage: set void-energy <0..100>. Sets current Void energy; 100 can trigger the Void death condition.";
            case "black-holes-absorbed" -> "Usage: set black-holes-absorbed <0..999>. Sets absorbed black hole count.";
            case "ship.x", "ship.y" -> "Usage: set " + target + " <0..screen edge>. Moves the ship to a screen coordinate.";
            default -> {
                if (target.matches("weapon\\.(front|rear|piercing|enlargement)\\.level")) yield "Usage: set " + target + " <0..3>. Directly sets a weapon construction level.";
                if (target.matches("utility\\.(modulator|swarm|metric|shield)\\.level")) yield "Usage: set " + target + " <0..3>. Sets a passive utility level.";
                if (target.matches("slot\\.[1-4]")) yield "Usage: set [-f|--force] " + target + " <ability-id 1..4>. See ls --abilities. Force prompts before slotting locked abilities.";
                if (target.matches("ability\\.(void|teleport|blackhole|drone|nova)\\.level")) yield "Usage: set " + target + " <" + (target.startsWith("ability.void.") ? "1" : "0..3") + ">. Construction level; Void is permanent.";
                if (target.matches("ability\\.(void|teleport|blackhole|drone|nova)\\.cooldown")) yield "Usage: set " + target + " <0..3600>. Seconds remaining; zero refreshes immediately. Black Hole cooldown is 30/20/10s.";
                yield null;
            }
        };
    }

    private static boolean validValue(String target, int value) {
        if (target.startsWith("slot.")) return value >= 1 && value < ABILITIES.length;
        if (target.startsWith("weapon.") && target.endsWith(".level")) return value >= 0 && value <= 3;
        if (target.endsWith(".level")) return target.startsWith("ability.void.") ? value == 1 : value >= 0 && value <= 3;
        if (target.endsWith(".cooldown")) return value >= 0 && value <= 3600;
        return switch (target) {
            case "level" -> value >= 0 && value <= 999;
            case "time" -> value >= 0 && value <= 30;
            case "cosmic-weight", "void-weight" -> value >= 0 && value <= 10;
            case "essence" -> value >= 1 && value <= 999999;
            case "fuel" -> value >= 0 && value <= 1000;
            case "void-energy" -> value >= 0 && value <= 100;
            case "black-holes-absorbed" -> value >= 0 && value <= 999;
            case "ship.x", "ship.y" -> value >= 0 && value <= 10000;
            default -> false;
        };
    }

    private void runReset(String[] args) {
        if (args.length == 1 || (args.length == 2 && args[1].equals("--help"))) {
            print("Usage: reset <set-target>. Restores that target's new-run default. Weight targets recalculate from the current level and resume normal level progression.");
            print("Examples: reset void-weight; reset weapon.front.level; reset ability.teleport.cooldown; reset slot.1. Use ls --targets for target names.");
            return;
        }
        String target = args[1].toLowerCase(Locale.ROOT);
        String usage = setUsage(target);
        if (usage == null) { print("Unknown reset target '" + target + "'. Use ls --targets."); return; }
        if (args.length == 3 && args[2].equals("--help")) {
            print("Usage: reset " + target + ". Restores its new-run default. " + usage);
            return;
        }
        if (args.length != 2) { print("Usage: reset " + target + ". Enter exactly one target."); return; }
        print(backend.reset(target));
    }

    private void runLs(String[] args) {
        if (args.length != 2) { print("Usage: ls --abilities|--targets|--variables/status|--help"); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "--help" -> print("Usage: ls --abilities (IDs for slots), ls --targets (set/clear targets), ls --variables/status (readable status keys).");
            case "--abilities" -> {
                print("0 = void (permanent, dedicated slot; not assignable to slots 1..4)");
                for (int i = 1; i < ABILITIES.length; i++) print(i + " = " + ABILITIES[i]);
            }
            case "--targets" -> {
                print("SET: level, time, cosmic-weight, void-weight, essence, fuel, void-energy, black-holes-absorbed, ship.x, ship.y, slot.1..4");
                print("SET: weapon.<front|rear|piercing|enlargement>.level");
                print("SET: utility.<modulator|swarm|metric|shield>.level");
                print("SET: ability.<void|teleport|blackhole|drone|nova>.<level|cooldown>");
                print("RESET: every SET target; reset --help explains defaults");
                print("CLEAR: bullets, lasers, drones, asteroids, cosmic entities, void creatures, black holes, particles");
                print("SPAWN: asteroid, cosmic, void, black-hole, drone (use spawn --help)");
            }
            case "--variables/status", "--variables", "--status" -> {
                print("STATUS VARIABLES:");
                print(String.join(", ", backend.status().keySet()));
            }
            default -> print("Unknown ls tag. Usage: ls --abilities|--targets|--variables/status|--help");
        }
    }

    private void runClear(String[] args) {
        if (args.length == 2 && args[1].equals("--help")) {
            print("Usage: clear <bullets|lasers|drones|asteroids|cosmic entities|void creatures|black holes|particles>. Objects can respawn through normal game rules.");
            return;
        }
        if (args.length < 2) { print("Usage: clear <target>. Use clear --help for valid targets."); return; }
        if (args[args.length - 1].equals("--help")) {
            String helpTarget = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
            print("Usage: clear " + helpTarget + ". Removes current objects immediately; normal spawns may replace them.");
            return;
        }
        String target = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase(Locale.ROOT);
        if (!Set.of("bullets", "lasers", "drones", "asteroids", "cosmic entities", "void creatures", "black holes", "particles").contains(target)) {
            print("Unknown clear target '" + target + "'. Use clear --help.");
            return;
        }
        print(backend.clear(target));
    }

    private void runSpawn(String[] args) {
        if (args.length == 1 || (args.length == 2 && args[1].equals("--help"))) {
            print("Usage: spawn asteroid <small|medium|large> [count 1..10]");
            print("       spawn cosmic|void <small|medium|large> [count 1..10]");
            print("       spawn black-hole|drone [count 1..10]. Runtime population caps still apply.");
            return;
        }
        String target = args[1].toLowerCase(Locale.ROOT);
        boolean variantRequired = Set.of("asteroid", "cosmic", "void").contains(target);
        boolean noVariant = Set.of("black-hole", "drone").contains(target);
        if (!variantRequired && !noVariant) {
            print("Unknown spawn target '" + target + "'. Use spawn --help.");
            return;
        }
        String usage = variantRequired
                ? "Usage: spawn " + target + " <small|medium|large> [count 1..10]"
                : "Usage: spawn " + target + " [count 1..10]";
        if (args.length == 3 && args[2].equals("--help")) { print(usage); return; }
        int minArgs = variantRequired ? 3 : 2;
        if (args.length < minArgs || args.length > minArgs + 1) { print(usage); return; }
        String variant = variantRequired ? args[2].toLowerCase(Locale.ROOT) : "";
        if (variantRequired && !Set.of("small", "medium", "large").contains(variant)) {
            print("Invalid variant '" + variant + "'. " + usage);
            return;
        }
        int count = 1;
        if (args.length == minArgs + 1) {
            try { count = Integer.parseInt(args[minArgs]); }
            catch (NumberFormatException exception) { print("Count must be an integer. " + usage); return; }
        }
        if (count < 1 || count > 10) { print("Count must be 1..10. " + usage); return; }
        print(backend.spawn(target, variant, count));
    }

    private void runStatus(String[] args) {
        if (args.length == 2 && args[1].equals("--help")) {
            print("Usage: status [--all|--<variable> ...]. Examples: status --level --essence; status --ability.teleport.cooldown. See ls --variables/status.");
            return;
        }
        Map<String, String> status = backend.status();
        if (args.length == 1 || (args.length == 2 && args[1].equals("--all"))) {
            status.forEach((key, value) -> print(key + " = " + value));
            return;
        }
        for (int i = 1; i < args.length; i++) {
            String name = args[i].startsWith("--") ? args[i].substring(2) : args[i];
            String value = status.get(name);
            if (value == null) print("Unknown status variable '" + name + "'. See ls --variables/status.");
            else print(name + " = " + value);
        }
    }

    private void print(String line) {
        output.add(line);
        if (output.size() > 600) output.remove(0);
        scrollLines = 0;
    }

    public void draw(Graphics2D g2, int width, int height) {
        int x = Math.max(24, width / 16), y = Math.max(24, height / 16);
        int w = width - 2 * x, h = height - 2 * y;
        g2.setColor(new Color(4, 9, 17, 246));
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setColor(new Color(75, 210, 165, 200));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, w, h, 14, 14);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        g2.setColor(new Color(140, 235, 190));
        g2.drawString("DEVELOPER TERMINAL", x + 24, y + 30);
        Shape clip = g2.getClip();
        g2.setClip(x + 20, y + 44, w - 40, h - 94);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        FontMetrics fm = g2.getFontMetrics();
        int maxChars = Math.max(30, (w - 56) / Math.max(1, fm.charWidth('M')));
        List<String> wrapped = new ArrayList<>();
        for (String line : output) {
            if (line.length() == 0) { wrapped.add(""); continue; }
            for (int i = 0; i < line.length(); i += maxChars) wrapped.add(line.substring(i, Math.min(line.length(), i + maxChars)));
        }
        int visible = Math.max(1, (h - 104) / 18);
        int end = Math.max(0, wrapped.size() - Math.min(scrollLines, wrapped.size()));
        int start = Math.max(0, end - visible);
        int textY = y + 63;
        g2.setColor(new Color(210, 235, 220));
        for (int i = start; i < end; i++) { g2.drawString(wrapped.get(i), x + 24, textY); textY += 18; }
        g2.setClip(clip);
        g2.setColor(new Color(35, 55, 65, 210));
        g2.fillRoundRect(x + 18, y + h - 45, w - 36, 30, 5, 5);
        g2.setColor(new Color(155, 245, 190));
        String prompt = "> " + input + "_";
        if (prompt.length() > maxChars) prompt = prompt.substring(prompt.length() - maxChars);
        g2.drawString(prompt, x + 26, y + h - 24);
    }
}
