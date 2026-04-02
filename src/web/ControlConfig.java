import java.awt.event.KeyEvent;
import java.util.*;

public class ControlConfig {
    private Map<String, Integer> controls = new HashMap<>();
    private Map<Integer, String> reverseMap = new HashMap<>();
    
    public ControlConfig() {
        // Defaults
        controls.put("ability1", KeyEvent.VK_Q);
        controls.put("ability2", KeyEvent.VK_W);
        controls.put("ability3", KeyEvent.VK_E);
        controls.put("ability4", KeyEvent.VK_R);
        controls.put("shoot", KeyEvent.VK_A);
        controls.put("thrust", KeyEvent.VK_UP);
        controls.put("left", KeyEvent.VK_LEFT);
        controls.put("right", KeyEvent.VK_RIGHT);
        controls.put("hyper", KeyEvent.VK_SPACE);
        updateReverseMap();
    }
    
    private void updateReverseMap() {
        reverseMap.clear();
        for (Map.Entry<String, Integer> e : controls.entrySet()) {
            reverseMap.put(e.getValue(), e.getKey());
        }
    }
    
    public boolean setControl(String action, int keyCode) {
        if (reverseMap.containsKey(keyCode)) return false;
        controls.put(action, keyCode);
        updateReverseMap();
        return true;
    }
    
    public int getKey(String action) {
        return controls.getOrDefault(action, -1);
    }
    
    public String getKeyName(int keyCode) {
        return KeyEvent.getKeyText(keyCode);
    }
    
    public Map<String, Integer> getAllControls() {
        return new HashMap<>(controls);
    }
}