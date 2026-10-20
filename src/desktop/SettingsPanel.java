import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

public class SettingsPanel {
    // General
    private int WIDTH;
    private int HEIGHT;
    private final int PADDING = 60;
    // Panel state
    private String selectedCategory = "Key bindings";
    private String hoveredCategory = null;
    private int leftScrollOffset = 0;
    private int middleScrollOffset = 0;
    private int rightScrollOffset = 0;
    private int maxLeftScroll = 0;
    private int maxMiddleScroll = 0;
    private int maxRightScroll = 0;
    // Add scrollbar dragging states for each column
    private boolean draggingLeftScrollbar = false;
    private boolean draggingMiddleScrollbar = false;
    private boolean draggingRightScrollbar = false;
    private int scrollbarDragStartY = 0;
    private int scrollbarDragStartOffset = 0;
    // Middle section state
    private String selectedMiddleOption = null;
    private String hoveredMiddleOption = null;
    private final Map<String, Rectangle> middleOptionBounds = new LinkedHashMap<>();
    // Right section state
    private String hoveredRightOption = null;
    private final Map<String, Rectangle> rightOptionBounds = new LinkedHashMap<>();
    // Button and category bounds
    private Rectangle closeButtonBounds;
    private boolean closeButtonHovered = false;
    private final Map<String, Rectangle> categoryBounds = new LinkedHashMap<>();
    
    // Settings values
    private boolean invertX = false;
    private boolean vibration = true;
    private int vibrationStrength = 100;
    private int movementSensitivity = 80;
    private int sfxVolume = 70;
    private int musicVolume = 50;
    private boolean effects = true;
    private boolean vSync = true;
    private int fps = 60;
    private boolean screenShake = true;
    private boolean windowedMode = false;
    private String colorBlindMode = "NORMAL";
    private String language = "English";
    
    // Slider tracking
    private String activeSlider = null;

    // Control Config
    private final ControlConfig controlConfig;
    private String configuringAction; 
    private boolean waitingForKey;
    private String keyBindingError;
    private int hoveredKeyConfig = -1; 
    private Map<String, Rectangle> keyConfigBounds = new LinkedHashMap<>();
    
    public SettingsPanel(int WIDTH, int HEIGHT, ControlConfig controlConfig) {
        this.WIDTH = WIDTH;
        this.HEIGHT = HEIGHT;
        this.controlConfig = controlConfig;
        categoryBounds.put("Key bindings", new Rectangle());
        categoryBounds.put("Developer", new Rectangle());
        categoryBounds.put("Controller", new Rectangle());
        categoryBounds.put("Graphics", new Rectangle());
        categoryBounds.put("Audio", new Rectangle());
        categoryBounds.put("Language", new Rectangle());
        categoryBounds.put("Credits", new Rectangle());
    }

    public void updateDimensions(int newWidth, int newHeight) {
        WIDTH = newWidth;
        HEIGHT = newHeight;
        // Reset scroll offsets to prevent out-of-bounds scrolling
        leftScrollOffset = Math.min(leftScrollOffset, maxLeftScroll);
        middleScrollOffset = Math.min(middleScrollOffset, maxMiddleScroll);
        rightScrollOffset = Math.min(rightScrollOffset, maxRightScroll);
    }
    
    public boolean handleClick(int mx, int my, AsteroidGame game) {
        // Check close button
        if (closeButtonBounds != null && closeButtonBounds.contains(mx, my)) {
            return true; // Close button was clicked
        }
        
        // Check category selection (left column)
        for (Map.Entry<String, Rectangle> entry : categoryBounds.entrySet()) {
            if (my >= 60 && my < HEIGHT - 20
                    && entry.getValue().contains(mx, my + leftScrollOffset)) {
                selectedCategory = entry.getKey();
                leftScrollOffset = 0;
                middleScrollOffset = 0;
                rightScrollOffset = 0;
                selectedMiddleOption = null;
                return false;
            }
        }
        
        // Bounds are stored in content coordinates; scrolling moves only the drawing.
        if (my >= 20 && my < HEIGHT - 20) {
            int contentY = my + middleScrollOffset;
            for (Map.Entry<String, Rectangle> entry : middleOptionBounds.entrySet()) {
                if (entry.getValue().contains(mx, contentY)) {
                    handleMiddleClick(entry.getKey(), mx, contentY);
                    return false;
                }
            }
        }
        
        // Check right section clicks
        for (Map.Entry<String, Rectangle> entry : rightOptionBounds.entrySet()) {
            if (entry.getValue().contains(mx, my)) {
                handleRightClick(entry.getKey(), mx, my);
                return false;
            }
        }
        
        return false;
    }

