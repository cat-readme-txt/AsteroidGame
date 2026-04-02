import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class StoryTerminal {
    private static final int CHAR_DELAY = 30; // milliseconds per character
    private static final int PARAGRAPH_PAUSE = 2000; // pause after paragraph completes
    private static final int MAX_VISIBLE_LINES = 18; // lines visible before fading
    private static final int FADE_START_LINE = 14; // line where fading begins
    
    private final List<String> storySegments;
    private final List<String> displayedLines;
    private int currentSegment = 0;
    private String currentLine = "";
    private int charIndex = 0;
    private long lastCharTime = 0;
    private boolean waitingForInput = false;
    private boolean storyComplete = false;
    private String userInput = "";
    private int scrollOffset = 0;
    
    // Animation variables
    private long bootTime = 0;
    private boolean booting = true;
    private int bootPhase = 0;
    
    public StoryTerminal() {
        displayedLines = new ArrayList<>();
        storySegments = new ArrayList<>();
        initializeStory();
        bootTime = System.currentTimeMillis();
    }
    
    private void initializeStory() {
        // Boot sequence
        storySegments.add("VOID_NAVIGATOR_OS v4.2.7 [INITIALIZING...]");
        storySegments.add(">> Loading dimensional protocol suite...");
        storySegments.add(">> Establishing quantum communication array...");
        storySegments.add(">> SYSTEM READY");
        storySegments.add("");
        storySegments.add("================================================================================");
        storySegments.add("                    CLASSIFIED MISSION BRIEFING");
        storySegments.add("         IMPERIAL ASTROSCIENCE DIRECTORATE - VOID RESEARCH DIVISION");
        storySegments.add("================================================================================");
        storySegments.add("");
        
        // Mission Background
        storySegments.add("SUBJECT: Dimensional Reconnaissance Mission Briefing");
        storySegments.add("CLASSIFICATION: LEVEL 7 - IMPERIAL EYES ONLY");
        storySegments.add("OPERATIVE: You (ID: VOID-NAVIGATOR-001)");
        storySegments.add("");
        storySegments.add("You are a deep-space reconnaissance specialist commissioned by the Galactic");
        storySegments.add("Imperium to conduct preliminary investigation of a newly-detected parallel");
        storySegments.add("dimension, designated as the \"Void Continuum.\"");
        storySegments.add("");
        
        // Scientific Discovery
        storySegments.add("Recent quantum-topological surveys have identified anomalous spatial signatures");
        storySegments.add("emanating from subspace fractures at coordinates [REDACTED]. Preliminary");
        storySegments.add("spectroscopic analysis suggests the presence of exotic matter configurations");
        storySegments.add("exhibiting properties inconsistent with standard-model physics.");
        storySegments.add("");
        storySegments.add("These novel elemental structures possess unprecedented energy density ratios,");
        storySegments.add("potentially catalyzing breakthroughs in propulsion technology, energy");
        storySegments.add("generation, and materials science. Furthermore, initial dimensional mapping");
        storySegments.add("indicates spatial curvature patterns conducive to superluminal transit via");
        storySegments.add("controlled manifold compression—effectively enabling instantaneous traversal");
        storySegments.add("of vast interstellar distances.");
        storySegments.add("");
        
        // Mission Objectives
        storySegments.add("PRIMARY OBJECTIVES:");
        storySegments.add("  [1] Survey dimensional topology and catalog exotic matter distributions");
        storySegments.add("  [2] Assess viability of Void Continuum for industrial resource extraction");
        storySegments.add("  [3] Evaluate potential for dimensional-warp transit network establishment");
        storySegments.add("  [4] Return with comprehensive analytical data and material samples");
        storySegments.add("");
        
        // Deployment
        storySegments.add("You were deployed aboard the ISV HORIZON SEEKER, a prototype vessel equipped");
        storySegments.add("with the Empire's cutting-edge Dimensional Phase Translocator (DPT-MKIV).");
        storySegments.add("This experimental technology exploits quantum entanglement cascades to");
        storySegments.add("facilitate cross-dimensional translation. The DPT maintains a tethered");
        storySegments.add("quantum link to baseline reality, permitting bidirectional transit provided");
        storySegments.add("you remain within the coherence envelope (estimated radius: 10^6 km).");
        storySegments.add("");
        storySegments.add("Translation sequence initiated. Dimensional membrane penetration successful.");
        storySegments.add("Welcome to the Void Continuum.");
        storySegments.add("");
        
        // Initial Exploration
        storySegments.add("================================================================================");
        storySegments.add("                         MISSION LOG - INITIAL PHASE");
        storySegments.add("================================================================================");
        storySegments.add("");
        storySegments.add("Initial exploration proceeded nominally. Sensor arrays detected abundant");
        storySegments.add("concentrations of previously-unknown hyperdense elements exhibiting");
        storySegments.add("non-Euclidean crystalline matrices. Energy signatures confirmed theoretical");
        storySegments.add("predictions. Mission objectives appeared achievable within projected timeline.");
        storySegments.add("");
        
        // Discovery of Anomaly
        storySegments.add("However, extended observation revealed a disturbing phenomenon:");
        storySegments.add("The Void Continuum's spacetime geometry is fundamentally unstable.");
        storySegments.add("");
        storySegments.add("Local spatial metrics undergo continuous microscopic fluctuations—quantum-scale");
        storySegments.add("perturbations in the dimensional fabric itself. These variations manifest as");
        storySegments.add("random, imperceptible positional drift. Over time, cumulative displacement");
        storySegments.add("occurs without detectable acceleration or velocity change relative to the");
        storySegments.add("local reference frame.");
        storySegments.add("");
        storySegments.add("In essence: You were being displaced through space without moving.");
        storySegments.add("");
        
        // Crisis Point
        storySegments.add("By the time you recognized this spatial drift phenomenon, you had been");
        storySegments.add("passively translated beyond the DPT's quantum coherence threshold.");
        storySegments.add("Communication with Imperial Command: TERMINATED.");
        storySegments.add("Dimensional translocation capability: OFFLINE.");
        storySegments.add("Emergency recall protocol: INOPERATIVE.");
        storySegments.add("");
        storySegments.add("You are stranded in an unstable dimension, beyond rescue range, with no");
        storySegments.add("established method of return to baseline reality.");
        storySegments.add("");
        
        // Continued Exploration
        storySegments.add("================================================================================");
        storySegments.add("                        SURVIVAL PROTOCOL INITIATED");
        storySegments.add("================================================================================");
        storySegments.add("");
        storySegments.add("Driven by determination to return home, you continued systematic exploration");
        storySegments.add("of the Void Continuum, searching for phenomena that might exploit principles");
        storySegments.add("beyond current Imperial scientific understanding.");
        storySegments.add("");
        storySegments.add("What you discovered was not salvation—but horror.");
        storySegments.add("");
        
        // Void Creatures Discovery
        storySegments.add("ALERT: Hostile extradimensional entities detected.");
        storySegments.add("Classification: VOID PREDATORS.");
        storySegments.add("");
        storySegments.add("These organisms exist outside conventional biological frameworks. They are");
        storySegments.add("hyperdimensional parasites that actively consume localized spacetime itself,");
        storySegments.add("metabolizing dimensional substrate as an energy source. Their feeding activity");
        storySegments.add("generates the cascading metric instabilities causing the Void's spatial drift.");
        storySegments.add("");
        storySegments.add("They are the root cause of the dimensional chaos.");
        storySegments.add("They are aware of your presence.");
        storySegments.add("They are hostile.");
        storySegments.add("");
        
        // Ship Corruption
        storySegments.add("CRITICAL ALERT: Hull integrity anomalies detected.");
        storySegments.add("");
        storySegments.add("Extended exposure to the Void Continuum's non-standard physical laws is");
        storySegments.add("inducing progressive structural degradation in the ISV HORIZON SEEKER.");
        storySegments.add("Molecular lattices are destabilizing. Quantum field interactions deviate");
        storySegments.add("increasingly from baseline-universe norms. Projections indicate complete");
        storySegments.add("structural collapse within [ESTIMATED: 72 HOURS].");
        storySegments.add("");
        storySegments.add("Your vessel—your only shelter—is being eroded by the dimension itself.");
        storySegments.add("Psychological stability assessment: DETERIORATING.");
        storySegments.add("Recommended action: IMMEDIATE EXTRACTION (UNAVAILABLE).");
        storySegments.add("");
        
        // The Hunt
        storySegments.add("With sanity eroding and time running out, you made a desperate decision:");
        storySegments.add("Hunt the apex predator.");
        storySegments.add("");
        storySegments.add("Sensor telemetry identified a massive void-entity: a segmented hyperdimensional");
        storySegments.add("organism resembling a colossal centipede, measuring approximately 200 meters");
        storySegments.add("in length. Its biomass exhibited extreme spacetime-warping properties,");
        storySegments.add("suggesting a concentrated void-energy nexus within its core structure.");
        storySegments.add("");
        storySegments.add("After a prolonged engagement utilizing every remaining weapons system,");
        storySegments.add("you successfully neutralized the entity and extracted its energetic core—");
        storySegments.add("a pulsating crystalline organ radiating intense dimensional flux.");
        storySegments.add("");
        
        // Ship Modification
        storySegments.add("Operating under extreme duress and with minimal equipment, you performed");
        storySegments.add("emergency retrofitting of the ISV HORIZON SEEKER's propulsion and dimensional");
        storySegments.add("systems, integrating the void-creature's core as a hybrid power source.");
        storySegments.add("");
        storySegments.add("The modification was successful.");
        storySegments.add("");
        storySegments.add("Your vessel now possesses limited autonomous dimensional translation");
        storySegments.add("capability—enabling controlled phase-shifting between the Void Continuum");
        storySegments.add("and baseline spacetime without requiring external tethering.");
        storySegments.add("");
        storySegments.add("You can return home.");
        storySegments.add("");
        
        // Emergence
        storySegments.add("Initiating dimensional egress sequence...");
        storySegments.add("Phase transition commencing...");
        storySegments.add("Membrane penetration: SUCCESSFUL.");
        storySegments.add("");
        storySegments.add("Re-entry into baseline universe confirmed.");
        storySegments.add("Astrogation systems recalibrating...");
        storySegments.add("Stellar position triangulation in progress...");
        storySegments.add("");
        storySegments.add("ALERT: Current location does not match any catalogued star systems.");
        storySegments.add("Estimated distance from Imperial Core Territories: [CALCULATING...]");
        storySegments.add("Distance estimate: BEYOND SURVEYED SPACE.");
        storySegments.add("");
        storySegments.add("The dimensional translation displaced you across unknown regions of the");
        storySegments.add("galaxy. You are lost in uncharted space, thousands of lightyears from");
        storySegments.add("any Imperial presence.");
        storySegments.add("");
        
        // New Threats
        storySegments.add("================================================================================");
        storySegments.add("                      HAZARD ASSESSMENT - BASELINE REALITY");
        storySegments.add("================================================================================");
        storySegments.add("");
        storySegments.add("Threat analysis indicates multiple hostile elements in your vicinity:");
        storySegments.add("");
        storySegments.add("  [1] COSMIC PREDATORS: Vast extradimensional organisms migrating through");
        storySegments.add("      realspace. These entities feed on electromagnetic emissions and are");
        storySegments.add("      attracted to active starship signatures. Extremely aggressive.");
        storySegments.add("");
        storySegments.add("  [2] GRAVITATIONAL ANOMALIES: Your dimensional exit trajectory generated");
        storySegments.add("      severe spacetime perturbations, spawning micro-singularities that have");
        storySegments.add("      coalesced into unstable black holes. Gravitational hazard: EXTREME.");
        storySegments.add("");
        storySegments.add("  [3] ASTEROID FIELDS: Dense debris clusters from ancient stellar collapse");
        storySegments.add("      events. High-velocity collision risk. Recommend immediate evasive action.");
        storySegments.add("");
        
        // Final Mission
        storySegments.add("PRIMARY OBJECTIVE UPDATED:");
        storySegments.add("Navigate back to Imperial space using available resources and improvised");
        storySegments.add("dimensional-warp capabilities. Survival probability: LOW.");
        storySegments.add("");
        storySegments.add("Secondary objective: Document all encounters for future Imperial expeditions.");
        storySegments.add("");
        storySegments.add("Your journey home begins now.");
        storySegments.add("");
        storySegments.add("================================================================================");
        storySegments.add("                         END MISSION BRIEFING");
        storySegments.add("                      GOOD LUCK, VOID NAVIGATOR");
        storySegments.add("================================================================================");
    }
    
    public void update() {
        long currentTime = System.currentTimeMillis();
        
        // Boot sequence
        if (booting) {
            if (currentTime - bootTime > 1500) {
                bootPhase++;
                bootTime = currentTime;
                
                if (bootPhase >= 4) {
                    booting = false;
                }
            }
            return;
        }
        
        if (storyComplete || waitingForInput) {
            return;
        }
        
        // Type out current segment
        if (currentSegment < storySegments.size()) {
            String fullSegment = storySegments.get(currentSegment);
            
            if (charIndex < fullSegment.length()) {
                if (currentTime - lastCharTime >= CHAR_DELAY) {
                    charIndex++;
                    currentLine = fullSegment.substring(0, charIndex);
                    lastCharTime = currentTime;
                }
            } else {
                // Segment complete
                displayedLines.add(fullSegment);
                currentSegment++;
                charIndex = 0;
                currentLine = "";
                
                // Check if this was a paragraph end (empty line or specific markers)
                if (fullSegment.isEmpty() || fullSegment.contains("====")) {
                    waitingForInput = true;
                }
                
                // Check if story is complete
                if (currentSegment >= storySegments.size()) {
                    storyComplete = true;
                    waitingForInput = true;
                }
            }
        }
    }
    
    public void handleInput(String input) {
        if (waitingForInput) {
            input = input.toLowerCase().trim();
            
            if (storyComplete) {
                // Story is done, only accept exit
                if (input.equals("exit") || input.equals("")) {
                    // Signal to return to menu
                }
            } else {
                // Continue story
                if (input.equals("continue") || input.equals("")) {
                    waitingForInput = false;
                }
            }
        }
    }
    
    public boolean isExitRequested() {
        if (storyComplete && userInput.isEmpty()) {
            return true;
        }
        return storyComplete && (userInput.toLowerCase().equals("exit"));
    }
    
    public void draw(Graphics2D g2, int width, int height) {
        // Terminal background
        g2.setColor(new Color(0, 10, 15, 250));
        g2.fillRect(0, 0, width, height);
        
        // Scanline effect
        long time = System.currentTimeMillis();
        for (int y = 0; y < height; y += 2) {
            int alpha = (int)(5 + 3 * Math.sin(time / 100.0 + y / 10.0));
            g2.setColor(new Color(0, 255, 200, alpha));
            g2.drawLine(0, y, width, y);
        }
        
        // Boot sequence
        if (booting) {
            drawBootSequence(g2, width, height);
            return;
        }
        
        // Draw story content
        int lineHeight = 22;
        int startY = height - 150;
        int currentY = startY;
        
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        
        // Draw displayed lines with fade effect
        int visibleLines = Math.min(displayedLines.size(), MAX_VISIBLE_LINES);
        int startLine = Math.max(0, displayedLines.size() - visibleLines);
        
        for (int i = startLine; i < displayedLines.size(); i++) {
            int linePos = i - startLine;
            int alpha = 255;
            
            // Fade older lines
            if (linePos < FADE_START_LINE && displayedLines.size() > MAX_VISIBLE_LINES) {
                float fadeProgress = (float)linePos / FADE_START_LINE;
                alpha = (int)(255 * fadeProgress);
            }
            
            g2.setColor(new Color(0, 255, 200, alpha));
            
            String line = displayedLines.get(i);
            int drawY = startY - (visibleLines - linePos - 1) * lineHeight - 30;
            
            // Add glow to certain keywords
            if (line.contains("ALERT") || line.contains("CRITICAL") || line.contains("WARNING")) {
                g2.setColor(new Color(255, 100, 100, alpha));
            } else if (line.contains("OBJECTIVE") || line.contains("MISSION")) {
                g2.setColor(new Color(255, 200, 0, alpha));
            } else if (line.contains("====")) {
                g2.setColor(new Color(0, 200, 255, alpha));
            }
            
            g2.drawString(line, 30, drawY);
        }
        
        // Draw current typing line
        if (!currentLine.isEmpty()) {
            g2.setColor(new Color(0, 255, 200, 255));
            g2.drawString(currentLine, 30, startY);
            
            // Blinking cursor
            if ((time / 500) % 2 == 0) {
                g2.fillRect(35 + g2.getFontMetrics().stringWidth(currentLine), startY - 12, 8, 14);
            }
        }
        
        // Command prompt
        drawCommandPrompt(g2, width, height);
    }
    
    private void drawBootSequence(Graphics2D g2, int width, int height) {
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        g2.setColor(new Color(0, 255, 200, 255));
        
        int y = height / 2 - 60;
        
        if (bootPhase >= 0) {
            g2.drawString(">> INITIALIZING VOID NAVIGATOR OS...", width / 2 - 200, y);
        }
        if (bootPhase >= 1) {
            g2.drawString(">> LOADING QUANTUM PROTOCOLS...", width / 2 - 200, y + 30);
        }
        if (bootPhase >= 2) {
            g2.drawString(">> ESTABLISHING SECURE CONNECTION...", width / 2 - 200, y + 60);
        }
        if (bootPhase >= 3) {
            g2.setColor(new Color(0, 255, 100, 255));
            g2.drawString(">> SYSTEM READY", width / 2 - 200, y + 90);
        }
        
        // Progress indicator
        long time = System.currentTimeMillis();
        int dots = (int)((time / 300) % 4);
        String dotString = ".".repeat(dots);
        g2.setColor(new Color(0, 200, 255, 200));
        g2.drawString(dotString, width / 2 + 100, y + 120);
    }
    
    private void drawCommandPrompt(Graphics2D g2, int width, int height) {
        int promptY = height - 80;
        
        // Prompt background
        g2.setColor(new Color(0, 20, 30, 230));
        g2.fillRect(0, promptY - 30, width, 110);
        
        // Top border with glow
        g2.setColor(new Color(0, 255, 200, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, promptY - 30, width, promptY - 30);
        g2.setStroke(new BasicStroke(1));
        
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        
        if (waitingForInput) {
            if (storyComplete) {
                g2.setColor(new Color(0, 255, 100, 255));
                g2.drawString("MISSION BRIEFING COMPLETE", 30, promptY);
                g2.setColor(new Color(255, 200, 0, 255));
                g2.drawString(">> Type 'exit' or press ENTER to return to main menu", 30, promptY + 25);
            } else {
                g2.setColor(new Color(255, 200, 0, 255));
                g2.drawString(">> Type 'continue' or press ENTER to proceed", 30, promptY);
            }
        } else {
            g2.setColor(new Color(0, 255, 200, 180));
            g2.drawString(">> Transmitting data stream...", 30, promptY);
        }
        
        // User input line
        g2.setColor(new Color(0, 255, 200, 255));
        g2.drawString("> " + userInput, 30, promptY + 50);
        
        // Blinking cursor
        long time = System.currentTimeMillis();
        if ((time / 500) % 2 == 0) {
            g2.fillRect(38 + g2.getFontMetrics().stringWidth(userInput), promptY + 38, 8, 14);
        }
    }
    
    public void addChar(char c) {
        if (c == '\n' || c == '\r') {
            handleInput(userInput);
            userInput = "";
        } else if (c == '\b' || c == 127) {
            if (userInput.length() > 0) {
                userInput = userInput.substring(0, userInput.length() - 1);
            }
        } else if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
            userInput += c;
        }
    }
    
    public boolean isStoryComplete() {
        return storyComplete;
    }
    
    public boolean isWaitingForInput() {
        return waitingForInput;
    }
}