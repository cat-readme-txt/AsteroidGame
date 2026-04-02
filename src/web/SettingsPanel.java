import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

public class SettingsPanel {
    // General
    private int WIDTH;
    private int HEIGHT;
    private final int PADDING = 60;
    // Panel state
    private String selectedCategory = "Instructions";
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
    private final AbilityLoadout abilityLoadout;
    private String configuringAction; 
    private boolean waitingForKey;
    private int selectedSlot = -1; 
    private int hoveredSlot = -1; 
    private int hoveredAbility = -1; 
    private int hoveredKeyConfig = -1; 
    private int instructionPage = 0;
    private final int totalInstructionPages = 5;
    private Rectangle prevPageButton;
    private Rectangle nextPageButton;
    private Map<String, Rectangle> abilitySlotBounds = new LinkedHashMap<>();
    private Map<String, Rectangle> availableAbilityBounds = new LinkedHashMap<>();
    private Map<String, Rectangle> keyConfigBounds = new LinkedHashMap<>();
    
    public SettingsPanel(int WIDTH, int HEIGHT, ControlConfig controlConfig, AbilityLoadout abilityLoadout) {
        this.WIDTH = WIDTH;
        this.HEIGHT = HEIGHT;
        this.controlConfig = controlConfig;
        this.abilityLoadout = abilityLoadout;
        categoryBounds.put("Instructions", new Rectangle());
        categoryBounds.put("Key bindings", new Rectangle());
        categoryBounds.put("Abilities", new Rectangle());
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
            if (entry.getValue().contains(mx, my)) {
                selectedCategory = entry.getKey();
                leftScrollOffset = 0;
                middleScrollOffset = 0;
                rightScrollOffset = 0;
                selectedMiddleOption = null;
                return false;
            }
        }
        