    private void handleMiddleClick(String option, int mx, int my) {
        switch (selectedCategory) {
            case "Developer" -> {
                if (option.equals("Enable terminal")) {
                    controlConfig.setDeveloperTerminalEnabled(!controlConfig.isDeveloperTerminalEnabled());
                }
            }
            case "Key bindings" -> {
                for (Map.Entry<String, Rectangle> entry : keyConfigBounds.entrySet()) {
                    if (entry.getValue().contains(mx, my)) {
                        configuringAction = entry.getKey();
                        waitingForKey = true;
                        keyBindingError = null;
                        return;
                    }
                }
            }
            case "Controller" -> {
            switch (option) {
                case "Invert X" -> invertX = !invertX;
                case "Vibration" -> vibration = !vibration;
                case "Screen shake" -> screenShake = !screenShake;
                case "Restore to default" -> {
                    invertX = false;
                    vibration = true;
                    vibrationStrength = 100;
                    movementSensitivity = 80;
                    screenShake = true;
                }
                default -> {
                }
            }
            }
            case "Graphics" -> {
                if (option.equals("Fullscreen resolution")) {
                    selectedMiddleOption = option;
                    rightScrollOffset = 0;
                } else if (option.equals("Effects")) {
                    effects = !effects;
                } else if (option.equals("V-Sync")) {
                    vSync = !vSync;
                } else if (option.equals("Screen shake")) {
                    screenShake = !screenShake;
                } else if (option.equals("Windowed mode")) {
                    windowedMode = !windowedMode;
                } else if (option.equals("Color blind mode")) {
                    selectedMiddleOption = option;
                    rightScrollOffset = 0;
                } else if (option.equals("Restore to default")) {
                    effects = true;
                    vSync = true;
                    fps = 60;
                    screenShake = true;
                    windowedMode = false;
                    colorBlindMode = "NORMAL";
                }
            }
            case "Audio" -> {
                if (option.equals("Restore to default")) {
                    sfxVolume = 70;
                    musicVolume = 50;
                }
            }
        }
    }

    private void handleRightClick(String option, int mx, int my) {
        if (selectedCategory.equals("Graphics") && selectedMiddleOption != null) {
            if (selectedMiddleOption.equals("Fullscreen resolution")) {
                // Parse resolution from option string (e.g., "1920x1080")
                // In a real implementation, you'd apply this resolution
            } else if (selectedMiddleOption.equals("Color blind mode")) {
                colorBlindMode = option;
            } 
        }
    }
    
    public void handleMouseMove(int mx, int my) {
        hoveredCategory = null;
        hoveredMiddleOption = null;
        hoveredRightOption = null;
        hoveredKeyConfig = -1;
        closeButtonHovered = false;
        
        // Check close button hover
        if (closeButtonBounds != null && closeButtonBounds.contains(mx, my)) {
            closeButtonHovered = true;
            return;
        }
        
        // Check category hovers
        for (Map.Entry<String, Rectangle> entry : categoryBounds.entrySet()) {
            if (my >= 60 && my < HEIGHT - 20
                    && entry.getValue().contains(mx, my + leftScrollOffset)) {
                hoveredCategory = entry.getKey();
                return;
            }
        }
        
        // Check middle section hovers in the same content coordinates as clicks.
        int contentY = my + middleScrollOffset;
        if (my >= 20 && my < HEIGHT - 20 && selectedCategory.equals("Key bindings")) {
            int index = 0;
            for (Map.Entry<String, Rectangle> entry : keyConfigBounds.entrySet()) {
                if (entry.getValue().contains(mx, contentY)) {
                    hoveredKeyConfig = index;
                    hoveredMiddleOption = entry.getKey();
                    return;
                }
                index++;
            }
        }
        if (my >= 20 && my < HEIGHT - 20) {
            for (Map.Entry<String, Rectangle> entry : middleOptionBounds.entrySet()) {
                if (entry.getValue().contains(mx, contentY)) {
                    hoveredMiddleOption = entry.getKey();
                    return;
                }
            }
        }
        
        // Check right section hovers
        for (Map.Entry<String, Rectangle> entry : rightOptionBounds.entrySet()) {
            if (entry.getValue().contains(mx, my)) {
                hoveredRightOption = entry.getKey();
                return;
            }
        }
    }

    public String hoveredActionId() {
        if (closeButtonHovered) return "close";
        if (hoveredCategory != null) return "category:" + hoveredCategory;
        if (hoveredMiddleOption != null) return "option:" + hoveredMiddleOption;
        if (hoveredRightOption != null) return "value:" + hoveredRightOption;
        return null;
    }
    
    public void handleScroll(int rotation, int mx, int panelX, int panelY, int panelWidth, int panelHeight) {
        int leftColumnWidth = (int)(WIDTH * 0.25);
        int dividerX = leftColumnWidth + 20;
        int contentX = dividerX + 20;
        int contentWidth = WIDTH - leftColumnWidth - PADDING;
        int middleWidth = shouldShowRightPanel()
            ? (contentWidth - 80) / 2 : contentWidth - 40;
        int rightX = contentX + middleWidth + 48;
        
        // Determine which column to scroll based on mouse position
        if (mx < dividerX) {
            // Left column
            leftScrollOffset += rotation * 30;
            leftScrollOffset = Math.max(0, Math.min(leftScrollOffset, maxLeftScroll));
        } else if (mx < rightX) {
            // Middle column
            middleScrollOffset += rotation * 30;
            middleScrollOffset = Math.max(0, Math.min(middleScrollOffset, maxMiddleScroll));
        } else {
            // Right column
            rightScrollOffset += rotation * 30;
            rightScrollOffset = Math.max(0, Math.min(rightScrollOffset, maxRightScroll));
        }
    }
    
