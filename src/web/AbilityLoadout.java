public class AbilityLoadout {
    private String[] slots = new String[4];
    private static final String[] ALL_ABILITIES = {
        "void", "fade", "teleport", "shield", "timeslow", "drone", "nova"
    };
    
    public AbilityLoadout() {
        slots[0] = "void";
        slots[1] = "fade";
        slots[2] = "teleport";
        slots[3] = "shield";
    }
    
    public boolean setSlot(int index, String ability) {
        if (index < 0 || index > 3) return false;
        if (!isValidAbility(ability)) return false;
        slots[index] = ability;
        return true;
    }
    
    public String getSlot(int index) {
        return (index >= 0 && index < 4) ? slots[index] : null;
    }
    
    public String[] getAllAbilities() {
        return ALL_ABILITIES.clone();
    }
    
    private boolean isValidAbility(String ability) {
        for (String a : ALL_ABILITIES) {
            if (a.equals(ability)) return true;
        }
        return false;
    }
}