        // Check middle section clicks
        for (Map.Entry<String, Rectangle> entry : middleOptionBounds.entrySet()) {
            if (entry.getValue().contains(mx, my)) {
                handleMiddleClick(entry.getKey(), mx, my);
                return false;
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
            case "Key bindings" -> {
                for (Map.Entry<String, Rectangle> entry : keyConfigBounds.entrySet()) {
                    if (entry.getValue().contains(mx, my)) {
                        configuringAction = entry.getKey();
                        waitingForKey = true;
                        return;
                    }
                }
            }
            case "Abilities" -> {
                // Handle ability slot selection
                for (Map.Entry<String, Rectangle> entry : abilitySlotBounds.entrySet()) {
                    if (entry.getValue().contains(mx, my)) {
                        int slotNum = Integer.parseInt(entry.getKey().replace("slot", ""));
                        selectedSlot = slotNum;
                        rightScrollOffset = 0;
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
        } else if (selectedCategory.equals("Abilities") && selectedSlot != -1) {
                // Assign ability to selected slot
                abilityLoadout.setSlot(selectedSlot, option);
                selectedSlot = -1; // Deselect after assignment
        }
    }
    
    public void handleMouseMove(int mx, int my) {
        hoveredCategory = null;
        hoveredMiddleOption = null;
        hoveredRightOption = null;
        hoveredSlot = -1;
        hoveredAbility = -1;
        hoveredKeyConfig = -1;
        closeButtonHovered = false;
        
        // Check close button hover
        if (closeButtonBounds != null && closeButtonBounds.contains(mx, my)) {
            closeButtonHovered = true;
            return;
        }
        
        // Check category hovers
        for (Map.Entry<String, Rectangle> entry : categoryBounds.entrySet()) {
            if (entry.getValue().contains(mx, my)) {
                hoveredCategory = entry.getKey();
                return;
            }
        }
        
        // Check middle section hovers
        if (selectedCategory.equals("Key bindings")) {
            int index = 0;
            for (Map.Entry<String, Rectangle> entry : keyConfigBounds.entrySet()) {
                if (entry.getValue().contains(mx, my)) {
                    hoveredKeyConfig = index;
                    hoveredMiddleOption = entry.getKey();
                    return;
                }
                index++;
            }
        } else if (selectedCategory.equals("Abilities")) {
            for (Map.Entry<String, Rectangle> entry : abilitySlotBounds.entrySet()) {
                if (entry.getValue().contains(mx, my)) {
                    hoveredSlot = Integer.parseInt(entry.getKey().replace("slot", ""));
                    hoveredMiddleOption = entry.getKey();
                    return;
                }
            }
        } else {
            for (Map.Entry<String, Rectangle> entry : middleOptionBounds.entrySet()) {
                if (entry.getValue().contains(mx, my)) {
                    hoveredMiddleOption = entry.getKey();
                    return;
                }
            }
        }
        
        // Check right section hovers
        if (selectedCategory.equals("Abilities") && selectedSlot != -1) {
            int index = 0;
            for (Map.Entry<String, Rectangle> entry : availableAbilityBounds.entrySet()) {
                if (entry.getValue().contains(mx, my)) {
                    hoveredAbility = index;
                    hoveredRightOption = entry.getKey();
                    return;
                }
                index++;
            }
        } else {
            for (Map.Entry<String, Rectangle> entry : rightOptionBounds.entrySet()) {
                if (entry.getValue().contains(mx, my)) {
                    hoveredRightOption = entry.getKey();
                    return;
                }
            }
        }
    }
    
    public void handleScroll(int rotation, int mx, int panelX, int panelY, int panelWidth, int panelHeight) {
        int leftColumnWidth = (int)(panelWidth * 0.25);
        int dividerX = panelX + leftColumnWidth + 20;
        int contentX = dividerX + 20;
        int contentWidth = panelWidth - leftColumnWidth - PADDING;
        int middleWidth = (int)(contentWidth * 0.5);
        int rightX = contentX + middleWidth + 20;
        
        // Determine which column to scroll based on mouse position
        if (mx < dividerX) {
            // Left column
            leftScrollOffset -= rotation * 20;
            leftScrollOffset = Math.max(0, Math.min(leftScrollOffset, maxLeftScroll));
        } else if (mx < rightX) {
            // Middle column
            middleScrollOffset -= rotation * 20;
            middleScrollOffset = Math.max(0, Math.min(middleScrollOffset, maxMiddleScroll));
        } else {
            // Right column
            rightScrollOffset -= rotation * 20;
            rightScrollOffset = Math.max(0, Math.min(rightScrollOffset, maxRightScroll));
        }
    }
    
    public void handleMouseDrag(int mx, int my, int panelX, int panelY, int panelWidth, int panelHeight) {
        if (activeSlider != null) {
            int leftColumnWidth = (int)(panelWidth * 0.25);
            int dividerX = panelX + leftColumnWidth + 20;
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
            int scrollbarHeight = panelHeight - 80;
            int contentHeight = maxLeftScroll + scrollbarHeight;
            
            if (contentHeight > scrollbarHeight) {
                float scrollPerPixel = (float)maxLeftScroll / (scrollbarHeight - 30);
                leftScrollOffset = Math.max(0, Math.min(maxLeftScroll, 
                    (int)(scrollbarDragStartOffset + deltaY * scrollPerPixel)));
            }
        }
        
        if (draggingMiddleScrollbar) {
            int deltaY = my - scrollbarDragStartY;
            int scrollbarHeight = panelHeight - 40;
            int contentHeight = maxMiddleScroll + scrollbarHeight;
            
            if (contentHeight > scrollbarHeight) {
                float scrollPerPixel = (float)maxMiddleScroll / (scrollbarHeight - 30);
                middleScrollOffset = Math.max(0, Math.min(maxMiddleScroll,
                    (int)(scrollbarDragStartOffset + deltaY * scrollPerPixel)));
            }
        }
        
        if (draggingRightScrollbar) {
            int deltaY = my - scrollbarDragStartY;
            int scrollbarHeight = panelHeight - 40;
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
        int leftColumnWidth = (int)(panelWidth * 0.25);
        int dividerX = panelX + leftColumnWidth + 20;
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
            scrollbarX = panelX + leftColumnWidth - 12;
            scrollbarY = panelY + PADDING;
            scrollbarHeight = panelHeight - 80;
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
            int middleWidth = (int)((panelWidth - leftColumnWidth - PADDING) * 0.5);
            scrollbarX = contentX + middleWidth - 12;
            scrollbarY = panelY + 20;
            scrollbarHeight = panelHeight - 40;
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
            int contentWidth = panelWidth - leftColumnWidth - PADDING;
            int middleWidth = (int)(contentWidth * 0.5);
            scrollbarX = panelX + panelWidth - 34;
            scrollbarY = panelY + 20;
            scrollbarHeight = panelHeight - 40;
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
                return;
            }
            
            if (controlConfig.setControl(configuringAction, keyCode)) {
                waitingForKey = false;
                configuringAction = null;
            } else {
                // Key already in use
                waitingForKey = false;
                configuringAction = null;
            }
        }
    }
    
    public void draw(Graphics2D g2, ControlConfig controlConfig, AbilityLoadout loadout) {
        // Calculate responsive dimensions
        int panelX = 0;
        int panelY = 0;
        
        int leftColumnWidth = (int)(WIDTH * 0.25);
        int contentWidth = WIDTH - leftColumnWidth - PADDING;
        
        // Draw main panel
        g2.setColor(new Color(15, 10, 35, 200));
        g2.fillRect(panelX, panelY, WIDTH, HEIGHT);
        
        // Draw vertical divider lines
        int dividerX1 = panelX + leftColumnWidth + 20;
        int middleWidth = (int)(contentWidth * 0.5);
        int dividerX2 = dividerX1 + 20 + middleWidth + 20;
        
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
        drawMiddleColumn(g2, dividerX1 + 20, panelY, middleWidth, HEIGHT, controlConfig, loadout);
        if (shouldShowRightPanel()) {
            drawRightColumn(g2, dividerX2 + 20, panelY, contentWidth - middleWidth - 40, HEIGHT);
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
        // Show right panel for Abilities when a slot is selected
        return selectedCategory.equals("Abilities") && selectedSlot != -1;
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
            
            Rectangle bounds = new Rectangle(categoryX, y, columnWidth - 20, itemHeight - 5);
            categoryBounds.put(category, bounds);
            
            // Draw background for selected/hovered
            if (isSelected) {
                g2.setColor(new Color(0, 200, 180, 200));
                g2.fillRoundRect(categoryX, y, columnWidth - 20, itemHeight - 5, 8, 8);
            } else if (isHovered) {
                g2.setColor(new Color(0, 150, 150, 100));
                g2.fillRoundRect(categoryX, y, columnWidth - 20, itemHeight - 5, 8, 8);
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
            drawScrollbar(g2, panelX + columnWidth - 4, panelY + 60, panelHeight - 80, 
                        leftScrollOffset, maxLeftScroll, draggingLeftScrollbar);
        }
    }

    private void drawMiddleColumn(Graphics2D g2, int x, int panelY, int width, int panelHeight, 
                                ControlConfig controlConfig, AbilityLoadout loadout) {
        Shape oldClip = g2.getClip();
        g2.setClip(x, panelY + 20, width, panelHeight - 40);
        g2.translate(0, -middleScrollOffset);
        
        middleOptionBounds.clear();
        int y = panelY + 60;
        
        switch (selectedCategory) {
            case "Instructions" -> y = drawInstructionsMiddle(g2, x, y, width);
            case "Key bindings" -> y = drawKeyBindingsMiddle(g2, x, y, width, controlConfig, loadout);
            case "Abilities" -> y = drawAbilitiesMiddle(g2, x, y, width, loadout);
            case "Controller" -> y = drawControllerMiddle(g2, x, y, width);
            case "Graphics" -> y = drawGraphicsMiddle(g2, x, y, width);
            case "Audio" -> y = drawAudioMiddle(g2, x, y, width);
            case "Language" -> y = drawLanguageMiddle(g2, x, y, width);
            case "Credits" -> y = drawCreditsMiddle(g2, x, y, width);
        }
        
        maxMiddleScroll = Math.max(0, y - (panelY + panelHeight - 100));
        
        g2.translate(0, middleScrollOffset);
        g2.setClip(oldClip);
        
        // Draw scrollbar if needed
        if (maxMiddleScroll > 0) {
            drawScrollbar(g2, x + width + 10, panelY + 20, panelHeight - 40, 
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
        } else if (selectedCategory.equals("Abilities") && selectedSlot != -1) {
            y = drawAbilityOptions(g2, x, y, width);
        }
        
        maxRightScroll = Math.max(0, y - (panelY + panelHeight - 100));
        
        g2.translate(0, rightScrollOffset);
        g2.setClip(oldClip);
        
        // Draw scrollbar if needed
        if (maxRightScroll > 0) {
            drawScrollbar(g2, x + width - 4, panelY + 20, panelHeight - 40, 
                        rightScrollOffset, maxRightScroll, draggingRightScrollbar);
        }
    }

    private int drawInstructionsMiddle(Graphics2D g2, int x, int y, int width) {
        // Animated neon title
        long time = System.currentTimeMillis();
        float pulse = (float)(Math.sin(time / 400.0) * 0.2 + 0.8);
        
        // Title with neon glow effect
        g2.setFont(new Font("Consolas", Font.BOLD, 32));
        
        // Outer glow layers
        for (int i = 3; i > 0; i--) {
            g2.setColor(new Color(0, 255, 200, (int)(40 * pulse / i)));
            g2.drawString("INSTRUCTIONS", x - i, y - i);
            g2.drawString("INSTRUCTIONS", x + i, y + i);
        }
        
        // Main text - bright cyan
        g2.setColor(new Color(0, 255, 200, 255));
        g2.drawString("INSTRUCTIONS", x, y);
        
        // Inner highlight
        g2.setColor(new Color(255, 255, 255, (int)(150 * pulse)));
        g2.drawString("INSTRUCTIONS", x, y);
        
        y += 50;
        
        // Holographic separator with animated elements
        g2.setStroke(new BasicStroke(3));
        
        // Main separator line with gradient effect
        for (int i = 0; i < 5; i++) {
            int alpha = (int)(100 - i * 15);
            g2.setColor(new Color(0, 200, 255, alpha));
            g2.drawLine(x, y + i, x + width - 20, y + i);
        }
        
        // Animated dots along the line
        g2.setColor(new Color(0, 255, 200, (int)(200 * pulse)));
        int dotSpacing = 40;
        for (int i = 0; i < width / dotSpacing; i++) {
            int offset = (int)((time / 10) % dotSpacing);
            int dotX = x + (i * dotSpacing) + offset;
            if (dotX < x + width - 20) {
                g2.fillOval(dotX - 2, y - 2, 4, 4);
            }
        }
        
        // Corner accents
        g2.setColor(new Color(255, 0, 150, (int)(180 * pulse)));
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int accentSize = 12;
        g2.drawLine(x, y, x + accentSize, y);
        g2.drawLine(x, y, x, y + accentSize);
        g2.drawLine(x + width - 20, y, x + width - 20 - accentSize, y);
        g2.drawLine(x + width - 20, y, x + width - 20, y + accentSize);
        
        g2.setStroke(new BasicStroke(1));
        y += 30;
        
        // Draw all pages in sequence (no pagination)
        y = drawPage1BasicControls(g2, x, y, width);
        y += 30;
        y = drawPage2Combat(g2, x, y, width);
        y += 30;
        y = drawPage3VoidWorld(g2, x, y, width);
        y += 30;
        y = drawPage4Abilities(g2, x, y, width);
        y += 30;
        
        return y;
    }

    // ========================================
    // PAGE 1: BASIC CONTROLS
    // ========================================

    private int drawPage1BasicControls(Graphics2D g2, int x, int y, int width) {
        // Movement Controls Section
        y = drawSectionHeader(g2, "SHIP MOVEMENT", x, y, width);
        y += 5;
        
        y = drawControlItem(g2, "↑ UP ARROW", "Apply Forward Thrust", 
            "Accelerates your ship in the direction it's facing. Ship has momentum and inertia.", x, y, width);
        y = drawControlItem(g2, "← LEFT ARROW", "Rotate Counter-Clockwise", 
            "Rotates ship left. Does not change velocity, only direction.", x, y, width);
        y = drawControlItem(g2, "→ RIGHT ARROW", "Rotate Clockwise", 
            "Rotates ship right. Does not change velocity, only direction.", x, y, width);
        y = drawControlItem(g2, "SPACE", "Hyper Mode", 
            "Temporary speed boost. Uses fuel faster.", x, y, width);
        
        y += 10;
        y = drawInfoBox(g2, "PHYSICS", 
            "Your ship obeys Newton's laws! Momentum is preserved when you stop thrusting. Use rotation to redirect your velocity vector.", 
            x, y, width, new Color(100, 150, 200));
        y += 15;
        
        // Screen Wrapping
        y = drawSectionHeader(g2, "SCREEN BOUNDARIES", x, y, width);
        y += 5;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(220, 220, 240));
        String[] wrapping = {
            "• Ship wraps around screen edges (left ↔ right, top ↔ bottom)",
            "• Asteroids also wrap around - watch all sides!",
            "• Bullets and lasers wrap until they expire"
        };
        for (String line : wrapping) {
            g2.drawString(line, x + 10, y);
            y += 22;
        }
        
        y += 15;
        
        // Fuel System
        y = drawSectionHeader(g2, "FUEL SYSTEM", x, y, width);
        y += 5;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(220, 220, 240));
        String[] fuelInfo = {
            "• Fuel is consumed when thrusting or using hyper mode",
            "• Fuel regenerates by destroying asteroids",
            "• Larger asteroids give more fuel (Small: 5, Medium: 10, Large: 20)",
            "• Maximum fuel capacity: 1000 units",
            "• Running out of fuel only prevents thrust - you can still coast"
        };
        for (String line : fuelInfo) {
            g2.drawString(line, x + 10, y);
            y += 22;
        }
        
        y += 10;
        y = drawWarningBox(g2, "⚠ SURVIVAL TIP", 
            "Conserve fuel! Don't thrust constantly. Use momentum and coast when possible.", 
            x, y, width);
        
        y += 20;
        return y;
    }


    // ========================================
    // PAGE 2: COMBAT SYSTEM
    // ========================================

    private int drawPage2Combat(Graphics2D g2, int x, int y, int width) {
        // Basic Shooting
        y = drawSectionHeader(g2, "SHOOTING MECHANICS", x, y, width);
        y += 5;
        
        y = drawControlItem(g2, "A", "Fire Weapon", 
            "Fires bullets in normal mode, void lasers when you have void energy.", x, y, width);
        
        y += 10;
        
        // Bullet vs Laser comparison
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        g2.setColor(new Color(255, 200, 100));
        g2.drawString("WEAPON COMPARISON", x, y);
        y += 25;
        
        // Bullets
        g2.setColor(new Color(100, 200, 255, 80));
        g2.fillRoundRect(x, y - 15, width - 20, 90, 8, 8);
        g2.setColor(new Color(100, 200, 255));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y - 15, width - 20, 90, 8, 8);
        g2.setStroke(new BasicStroke(1));
        
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(255, 255, 255));
        g2.drawString("BULLETS (Default Weapon)", x + 10, y);
        y += 20;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(220, 220, 240));
        String[] bulletInfo = {
            "✓ Unlimited ammo - no cost to fire",
            "✓ Good damage against asteroids",
            "✓ Moderate travel speed and range",
            "✗ Cannot damage void creatures"
        };
        for (String line : bulletInfo) {
            g2.drawString(line, x + 20, y);
            y += 18;
        }
        y += 15;
        