    public void handleMouseDrag(int mx, int my, int panelX, int panelY, int panelWidth, int panelHeight) {
        if (activeSlider != null) {
            int leftColumnWidth = (int)(WIDTH * 0.25);
            int dividerX = leftColumnWidth + 20;
            int contentX = dividerX + 20;
            
            int sliderWidth = 150;
            int sliderX = contentX + 550;
            float percent = Math.max(0, Math.min(1, (mx - sliderX) / (float)sliderWidth));
            
            switch (activeSlider) {
                case "vibrationStrength" -> vibrationStrength = (int)(percent * 100);
                case "movementSensitivity" -> movementSensitivity = (int)(percent * 100);
                case "sfxVolume" -> sfxVolume = (int)(percent * 100);
                case "musicVolume" -> musicVolume = (int)(percent * 100);
            }
        }
        
        // Handle scrollbar dragging for each column
        if (draggingLeftScrollbar) {
            int deltaY = my - scrollbarDragStartY;
            int scrollbarHeight = HEIGHT - 80;
            int contentHeight = maxLeftScroll + scrollbarHeight;
            
            if (contentHeight > scrollbarHeight) {
                float scrollPerPixel = (float)maxLeftScroll / (scrollbarHeight - 30);
                leftScrollOffset = Math.max(0, Math.min(maxLeftScroll, 
                    (int)(scrollbarDragStartOffset + deltaY * scrollPerPixel)));
            }
        }
        
        if (draggingMiddleScrollbar) {
            int deltaY = my - scrollbarDragStartY;
            int scrollbarHeight = HEIGHT - 40;
            int contentHeight = maxMiddleScroll + scrollbarHeight;
            
            if (contentHeight > scrollbarHeight) {
                float scrollPerPixel = (float)maxMiddleScroll / (scrollbarHeight - 30);
                middleScrollOffset = Math.max(0, Math.min(maxMiddleScroll,
                    (int)(scrollbarDragStartOffset + deltaY * scrollPerPixel)));
            }
        }
        
        if (draggingRightScrollbar) {
            int deltaY = my - scrollbarDragStartY;
            int scrollbarHeight = HEIGHT - 40;
            int contentHeight = maxRightScroll + scrollbarHeight;
            
            if (contentHeight > scrollbarHeight) {
                float scrollPerPixel = (float)maxRightScroll / (scrollbarHeight - 30);
                rightScrollOffset = Math.max(0, Math.min(maxRightScroll,
                    (int)(scrollbarDragStartOffset + deltaY * scrollPerPixel)));
            }
        }
    }
    
    public void handleMousePress(int mx, int my, int panelX, int panelY, int panelWidth, int panelHeight) {
        // Check if clicking on a slider handle
        int leftColumnWidth = (int)(WIDTH * 0.25);
        int dividerX = leftColumnWidth + 20;
        int contentX = dividerX + 20;
        
        for (Map.Entry<String, Rectangle> entry : middleOptionBounds.entrySet()) {
            Rectangle bounds = entry.getValue();
            String option = entry.getKey();
            
            // Check if this is a slider option
            if (option.contains("Volume") || option.contains("Strength") || option.contains("sensitivity")) {
                int sliderX = contentX + 550;
                int sliderWidth = 150;
                int sliderY = bounds.y + bounds.height / 2;
                
                // Check if clicking near the slider
                if (mx >= sliderX && mx <= sliderX + sliderWidth &&
                    my >= sliderY - 12 && my <= sliderY + 12) {
                    
                    if (option.equals("Vibration Strength")) activeSlider = "vibrationStrength";
                    else if (option.equals("Movement sensitivity")) activeSlider = "movementSensitivity";
                    else if (option.contains("Sound Effects")) activeSlider = "sfxVolume";
                    else if (option.contains("Background Music")) activeSlider = "musicVolume";
                    
                    return;
                }
            }
        }
        
        // Check scrollbar clicks
        int scrollbarX, scrollbarY, scrollbarHeight, thumbY, thumbHeight;
        
        // Left scrollbar
        if (maxLeftScroll > 0) {
            scrollbarX = leftColumnWidth - 16;
            scrollbarY = 60;
            scrollbarHeight = HEIGHT - 80;
            thumbHeight = Math.max(30, (int)(scrollbarHeight * (scrollbarHeight / (float)(scrollbarHeight + maxLeftScroll))));
            thumbY = scrollbarY + (int)((scrollbarHeight - thumbHeight) * (leftScrollOffset / (float)maxLeftScroll));
            
            if (mx >= scrollbarX && mx <= scrollbarX + 8 &&
                my >= thumbY && my <= thumbY + thumbHeight) {
                draggingLeftScrollbar = true;
                scrollbarDragStartY = my;
                scrollbarDragStartOffset = leftScrollOffset;
                return;
            }
        }
        
        // Middle scrollbar
        if (maxMiddleScroll > 0) {
            int contentWidth = WIDTH - leftColumnWidth - PADDING;
            int middleWidth = shouldShowRightPanel()
                    ? (contentWidth - 80) / 2 : contentWidth - 40;
            scrollbarX = contentX + middleWidth + 20;
            scrollbarY = 20;
            scrollbarHeight = HEIGHT - 40;
            thumbHeight = Math.max(30, (int)(scrollbarHeight * (scrollbarHeight / (float)(scrollbarHeight + maxMiddleScroll))));
            thumbY = scrollbarY + (int)((scrollbarHeight - thumbHeight) * (middleScrollOffset / (float)maxMiddleScroll));
            
            if (mx >= scrollbarX && mx <= scrollbarX + 8 &&
                my >= thumbY && my <= thumbY + thumbHeight) {
                draggingMiddleScrollbar = true;
                scrollbarDragStartY = my;
                scrollbarDragStartOffset = middleScrollOffset;
                return;
            }
        }
        
        // Right scrollbar
        if (maxRightScroll > 0 && shouldShowRightPanel()) {
            scrollbarX = WIDTH - 20;
            scrollbarY = 20;
            scrollbarHeight = HEIGHT - 40;
            thumbHeight = Math.max(30, (int)(scrollbarHeight * (scrollbarHeight / (float)(scrollbarHeight + maxRightScroll))));
            thumbY = scrollbarY + (int)((scrollbarHeight - thumbHeight) * (rightScrollOffset / (float)maxRightScroll));
            
            if (mx >= scrollbarX && mx <= scrollbarX + 8 &&
                my >= thumbY && my <= thumbY + thumbHeight) {
                draggingRightScrollbar = true;
                scrollbarDragStartY = my;
                scrollbarDragStartOffset = rightScrollOffset;
                return;
            }
        }
    }

