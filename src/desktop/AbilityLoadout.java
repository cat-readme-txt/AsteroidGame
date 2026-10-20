import java.util.Arrays;

/** Four constructed ability slots; permanent Void has its own control. */
public class AbilityLoadout {
    public static final int SLOT_COUNT = 4;
    private static final String[] ALL_ABILITIES = {"teleport", "drone", "nova", "blackhole"};
    private final String[] slots = new String[SLOT_COUNT];

    public void reset() { Arrays.fill(slots, null); }
    public void resetForNewRun() { reset(); }

    public boolean setSlot(int index, String ability) {
        if (index < 0 || index >= SLOT_COUNT || !isValidAbility(ability)) return false;
        if (ability.equals(slots[index])) return true;
        if (!isSlotAvailable(index)) return false;
        int old = findSlot(ability);
        if (old >= 0) slots[old] = null;
        slots[index] = ability;
        return true;
    }

    public boolean setSlotForDeveloper(int index, String ability) {
        if (index < 0 || index >= SLOT_COUNT || !isValidAbility(ability)) return false;
        int old = findSlot(ability);
        if (old >= 0) slots[old] = null;
        slots[index] = ability;
        return true;
    }

    public boolean clearSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT) return false;
        slots[index] = null;
        return true;
    }

    public String getSlot(int index) {
        return index >= 0 && index < SLOT_COUNT ? slots[index] : null;
    }

    public boolean isSlotAvailable(int index) {
        return index >= 0 && index < SLOT_COUNT && slots[index] == null;
    }

    public int findSlot(String ability) {
        for (int i = 0; i < SLOT_COUNT; i++)
            if (ability != null && ability.equals(slots[i])) return i;
        return -1;
    }

    public String[] getAllAbilities() { return ALL_ABILITIES.clone(); }

    private boolean isValidAbility(String ability) {
        for (String name : ALL_ABILITIES) if (name.equals(ability)) return true;
        return false;
    }
}