        // Void Lasers
        g2.setColor(new Color(180, 0, 255, 80));
        g2.fillRoundRect(x, y - 15, width - 20, 110, 8, 8);
        g2.setColor(new Color(180, 0, 255));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y - 15, width - 20, 110, 8, 8);
        g2.setStroke(new BasicStroke(1));
        
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(200, 150, 255));
        g2.drawString("VOID LASERS (Requires Void Energy)", x + 10, y);
        y += 20;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(220, 220, 240));
        String[] laserInfo = {
            "✓ Extremely fast, near-instant travel",
            "✓ Can damage both asteroids AND void creatures",
            "✓ Higher damage than bullets",
            "✗ Consumes void energy (2 in void, 5 outside)",
            "✗ Only available when you have void energy"
        };
        for (String line : laserInfo) {
            g2.drawString(line, x + 20, y);
            y += 18;
        }
        y += 20;
        
        // Asteroid Destruction
        y = drawSectionHeader(g2, "ASTEROID MECHANICS", x, y, width);
        y += 5;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(220, 220, 240));
        String[] asteroidInfo = {
            "• Large asteroids split into 2 medium asteroids",
            "• Medium asteroids split into 2 small asteroids",
            "• Small asteroids are destroyed completely",
            "• Each size grants score, XP, and fuel when destroyed"
        };
        for (String line : asteroidInfo) {
            g2.drawString(line, x + 10, y);
            y += 22;
        }
        
        y += 10;
        
        // Rewards table
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("DESTRUCTION REWARDS:", x, y);
        y += 25;
        
        String[][] rewards = {
            {"Large Asteroid", "20 Score", "10 XP", "20 Fuel"},
            {"Medium Asteroid", "10 Score", "5 XP", "10 Fuel"},
            {"Small Asteroid", "5 Score", "3 XP", "5 Fuel"}
        };
        
        for (String[] reward : rewards) {
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(new Color(150, 150, 180));
            g2.drawString(reward[0], x + 20, y);
            
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(reward[1], x + 200, y);
            
            g2.setColor(new Color(100, 255, 150));
            g2.drawString(reward[2], x + 300, y);
            
            g2.setColor(new Color(255, 150, 50));
            g2.drawString(reward[3], x + 380, y);
            
            y += 22;
        }
        
        y += 20;
        return y;
    }


    // ========================================
    // PAGE 3: VOID WORLD
    // ========================================

    private int drawPage3VoidWorld(Graphics2D g2, int x, int y, int width) {
        y = drawSectionHeader(g2, "VOID ENERGY SYSTEM", x, y, width);
        y += 5;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(220, 220, 240));
        String[] voidIntro = {
            "Void energy is a powerful but dangerous resource that lets you:",
            "  • Enter an alternate dimension (Void World)",
            "  • Fire powerful void lasers",
            "  • Become temporarily invulnerable to normal threats"
        };
        for (String line : voidIntro) {
            g2.drawString(line, x + 10, y);
            y += 22;
        }
        
        y += 15;
        
        // How to gain/lose void energy
        y = drawSectionHeader(g2, "VOID ENERGY MECHANICS", x, y, width);
        y += 5;
        
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(new Color(100, 255, 150));
        g2.drawString("GAINING VOID ENERGY:", x + 10, y);
        y += 20;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(220, 220, 240));
        String[] gaining = {
            "✓ Automatically absorbs when in Void World at 2 energy/frame",
            "✓ Passive gain of 0.05 energy/frame when NOT in Void World"
        };
        for (String line : gaining) {
            g2.drawString(line, x + 20, y);
            y += 20;
        }
        
        y += 10;
        
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(new Color(255, 100, 100));
        g2.drawString("LOSING VOID ENERGY:", x + 10, y);
        y += 20;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(220, 220, 240));
        String[] losing = {
            "✗ Firing void lasers: 2 energy (in void), 5 energy (outside)",
            "✗ Using Fade ability: 0.05 energy/frame while active",
            "✗ Automatically dissipates at 0.5 energy/frame outside void"
        };
        for (String line : losing) {
            g2.drawString(line, x + 20, y);
            y += 20;
        }
        
        y += 15;
        
        // Void world dangers
        y = drawWarningBox(g2, "⚠ CRITICAL WARNING", 
            "Void energy has a THRESHOLD of 100. If exceeded, you DIE INSTANTLY! Monitor your void energy bar carefully.", 
            x, y, width);
        y += 10;
        
        y = drawSectionHeader(g2, "VOID WORLD MECHANICS", x, y, width);
        y += 5;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(220, 220, 240));
        String[] voidWorld = {
            "When you activate Void (default Q):",
            "",
            "BENEFITS:",
            "  • Asteroids become semi-transparent (30% opacity)",
            "  • You can pass through asteroids without collision",
            "  • You can pass through regular bullets without damage",
            "  • Void energy regenerates much faster (2/frame vs 0.05/frame)",
            "",
            "DANGERS:",
            "  • Void creatures spawn and can kill you",
            "  • Void hazards appear and are lethal on contact",
            "  • Screen overlay changes to purple hexagonal grid",
            "  • Energy builds up - must exit before hitting 100",
            "",
            "STRATEGY:",
            "  • Use to dodge asteroids in tight situations",
            "  • Exit immediately if energy approaches 100",
            "  • 3-second cooldown after exiting void"
        };
        for (String line : voidWorld) {
            if (line.isEmpty()) {
                y += 10;
            } else {
                g2.drawString(line, x + 10, y);
                y += 20;
            }
        }
        
        y += 15;
        
        // Void creatures
        y = drawSectionHeader(g2, "VOID THREATS", x, y, width);
        y += 5;
        
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(new Color(255, 100, 255));
        g2.drawString("VOID CREATURES (Only appear in Void World)", x + 10, y);
        y += 20;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(220, 220, 240));
        String[] creatures = {
            "• Specters: Small fast-moving entities, spawn in groups",
            "• Centipede: Large segmented creature, always present in void",
            "• Can only be damaged by void lasers or abilities",
            "• Contact with any void creature = instant death"
        };
        for (String line : creatures) {
            g2.drawString(line, x + 20, y);
            y += 20;
        }
        
        y += 10;
        
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(new Color(200, 100, 255));
        g2.drawString("VOID HAZARDS (Visible in both worlds)", x + 10, y);
        y += 20;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(220, 220, 240));
        String[] hazards = {
            "• Stationary purple energy fields that drift slowly",
            "• Lethal on contact when in Void World",
            "• Can be destroyed by Nova Blast ability",
            "• Respawn after being destroyed"
        };
        for (String line : hazards) {
            g2.drawString(line, x + 20, y);
            y += 20;
        }
        
        y += 20;
        return y;
    }


    // ========================================
    // PAGE 4: ABILITIES
    // ========================================

    private int drawPage4Abilities(Graphics2D g2, int x, int y, int width) {
        y = drawSectionHeader(g2, "ABILITY SYSTEM", x, y, width);
        y += 5;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(220, 220, 240));
        String[] intro = {
            "Abilities are unlocked by leveling up your ship. You can equip 4 abilities",
            "at a time in the Abilities settings page. Configure keybindings in settings."
        };
        for (String line : intro) {
            g2.drawString(line, x + 10, y);
            y += 20;
        }
        
        y += 15;
        
        // Void ability
        y = drawAbilityDetail(g2, "VOID", "Q", "3 seconds", new Color(180, 0, 255),
            new String[]{
                "Toggle between normal space and Void World",
                "In Void: pass through asteroids, gain energy faster (2/frame)",
                "Out of Void: 3-second cooldown before re-entering",
                "Asteroids become 30% transparent while in void",
                "⚠ Exit before energy reaches 100 or you die instantly"
            }, x, y, width);
        
        // Fade ability
        y = drawAbilityDetail(g2, "FADE", "W", "8 seconds", new Color(255, 215, 0),
            new String[]{
                "Become intangible for 5 seconds",
                "Pass through asteroids, bullets, void creatures, and entities",
                "Consumes void energy slowly (0.05/frame)",
                "Screen overlay: golden circuit pattern",
                "Complete invulnerability to ALL threats while active"
            }, x, y, width);
        
        // Teleport ability
        y = drawAbilityDetail(g2, "TELEPORT", "E", "5 seconds", new Color(0, 255, 200),
            new String[]{
                "Press once: Place anchor at current position",
                "Press again: Instantly teleport back to anchor",
                "Anchor remains until used or you die",
                "Creates white flash effect on teleport",
                "5-second cooldown after teleporting"
            }, x, y, width);
        
        // Shield Burst ability
        y = drawAbilityDetail(g2, "SHIELD", "R", "12 seconds", new Color(100, 200, 255),
            new String[]{
                "Expanding energy wave that destroys everything nearby",
                "Lasts 2 seconds, expands from 0 to 150 pixel radius",
                "Destroys asteroids, void creatures, and cosmic entities",
                "Awards full points and resources for destroyed objects",
                "12-second cooldown after activation"
            }, x, y, width);
        
        // Time Slow ability
        y = drawAbilityDetail(g2, "TIME SLOW", "T", "15 seconds", new Color(150, 255, 255),
            new String[]{
                "Slows down all asteroids to 30% of normal speed",
                "Lasts for 5 seconds",
                "Blue overlay with animated scan lines",
                "Does not affect ship movement or abilities",
                "15-second cooldown after duration ends"
            }, x, y, width);
        
        // Drone ability
        y = drawAbilityDetail(g2, "DRONE", "Y", "20 seconds", new Color(255, 150, 100),
            new String[]{
                "Deploy autonomous combat drone that orbits your ship",
                "Auto-targets and shoots nearest asteroid within 200 pixels",
                "Fires every 0.5 seconds with perfect accuracy",
                "Has 3 health points, destroyed after 3 hits",
                "Press again to recall drone early (starts cooldown)"
            }, x, y, width);
        
        // Nova Blast ability
        y = drawAbilityDetail(g2, "NOVA", "U", "Instant recharge", new Color(255, 100, 255),
            new String[]{
                "Massive explosion that clears the entire screen",
                "1-second charge time with pulsing visual effect",
                "Explosion expands to 1000 pixel radius over 1 second",
                "Destroys asteroids, bullets, hazards, creatures, entities, and black holes",
                "⚠ Consumes ALL void energy - use as last resort"
            }, x, y, width);
        
        y += 10;
        y = drawInfoBox(g2, "UNLOCK LEVELS", 
            "Void: Lv1 | Fade: Lv2 | Teleport: Lv3 | Shield: Lv4 | Time Slow: Lv5 | Drone: Lv6 | Nova: Lv7", 
            x, y, width, new Color(100, 255, 150));
        
        y += 20;
        
        return y;
    }

    // ========================================
    // HELPER METHODS FOR DRAWING
    // ========================================

    private int drawSectionHeader(Graphics2D g2, String title, int x, int y, int width) {
        long time = System.currentTimeMillis();
        float pulse = (float)(Math.sin(time / 500.0) * 0.15 + 0.85);
        
        // Hexagonal bullet point
        g2.setColor(new Color(0, 255, 200, (int)(200 * pulse)));
        int hexSize = 8;
        drawHexagon(g2, x - 15, y - 8, hexSize, true);
        
        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        
        // Text glow
        g2.setColor(new Color(0, 255, 200, 80));
        g2.drawString(title, x - 1, y);
        g2.drawString(title, x + 1, y);
        
        // Main text
        g2.setColor(new Color(0, 255, 200, 255));
        g2.drawString(title, x, y);
        
        // Underline with gradient
        FontMetrics fm = g2.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(0, 255, 200, (int)(150 * pulse)));
        g2.drawLine(x, y + 8, x + titleWidth, y + 8);
        
        // Secondary underline
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(255, 0, 150, 80));
        g2.drawLine(x, y + 10, x + titleWidth, y + 10);
        
        g2.setStroke(new BasicStroke(1));
        
        return y + 30;
    }

    private int drawControlItem(Graphics2D g2, String key, String action, String description, int x, int y, int width) {
        long time = System.currentTimeMillis();
        float pulse = (float)(Math.sin(time / 600.0) * 0.15 + 0.85);
        
        // Holographic key background with hexagonal design
        int keyWidth = 130;
        int keyHeight = 32;
        
        // Key background glow
        g2.setColor(new Color(0, 200, 255, 40));
        g2.fillRoundRect(x + 8, y - 20, keyWidth + 4, keyHeight + 4, 8, 8);
        
        // Main key background
        g2.setColor(new Color(20, 40, 60, 220));
        g2.fillRoundRect(x + 10, y - 18, keyWidth, keyHeight, 6, 6);
        
        // Neon border
        g2.setColor(new Color(0, 255, 200, (int)(200 * pulse)));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x + 10, y - 18, keyWidth, keyHeight, 6, 6);
        
        // Corner accents
        g2.setColor(new Color(255, 0, 150, (int)(180 * pulse)));
        int accentSize = 5;
        g2.drawLine(x + 10, y - 18, x + 10 + accentSize, y - 18);
        g2.drawLine(x + 10, y - 18, x + 10, y - 18 + accentSize);
        g2.drawLine(x + 10 + keyWidth, y - 18, x + 10 + keyWidth - accentSize, y - 18);
        g2.drawLine(x + 10 + keyWidth, y - 18, x + 10 + keyWidth, y - 18 + accentSize);
        
        g2.setStroke(new BasicStroke(1));
        
        // Key text with glow
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int keyTextX = x + 10 + (keyWidth - fm.stringWidth(key)) / 2;
        
        g2.setColor(new Color(0, 255, 200, 100));
        g2.drawString(key, keyTextX - 1, y);
        g2.drawString(key, keyTextX + 1, y);
        
        g2.setColor(new Color(255, 255, 255, 255));
        g2.drawString(key, keyTextX, y);
        
        // Action text
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        g2.setColor(new Color(255, 255, 255, 230));
        g2.drawString(action, x + 155, y);
        
        // Description with subtle glow
        g2.setFont(new Font("Consolas", Font.PLAIN, 11));
        g2.setColor(new Color(100, 200, 255, 180));
        g2.drawString(description, x + 155, y + 15);
        
        return y + 38;
    }

    private int drawInfoBox(Graphics2D g2, String title, String message, int x, int y, int width, Color themeColor) {
        long time = System.currentTimeMillis();
        float pulse = (float)(Math.sin(time / 700.0) * 0.2 + 0.8);
        
        int boxHeight = 70;
        
        // Holographic background with scanlines
        g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 25));
        g2.fillRoundRect(x, y, width - 20, boxHeight, 10, 10);
        
        // Scanline effect
        for (int i = 0; i < boxHeight; i += 3) {
            int alpha = (int)(15 + 10 * Math.sin(time / 200.0 + i / 5.0));
            g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha));
            g2.drawLine(x, y + i, x + width - 20, y + i);
        }
        
        // Outer glow
        g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(60 * pulse)));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x - 1, y - 1, width - 18, boxHeight + 2, 10, 10);
        
        // Main neon border
        g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(220 * pulse)));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, width - 20, boxHeight, 10, 10);
        
        // Corner accents
        g2.setColor(new Color(0, 255, 200, (int)(150 * pulse)));
        int accentSize = 8;
        g2.drawLine(x, y, x + accentSize, y);
        g2.drawLine(x, y, x, y + accentSize);
        g2.drawLine(x + width - 20, y, x + width - 20 - accentSize, y);
        g2.drawLine(x + width - 20, y, x + width - 20, y + accentSize);
        g2.drawLine(x, y + boxHeight, x + accentSize, y + boxHeight);
        g2.drawLine(x, y + boxHeight, x, y + boxHeight - accentSize);
        g2.drawLine(x + width - 20, y + boxHeight, x + width - 20 - accentSize, y + boxHeight);
        g2.drawLine(x + width - 20, y + boxHeight, x + width - 20, y + boxHeight - accentSize);
        
        g2.setStroke(new BasicStroke(1));
        
        // Title with hexagon bullet
        g2.setColor(themeColor);
        drawHexagon(g2, x + 10, y + 15, 5, true);
        
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        
        // Title glow
        g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 120));
        g2.drawString(title, x + 20, y + 20);
        
        // Title text
        g2.setColor(themeColor);
        g2.drawString(title, x + 21, y + 20);
        
        // Message - wrap text with holographic appearance
        g2.setFont(new Font("Consolas", Font.PLAIN, 11));
        g2.setColor(new Color(200, 230, 255, 240));
        
        String[] words = message.split(" ");
        String line = "";
        int lineY = y + 40;
        FontMetrics fm = g2.getFontMetrics();
        
        for (String word : words) {
            String testLine = line + word + " ";
            if (fm.stringWidth(testLine) > width - 50) {
                g2.drawString(line, x + 15, lineY);
                line = word + " ";
                lineY += 15;
            } else {
                line = testLine;
            }
        }
        g2.drawString(line, x + 15, lineY);
        
        return y + boxHeight + 10;
    }

    private int drawWarningBox(Graphics2D g2, String title, String message, int x, int y, int width) {
        return drawInfoBox(g2, title, message, x, y, width, new Color(255, 100, 100));
    }

    private int drawAbilityDetail(Graphics2D g2, String name, String defaultKey, String cooldown, Color color, String[] details, int x, int y, int width) {
        long time = System.currentTimeMillis();
        float pulse = (float)(Math.sin(time / 800.0) * 0.15 + 0.85);
        
        int headerHeight = 38;
        
        // Holographic header background with animated scanlines
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
        g2.fillRoundRect(x, y, width - 20, headerHeight, 10, 10);
        
        // Animated scanlines
        for (int i = 0; i < headerHeight; i += 2) {
            int offset = (int)((time / 20) % 4);
            if ((i + offset) % 4 == 0) {
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                g2.drawLine(x, y + i, x + width - 20, y + i);
            }
        }
        
        // Outer glow
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(80 * pulse)));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x - 1, y - 1, width - 18, headerHeight + 2, 10, 10);
        
        // Main border
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(230 * pulse)));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, width - 20, headerHeight, 10, 10);
        
        // Corner tech accents
        g2.setColor(new Color(0, 255, 200, (int)(180 * pulse)));
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int accentSize = 10;
        g2.drawLine(x, y, x + accentSize, y);
        g2.drawLine(x, y, x, y + accentSize);
        g2.drawLine(x + width - 20, y, x + width - 20 - accentSize, y);
        g2.drawLine(x + width - 20, y, x + width - 20, y + accentSize);
        
        g2.setStroke(new BasicStroke(1));
        
        // Hexagonal icon
        g2.setColor(color);
        drawHexagon(g2, x + 20, y + headerHeight / 2, 8, true);
        
        // Ability name with glow
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g2.drawString(name, x + 35, y + 24);
        
        g2.setColor(new Color(255, 255, 255));
        g2.drawString(name, x + 36, y + 24);
        
        // Default key in holographic bracket
        g2.setFont(new Font("Consolas", Font.PLAIN, 11));
        g2.setColor(new Color(0, 255, 200, (int)(200 * pulse)));
        String keyText = "[ KEY: " + defaultKey + " ]";
        g2.drawString(keyText, x + 160, y + 24);
        
        // Cooldown indicator
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(new Color(255, 180, 100, (int)(220 * pulse)));
        String cdText = "◉ CD: " + cooldown;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(cdText, x + width - 40 - fm.stringWidth(cdText), y + 24);
        
        y += headerHeight + 8;
        
        // Details with hexagonal bullets
        g2.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2.setColor(new Color(220, 230, 255, 240));
        for (String detail : details) {
            // Hexagonal bullet
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(180 * pulse)));
            drawHexagon(g2, x + 20, y - 5, 4, true);
            
            // Detail text
            g2.setColor(new Color(220, 230, 255, 240));
            g2.drawString(detail, x + 30, y);
            y += 19;
        }
        
        y += 15;
        return y;
    }

    private int drawKeyBindingsMiddle(Graphics2D g2, int x, int y, int width, 
                                    ControlConfig controlConfig, AbilityLoadout loadout) {
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(255, 200, 100));
        g2.drawString("KEY BINDINGS:", x, y);
        y += 40;
        
        String[] actions = {"ability1", "ability2", "ability3", "ability4", "shoot", "thrust", "left", "right", "hyper"};
        String[] labels = {"Ability 1", "Ability 2", "Ability 3", "Ability 4", "Shoot", "Thrust", "Rotate Left", "Rotate Right", "Hyper"};
        
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

        y += actions.length * 40 + 20;
        return y;
    }

    private int drawAbilitiesMiddle(Graphics2D g2, int x, int y, int width, AbilityLoadout loadout) {
        abilitySlotBounds.clear();
        
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(new Color(255, 200, 100));
        g2.drawString("ABILITY SLOTS", x, y);
        y += 50;
        
        // Draw 4 ability slots vertically
        for (int i = 0; i < 4; i++) {
            String ability = abilityLoadout.getSlot(i);
            String key = controlConfig.getKeyName(controlConfig.getKey("ability" + (i + 1)));
            
            int slotY = y + i * 90;
            Rectangle bounds = new Rectangle(x, slotY, width - 20, 75);
            abilitySlotBounds.put("slot" + i, bounds);
            middleOptionBounds.put("slot" + i, bounds);
            
            boolean isSelected = (i == selectedSlot);
            boolean isHovered = (i == hoveredSlot);
            
            // Background
            if (isSelected) {
                g2.setColor(new Color(100, 150, 255, 150));
            } else if (isHovered) {
                g2.setColor(new Color(100, 150, 255, 80));
            } else {
                g2.setColor(ability != null && !ability.isEmpty() ? 
                    new Color(60, 80, 120, 220) : new Color(40, 40, 40, 150));
            }
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);
            
            // Border
            g2.setColor(isSelected ? new Color(150, 200, 255) : new Color(100, 150, 255, 200));
            g2.setStroke(new BasicStroke(isSelected ? 3 : 2));
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);
            g2.setStroke(new BasicStroke(1));
            
            // Key binding label
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(new Color(255, 215, 0));
            g2.drawString("[" + key + "]", bounds.x + 15, bounds.y + 25);
            
            // Slot number
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(150, 150, 180));
            g2.drawString("Slot " + (i + 1), bounds.x + 15, bounds.y + bounds.height - 15);
            
            // Ability name or "Empty"
            if (ability != null && !ability.isEmpty()) {
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                g2.setColor(Color.WHITE);
                g2.drawString(ability.toUpperCase(), bounds.x + 80, bounds.y + 45);
            } else {
                g2.setFont(new Font("Arial", Font.ITALIC, 14));
                g2.setColor(new Color(120, 120, 120));
                g2.drawString("Empty - Click to assign", bounds.x + 80, bounds.y + 45);
            }
        }
        
        y += 4 * 90 + 30;
        
        // Instructions
        if (selectedSlot != -1) {
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(new Color(255, 215, 0));
            g2.drawString("→ Select an ability from the right panel", x, y);
            y += 25;
        } else {
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(150, 150, 180));
            g2.drawString("• Click a slot to view available abilities", x, y);
            y += 20;
            g2.drawString("• Available abilities will appear on the right", x, y);
            y += 25;
        }
        
        return y;
    }

    private int drawAbilityOptions(Graphics2D g2, int x, int y, int width) {
        availableAbilityBounds.clear();
        
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(255, 200, 100));
        g2.drawString("AVAILABLE ABILITIES", x, y);
        y += 40;
        
        String[] allAbilities = abilityLoadout.getAllAbilities();
        for (int i = 0; i < allAbilities.length; i++) {
            Rectangle bounds = new Rectangle(x, y, width - 40, 50);
            availableAbilityBounds.put(allAbilities[i], bounds);
            rightOptionBounds.put(allAbilities[i], bounds);
            
            boolean isHovered = (i == hoveredAbility);
            String currentAbility = abilityLoadout.getSlot(selectedSlot);
            boolean isEquipped = allAbilities[i].equals(currentAbility);
            
            // Background
            if (isEquipped) {
                g2.setColor(new Color(0, 255, 200, 100));
            } else if (isHovered) {
                g2.setColor(new Color(100, 150, 255, 80));
            } else {
                g2.setColor(new Color(50, 60, 80, 150));
            }
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            
            // Border
            g2.setColor(isEquipped ? new Color(0, 255, 200) : 
                    isHovered ? new Color(100, 150, 255) : new Color(80, 120, 180, 100));
            g2.setStroke(new BasicStroke(isEquipped ? 2 : 1));
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            g2.setStroke(new BasicStroke(1));
            
            // Text
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(isEquipped ? new Color(0, 255, 200) : 
                    isHovered ? Color.WHITE : new Color(200, 200, 220));
            g2.drawString(allAbilities[i].toUpperCase(), bounds.x + 15, bounds.y + 32);
            
            // "Equipped" label
            if (isEquipped) {
                g2.setFont(new Font("Arial", Font.PLAIN, 11));
                g2.setColor(new Color(0, 200, 180));
                FontMetrics fm = g2.getFontMetrics();
                String equipped = "(Equipped)";
                g2.drawString(equipped, bounds.x + bounds.width - fm.stringWidth(equipped) - 10, bounds.y + 32);
            }
            
            y += 60;
        }
        
        return y;
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
        g2.drawString("Version 1.0 - 2025", x, y);
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
        // Adjust x position to avoid content overlap
        int scrollbarWidth = 8;
        int adjustedX = x - scrollbarWidth - 4; // Move 4 pixels left for spacing
        
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
        int buttonSize = 60;
        int buttonX = panelX + WIDTH - buttonSize - 25;
        int buttonY = panelY + 25;
        
        closeButtonBounds = new Rectangle(buttonX, buttonY, buttonSize, buttonSize);
        
        int centerX = buttonX + buttonSize / 2;
        int centerY = buttonY + buttonSize / 2;
        
        // Animated glow pulse effect
        long time = System.currentTimeMillis();
        float pulse = (float)(Math.sin(time / 300.0) * 0.3 + 0.7);
        
        if (closeButtonHovered) {
            // Outer glow rings when hovered
            for (int i = 3; i > 0; i--) {
                int glowAlpha = (int)(30 * pulse / i);
                g2.setColor(new Color(255, 50, 100, glowAlpha));
                g2.setStroke(new BasicStroke(2 + i * 2));
                drawHexagon(g2, centerX, centerY, 28 + i * 4, false);
            }
        }
        
        // Main hexagonal background
        if (closeButtonHovered) {
            // Bright neon red/pink when hovered
            g2.setColor(new Color(255, 20, 80, 220));
        } else {
            // Dark with subtle glow when not hovered
            g2.setColor(new Color(40, 10, 20, 200));
        }
        drawHexagon(g2, centerX, centerY, 28, true);
        
        // Hexagonal border with neon glow
        if (closeButtonHovered) {
            // Bright neon outline
            g2.setColor(new Color(255, 50, 150, 255));
            g2.setStroke(new BasicStroke(3));
        } else {
            // Subtle cyan-tinted outline
            g2.setColor(new Color(255, 80, 120, 180));
            g2.setStroke(new BasicStroke(2));
        }
        drawHexagon(g2, centerX, centerY, 28, false);
        
        // Inner hexagon for depth
        g2.setColor(new Color(255, 100, 150, 100));
        g2.setStroke(new BasicStroke(1));
        drawHexagon(g2, centerX, centerY, 22, false);
        
        // Sci-fi corner accents
        g2.setColor(closeButtonHovered ? new Color(0, 255, 200, 255) : new Color(0, 255, 200, 120));
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int accentSize = 6;
        // Top corners
        g2.drawLine(centerX - 18, centerY - 28, centerX - 18 + accentSize, centerY - 28);
        g2.drawLine(centerX - 18, centerY - 28, centerX - 18, centerY - 28 + accentSize);
        g2.drawLine(centerX + 18, centerY - 28, centerX + 18 - accentSize, centerY - 28);
        g2.drawLine(centerX + 18, centerY - 28, centerX + 18, centerY - 28 + accentSize);
        
        // X mark - futuristic crossed lines
        g2.setColor(closeButtonHovered ? new Color(255, 255, 255, 255) : new Color(255, 150, 180, 200));
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int offset = 10;
        g2.drawLine(centerX - offset, centerY - offset, centerX + offset, centerY + offset);
        g2.drawLine(centerX + offset, centerY - offset, centerX - offset, centerY + offset);
        
        // Add glow to X when hovered
        if (closeButtonHovered) {
            g2.setColor(new Color(255, 255, 255, 80));
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(centerX - offset, centerY - offset, centerX + offset, centerY + offset);
            g2.drawLine(centerX + offset, centerY - offset, centerX - offset, centerY + offset);
        }
        
        g2.setStroke(new BasicStroke(1));
        
        // Holographic tooltip when hovered
        if (closeButtonHovered) {
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            String tooltip = "[ EXIT ]";
            FontMetrics fm = g2.getFontMetrics();
            int textX = buttonX - fm.stringWidth(tooltip) - 15;
            int textY = buttonY + buttonSize / 2 + fm.getAscent() / 2 - 2;
            
            // Holographic background with scanlines
            g2.setColor(new Color(0, 255, 200, 30));
            g2.fillRect(textX - 8, textY - fm.getAscent() - 3, fm.stringWidth(tooltip) + 16, fm.getHeight() + 2);
            
            // Scanline effect
            for (int i = 0; i < fm.getHeight() + 2; i += 2) {
                g2.setColor(new Color(0, 255, 200, 15));
                g2.drawLine(textX - 8, textY - fm.getAscent() - 3 + i, 
                           textX + fm.stringWidth(tooltip) + 8, textY - fm.getAscent() - 3 + i);
            }
            
            // Neon border
            g2.setColor(new Color(0, 255, 200, 200));
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(textX - 8, textY - fm.getAscent() - 3, fm.stringWidth(tooltip) + 16, fm.getHeight() + 2);
            
            // Glowing text
            g2.setColor(new Color(0, 255, 200, 100));
            g2.drawString(tooltip, textX - 1, textY);
            g2.drawString(tooltip, textX + 1, textY);
            g2.setColor(new Color(255, 255, 255, 255));
            g2.drawString(tooltip, textX, textY);
            
            g2.setStroke(new BasicStroke(1));
        }
    }
    
    // Helper method to draw hexagons
    private void drawHexagon(Graphics2D g2, int centerX, int centerY, int radius, boolean fill) {
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 6;
            xPoints[i] = (int)(centerX + radius * Math.cos(angle));
            yPoints[i] = (int)(centerY + radius * Math.sin(angle));
        }
        
        Polygon hexagon = new Polygon(xPoints, yPoints, 6);
        if (fill) {
            g2.fillPolygon(hexagon);
        } else {
            g2.drawPolygon(hexagon);
        }
    }
    
    // Getters
    public int getSFXVolume() { return sfxVolume; }
    public int getMusicVolume() { return musicVolume; }
    public boolean isEffectsEnabled() { return effects; }
    public boolean isScreenShakeEnabled() { return screenShake; }
}