    public void handleMouseRelease() {
        activeSlider = null;
        draggingLeftScrollbar = false;
        draggingMiddleScrollbar = false;
        draggingRightScrollbar = false;
    }

    public void handleKeyPress(int keyCode) {
        if (selectedCategory.equals("Key bindings") && waitingForKey) {
            if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER) {
                waitingForKey = false;
                configuringAction = null;
                keyBindingError = null;
                return;
            }
            
            if (keyCode == KeyEvent.VK_C || keyCode == KeyEvent.VK_T) {
                keyBindingError = keyCode == KeyEvent.VK_C
                        ? "C is reserved for Construct." : "T is reserved for Developer terminal.";
            } else if (keyCode == controlConfig.getKey(configuringAction)
                    || controlConfig.setControl(configuringAction, keyCode)) {
                waitingForKey = false;
                configuringAction = null;
                keyBindingError = null;
            } else {
                keyBindingError = "That key is already in use.";
            }
        }
    }
    
    public void draw(Graphics2D g2, ControlConfig controlConfig) {
        // Calculate responsive dimensions
        int panelX = 0;
        int panelY = 0;
        
        int leftColumnWidth = (int)(WIDTH * 0.25);
        int contentWidth = WIDTH - leftColumnWidth - PADDING;
        
        // Draw main panel
        g2.setColor(new Color(15, 10, 35));
        g2.fillRect(panelX, panelY, WIDTH, HEIGHT);
        
        // Draw vertical divider lines
        int dividerX1 = panelX + leftColumnWidth + 20;
        int middleWidth = shouldShowRightPanel()
            ? (contentWidth - 80) / 2 : contentWidth - 40;
        int dividerX2 = dividerX1 + 20 + middleWidth + 48;
        
        g2.setColor(new Color(0, 255, 200));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(dividerX1, panelY + 20, dividerX1, panelY + HEIGHT - 20);
        
        // Only draw second divider if right panel should be shown
        if (shouldShowRightPanel()) {
            g2.drawLine(dividerX2, panelY + 20, dividerX2, panelY + HEIGHT - 20);
        }
        g2.setStroke(new BasicStroke(1));
        
        // Draw three columns
        drawLeftColumn(g2, panelX, panelY, leftColumnWidth, HEIGHT);
        drawMiddleColumn(g2, dividerX1 + 20, panelY, middleWidth, HEIGHT, controlConfig);
        if (shouldShowRightPanel()) {
            drawRightColumn(g2, dividerX2 + 20, panelY, WIDTH - dividerX2 - 60, HEIGHT);
        }
        
        // Draw close button
        drawCloseButton(g2, panelX, panelY);
    }

    private boolean shouldShowRightPanel() {
        // Show right panel for Graphics options
        if (selectedCategory.equals("Graphics") && selectedMiddleOption != null &&
            (selectedMiddleOption.equals("Fullscreen resolution") || 
            selectedMiddleOption.equals("Color blind mode"))) {
            return true;
        }
        return false;
    }

    private void drawLeftColumn(Graphics2D g2, int panelX, int panelY, int columnWidth, int panelHeight) {
        Shape oldClip = g2.getClip();
        g2.setClip(panelX, panelY + 60, columnWidth, panelHeight - 80);
        g2.translate(0, -leftScrollOffset);
        
        int y = panelY + 60;
        int itemHeight = 50;
        int categoryX = panelX + 20;
        
        for (String category : categoryBounds.keySet()) {
            boolean isSelected = category.equals(selectedCategory);
            boolean isHovered = category.equals(hoveredCategory);
            
            Rectangle bounds = new Rectangle(categoryX, y, columnWidth - 56, itemHeight - 5);
            categoryBounds.put(category, bounds);
            
            // Draw background for selected/hovered
            if (isSelected) {
                g2.setColor(new Color(0, 200, 180, 200));
                g2.fillRoundRect(categoryX, y, columnWidth - 56, itemHeight - 5, 8, 8);
            } else if (isHovered) {
                g2.setColor(new Color(0, 150, 150, 100));
                g2.fillRoundRect(categoryX, y, columnWidth - 56, itemHeight - 5, 8, 8);
            }
            
            // Draw text
            Font font = new Font("Arial", isSelected ? Font.BOLD : Font.PLAIN, 16);
            g2.setFont(font);
            g2.setColor(isSelected ? new Color(10, 10, 40) : Color.WHITE);
            g2.drawString(category, categoryX + 15, y + 28);
            
            y += itemHeight;
        }
        
        maxLeftScroll = Math.max(0, y - (panelY + panelHeight - 80));
        
        g2.translate(0, leftScrollOffset);
        g2.setClip(oldClip);
        
        // Draw scrollbar if needed
        if (maxLeftScroll > 0) {
            drawScrollbar(g2, panelX + columnWidth - 16, panelY + 60, panelHeight - 80, 
                        leftScrollOffset, maxLeftScroll, draggingLeftScrollbar);
        }
    }

    private void drawMiddleColumn(Graphics2D g2, int x, int panelY, int width, int panelHeight, 
                                ControlConfig controlConfig) {
        Shape oldClip = g2.getClip();
        g2.setClip(x, panelY + 20, width, panelHeight - 40);
        g2.translate(0, -middleScrollOffset);
        
        middleOptionBounds.clear();
        int y = panelY + 60;
        
        switch (selectedCategory) {
            case "Developer" -> y = drawDeveloperMiddle(g2, x, y, width);
            case "Key bindings" -> y = drawKeyBindingsMiddle(g2, x, y, width, controlConfig);
            case "Controller" -> y = drawControllerMiddle(g2, x, y, width);
            case "Graphics" -> y = drawGraphicsMiddle(g2, x, y, width);
            case "Audio" -> y = drawAudioMiddle(g2, x, y, width);
            case "Language" -> y = drawLanguageMiddle(g2, x, y, width);
            case "Credits" -> y = drawCreditsMiddle(g2, x, y, width);
        }
        
        maxMiddleScroll = Math.max(0, y - (panelY + panelHeight - 40));
        
        g2.translate(0, middleScrollOffset);
        g2.setClip(oldClip);
        
        // Draw scrollbar if needed
        if (maxMiddleScroll > 0) {
            drawScrollbar(g2, x + width + 20, panelY + 20, panelHeight - 40, 
                        middleScrollOffset, maxMiddleScroll, draggingMiddleScrollbar);
        }
    }

    private void drawRightColumn(Graphics2D g2, int x, int panelY, int width, int panelHeight) {
        if (!shouldShowRightPanel()) return;
        
        Shape oldClip = g2.getClip();
        g2.setClip(x, panelY + 20, width, panelHeight - 40);
        g2.translate(0, -rightScrollOffset);
        
        rightOptionBounds.clear();
        int y = panelY + 60;
        
        if (selectedCategory.equals("Graphics") && selectedMiddleOption != null) {
            if (selectedMiddleOption.equals("Fullscreen resolution")) {
                y = drawResolutionOptions(g2, x, y, width);
            } else if (selectedMiddleOption.equals("Color blind mode")) {
                y = drawColorBlindOptions(g2, x, y, width);
            }
        }
        
        maxRightScroll = Math.max(0, y - (panelY + panelHeight - 100));
        
        g2.translate(0, rightScrollOffset);
        g2.setClip(oldClip);
        
        // Draw scrollbar if needed
        if (maxRightScroll > 0) {
            drawScrollbar(g2, x + width + 20, panelY + 20, panelHeight - 40, 
                        rightScrollOffset, maxRightScroll, draggingRightScrollbar);
        }
    }

    private int drawKeyBindingsMiddle(Graphics2D g2, int x, int y, int width, 
                                    ControlConfig controlConfig) {
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(255, 200, 100));
        g2.drawString("KEY BINDINGS:", x, y);
        y += 40;
        
        String[] actions = {
            "ability1", "ability2", "ability3", "ability4", "void",
            "shoot", "thrust", "left", "right", "hyper",
            "constructTeleport", "constructDrone", "constructNova", "constructBlackHole"
        };
        String[] labels = {
            "Ability slot 1", "Ability slot 2", "Ability slot 3", "Ability slot 4", "Void ability",
            "Shoot", "Thrust", "Rotate Left", "Rotate Right", "Hyper",
            "Construct Teleport", "Construct Drone", "Construct Nova", "Construct Black Hole"
        };
        
        for (int i = 0; i < actions.length; i++) {
            int itemY = y + i * 40;
            Rectangle bounds = new Rectangle(x, itemY - 16, width, 35);
            keyConfigBounds.put(actions[i], bounds);
            middleOptionBounds.put(actions[i], bounds);
            
            String action = actions[i];
            boolean isConfiguring = waitingForKey && action.equals(configuringAction);
            boolean isHovered = (i == hoveredKeyConfig);
            
            g2.setColor(isConfiguring ? new Color(255, 215, 0, 100) : 
                    isHovered ? new Color(80, 100, 130, 200) : new Color(50, 60, 80, 150));
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            
            g2.setColor(isConfiguring ? new Color(255, 215, 0) : new Color(80, 120, 180));
            g2.setStroke(new BasicStroke(isConfiguring ? 2 : 1));
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            g2.setStroke(new BasicStroke(1));
            
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(200, 200, 220));
            g2.drawString(labels[i] + ":", bounds.x + 10, bounds.y + 22);
            
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            String keyName = isConfiguring ? "Press key..." : controlConfig.getKeyName(controlConfig.getKey(action));
            g2.setColor(isConfiguring ? new Color(255, 215, 0) : new Color(100, 255, 150));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(keyName, bounds.x + bounds.width - fm.stringWidth(keyName) - 10, bounds.y + 22);
        }

        y += actions.length * 40 + 18;
        if (keyBindingError != null) {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.setColor(new Color(255, 145, 155));
            g2.drawString(keyBindingError, x, y);
        }
        y += 24;
        return y;
    }

    private int drawDeveloperMiddle(Graphics2D g2, int x, int y, int width) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.setColor(new Color(100, 220, 235));
        g2.drawString("DEVELOPER", x, y);
        y += 32;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(new Color(220, 225, 240));
        g2.drawString("Enable in-game testing commands.", x, y);
        y += 30;
        Rectangle toggle = new Rectangle(x, y, width, 42);
        middleOptionBounds.put("Enable terminal", toggle);
        g2.setColor(new Color(45, 60, 80, 170));
        g2.fillRoundRect(x, y, width, 42, 8, 8);
        g2.setColor(new Color(95, 180, 195));
        g2.drawRoundRect(x, y, width, 42, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("Terminal during game", x + 12, y + 26);
        String state = controlConfig.isDeveloperTerminalEnabled() ? "ON" : "OFF";
        g2.setColor(controlConfig.isDeveloperTerminalEnabled()
                ? new Color(135, 235, 170) : new Color(175, 185, 200));
        g2.drawString(state, x + width - 45, y + 26);
        y += 68;
        g2.setColor(new Color(195, 205, 225));
        g2.drawString("Press T to open; Esc to close.", x, y);
        y += 25;
        g2.drawString("Type help for commands and target rules.", x, y);
        return y + 20;
    }

    private int drawControllerMiddle(Graphics2D g2, int x, int y, int width) {
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        
        // Invert X
        Rectangle bounds = new Rectangle(x, y - 16, width, 40);
        middleOptionBounds.put("Invert X", bounds);
        drawToggle(g2, "Invert X", invertX, x, y);
        y += 60;
        
        // Vibration
        bounds = new Rectangle(x, y - 16, width, 40);
        middleOptionBounds.put("Vibration", bounds);
        drawToggle(g2, "Vibration", vibration, x, y);
        y += 60;
        
        // Vibration Strength
        if (vibration) {
            bounds = new Rectangle(x + 550, y - 16, 150, 24);
            middleOptionBounds.put("Vibration Strength", bounds);
            drawSlider(g2, "Vibration Strength", vibrationStrength, x, y, "vibrationStrength");
            y += 60;
        }
        
        // Movement sensitivity
        bounds = new Rectangle(x + 550, y - 16, 150, 24);
        middleOptionBounds.put("Movement sensitivity", bounds);
        drawSlider(g2, "Movement sensitivity", movementSensitivity, x, y, "movementSensitivity");
        y += 60;
        
        // Restore to default button
        bounds = new Rectangle(x, y, 200, 35);
        middleOptionBounds.put("Restore to default", bounds);
        drawButton(g2, "Restore to default", x, y, 200);
        y += 80;
        
        return y;
    }

    private int drawGraphicsMiddle(Graphics2D g2, int x, int y, int width) {
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        
        // Fullscreen resolution - clickable to show options
        Rectangle bounds = new Rectangle(x, y - 16, width, 40);
        middleOptionBounds.put("Fullscreen resolution", bounds);
        
        boolean isSelected = "Fullscreen resolution".equals(selectedMiddleOption);
        if (isSelected || "Fullscreen resolution".equals(hoveredMiddleOption)) {
            g2.setColor(new Color(0, 255, 255, isSelected ? 60 : 30));
            g2.fillRect(x - 5, y - 20, width, 40);
        }
        
        g2.setColor(new Color(0, 255, 200));
        g2.drawString("Fullscreen resolution", x, y);
        g2.setColor(Color.WHITE);
        g2.drawString("1920x1080", x + 500, y);
        y += 60;
        
        // Effects
        bounds = new Rectangle(x, y - 16, width, 40);
        middleOptionBounds.put("Effects", bounds);
        drawToggle(g2, "Effects", effects, x, y);
        y += 60;
        
        // V-Sync
        bounds = new Rectangle(x, y - 16, width, 40);
        middleOptionBounds.put("V-Sync", bounds);
        drawToggle(g2, "V-Sync", vSync, x, y);
        y += 60;
        
        // FPS (read-only)
        g2.setColor(new Color(0, 255, 200));
        g2.drawString("FPS", x, y);
        g2.setColor(new Color(0, 200, 255));
        g2.drawString(String.valueOf(fps), x + 500, y);
        y += 60;
        
        // Screen shake
        bounds = new Rectangle(x, y - 16, width, 40);
        middleOptionBounds.put("Screen shake", bounds);
        drawToggle(g2, "Screen shake", screenShake, x, y);
        y += 60;
        
        // Windowed mode
        bounds = new Rectangle(x, y - 16, width, 40);
        middleOptionBounds.put("Windowed mode", bounds);
        drawToggle(g2, "Windowed mode", windowedMode, x, y);
        y += 60;
        
        // Color blind mode - clickable to show options
        bounds = new Rectangle(x, y - 16, width, 40);
        middleOptionBounds.put("Color blind mode", bounds);
        
        isSelected = "Color blind mode".equals(selectedMiddleOption);
        if (isSelected || "Color blind mode".equals(hoveredMiddleOption)) {
            g2.setColor(new Color(0, 255, 255, isSelected ? 60 : 30));
            g2.fillRect(x - 5, y - 20, width, 40);
        }
        
        g2.setColor(new Color(0, 255, 200));
        g2.drawString("Color blind mode", x, y);
        g2.setColor(new Color(0, 200, 255));
        g2.drawString(colorBlindMode, x + 500, y);
        y += 60;
        
        // Restore to default
        bounds = new Rectangle(x, y, 200, 35);
        middleOptionBounds.put("Restore to default", bounds);
        drawButton(g2, "Restore to default", x, y, 200);
        y += 80;
        
        return y;
    }

    private int drawAudioMiddle(Graphics2D g2, int x, int y, int width) {
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        
        // SFX Volume
        Rectangle bounds = new Rectangle(x + 550, y - 16, 150, 24);
        middleOptionBounds.put("Sound Effects Volume", bounds);
        drawSlider(g2, "Sound Effects Volume", sfxVolume, x, y, "sfxVolume");
        y += 60;
        
        // Music Volume
        bounds = new Rectangle(x + 550, y - 16, 150, 24);
        middleOptionBounds.put("Background Music Volume", bounds);
        drawSlider(g2, "Background Music Volume", musicVolume, x, y, "musicVolume");
        y += 60;
        
        // Restore to default
        bounds = new Rectangle(x, y, 200, 35);
        middleOptionBounds.put("Restore to default", bounds);
        drawButton(g2, "Restore to default", x, y, 200);
        y += 80;
        
        return y;
    }

    private int drawLanguageMiddle(Graphics2D g2, int x, int y, int width) {
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(new Color(0, 255, 200));
        g2.drawString("Language", x, y);
        g2.setColor(Color.WHITE);
        g2.drawString(language, x + 400, y);
        y += 80;
        
        return y;
    }

    private int drawCreditsMiddle(Graphics2D g2, int x, int y, int width) {
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(new Color(255, 200, 100));
        g2.drawString("ASTEROIDS: Navigate the Void", x, y);
        y += 60;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("Version 0", x, y);
        y += 30;
        g2.drawString("Created with Java/Swing", x, y);
        y += 80;
        
        return y;
    }

    private int drawResolutionOptions(Graphics2D g2, int x, int y, int width) {
        String[] resolutions = {
            "1360x768",
            "1366x768", 
            "1440x900",
            "1600x900",
            "1680x1050",
            "1920x1080",
            "1920x1200"
        };
        
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        
        for (String resolution : resolutions) {
            Rectangle bounds = new Rectangle(x, y - 20, width - 40, 35);
            rightOptionBounds.put(resolution, bounds);
            
            boolean isHovered = resolution.equals(hoveredRightOption);
            boolean isSelected = resolution.equals("1920x1080"); // Current resolution
            
            // Background
            if (isSelected) {
                g2.setColor(new Color(0, 255, 200, 100));
                g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            } else if (isHovered) {
                g2.setColor(new Color(0, 255, 255, 60));
                g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            
            // Border
            g2.setColor(isSelected ? new Color(0, 255, 200) : new Color(0, 180, 180));
            g2.setStroke(new BasicStroke(isSelected ? 2 : 1));
            g2.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g2.setStroke(new BasicStroke(1));
            
            // Text
            g2.setColor(isSelected ? Color.WHITE : new Color(200, 220, 255));
            g2.drawString(resolution, bounds.x + 10, bounds.y + 22);
            
            y += 45;
        }
        
        return y;
    }

    private int drawColorBlindOptions(Graphics2D g2, int x, int y, int width) {
        String[] modes = {
            "NORMAL",
            "PROTANOPIA",
            "DEUTERANOPIA",
            "TRITANOPIA"
        };
        
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        
        for (String mode : modes) {
            Rectangle bounds = new Rectangle(x, y - 20, width - 40, 35);
            rightOptionBounds.put(mode, bounds);
            
            boolean isHovered = mode.equals(hoveredRightOption);
            boolean isSelected = mode.equals(colorBlindMode);
            
            // Background
            if (isSelected) {
                g2.setColor(new Color(0, 255, 200, 100));
                g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            } else if (isHovered) {
                g2.setColor(new Color(0, 255, 255, 60));
                g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            
            // Border
            g2.setColor(isSelected ? new Color(0, 255, 200) : new Color(0, 180, 180));
            g2.setStroke(new BasicStroke(isSelected ? 2 : 1));
            g2.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g2.setStroke(new BasicStroke(1));
            
            // Text
            g2.setColor(isSelected ? Color.WHITE : new Color(200, 220, 255));
            g2.drawString(mode, bounds.x + 10, bounds.y + 22);
            
            y += 45;
        }
        
        return y;
    }
    
    private void drawToggle(Graphics2D g2, String label, boolean value, int x, int y) {
        g2.setColor(new Color(0, 255, 200));
        g2.drawString(label, x, y);
        
        // Toggle switch
        int toggleX = x + 550;
        int toggleWidth = 50;
        int toggleHeight = 24;
        
        g2.setColor(value ? new Color(0, 255, 200) : new Color(100, 100, 100));
        g2.fillRoundRect(toggleX, y - 16, toggleWidth, toggleHeight, 12, 12);
        
        // Toggle circle
        int circleX = value ? toggleX + toggleWidth - 20 : toggleX + 4;
        g2.setColor(Color.WHITE);
        g2.fillOval(circleX, y - 12, 16, 16);
        
        // Status text
        g2.setColor(value ? new Color(0, 255, 200) : new Color(150, 150, 150));
        g2.drawString(value ? "ON" : "OFF", toggleX + toggleWidth + 20, y);
    }
    
    private void drawSlider(Graphics2D g2, String label, int value, int x, int y, String sliderName) {
        g2.setColor(new Color(0, 255, 200));
        g2.drawString(label, x, y);
        
        // Slider track
        int sliderX = x + 550;
        int sliderWidth = 150;
        g2.setColor(new Color(60, 80, 100));
        g2.fillRoundRect(sliderX, y - 8, sliderWidth, 8, 4, 4);
        
        // Slider fill
        int fillWidth = (int)(sliderWidth * (value / 100.0));
        g2.setColor(new Color(0, 255, 200));
        g2.fillRoundRect(sliderX, y - 8, fillWidth, 8, 4, 4);
        
        // Slider handle
        g2.setColor(Color.WHITE);
        g2.fillOval(sliderX + fillWidth - 8, y - 12, 16, 16);
        
        // Value text
        g2.setColor(new Color(0, 200, 255));
        g2.drawString("[" + value + "]", sliderX + sliderWidth + 20, y);
    }
    
    private void drawButton(Graphics2D g2, String label, int x, int y, int width) {
        g2.setColor(new Color(40, 60, 90, 200));
        g2.fillRoundRect(x, y, width, 35, 8, 8);
        g2.setColor(new Color(0, 200, 180));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, width, 35, 8, 8);
        g2.setStroke(new BasicStroke(1));
        
        g2.setColor(new Color(0, 255, 200));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, x + width/2 - fm.stringWidth(label)/2, y + 22);
    }
    
    private void drawScrollbar(Graphics2D g2, int x, int y, int height, int scrollOffset, 
                        int maxScroll, boolean isDragging) {
        int scrollbarWidth = 8;
        int adjustedX = x;
        
        // Track
        g2.setColor(new Color(40, 60, 80, 150));
        g2.fillRoundRect(adjustedX, y, scrollbarWidth, height, 4, 4);
        
        // Thumb
        if (maxScroll > 0) {
            int thumbHeight = Math.max(30, (int)(height * (height / (float)(height + maxScroll))));
            int thumbY = y + (int)((height - thumbHeight) * (scrollOffset / (float)maxScroll));
            
            g2.setColor(isDragging ? new Color(0, 255, 200, 255) : new Color(0, 255, 200, 200));
            g2.fillRoundRect(adjustedX, thumbY, scrollbarWidth, thumbHeight, 4, 4);
        }
    }
    
    private void drawCloseButton(Graphics2D g2, int panelX, int panelY) {
        int size = 36;
        int x = panelX + WIDTH - size - 62;
        int y = panelY + 20;
        closeButtonBounds = new Rectangle(x, y, size, size);

        g2.setColor(closeButtonHovered ? new Color(190, 65, 90, 170)
                                       : new Color(35, 45, 65, 145));
        g2.fillRoundRect(x, y, size, size, 8, 8);
        g2.setColor(closeButtonHovered ? Color.WHITE : new Color(190, 205, 225));
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawRoundRect(x, y, size, size, 8, 8);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        g2.drawLine(centerX - 6, centerY - 6, centerX + 6, centerY + 6);
        g2.drawLine(centerX + 6, centerY - 6, centerX - 6, centerY + 6);
        g2.setStroke(new BasicStroke(1));
    }

    // Getters
    public int getSFXVolume() { return sfxVolume; }
    public int getMusicVolume() { return musicVolume; }
    public boolean isEffectsEnabled() { return effects; }
    public boolean isScreenShakeEnabled() { return screenShake; }
}
