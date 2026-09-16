import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

public final class AsteroidGame extends JPanel implements ActionListener, KeyListener {
    private static final int MAX_VOID_HAZARDS = 5;
    // Alpha Fades and Timers
    private int level;
	private ShipLevel shipLevel;
	private int levelUpFrame = 0;
	private boolean showingLevelUp = false;
	private final Timer timer;
	private final GameClock gameClock = new GameClock(System.nanoTime());
	private int spawnCreaturesCooldown = 0;
	private int spawnEntitiesCooldown = 0;
	private int deathCountdown;
    private int levelStartCountdown;
	private int levelDisplayAlpha;
	private int levelTimer;
	private boolean levelStarting = false; 
	private int voidCooldownTimer = 0;
	private final int voidMaxCooldown = 3 * 60;
	private int fadeCooldownTimer = 0;
	private final int fadeMaxCooldown = 8 * 60;
		
	// Game Objects
	private final List<Asteroid> asteroids = new ArrayList<>();
	private final List<Bullet> bullets = new ArrayList<>();
	private final List<VoidLaser> voidLasers = new ArrayList<>();
	private final List<VoidHazard> voidHazards = new ArrayList<>();
	private final List<BlackHole> blackHoles = new ArrayList<>();
	private final List<CosmicEntity> cosmicEntities = new ArrayList<>();
	private final List<VoidCreature> voidCreatures = new ArrayList<>();
	// Collision & Physics Handling
	private final List<Asteroid> asteroidsToAdd = new ArrayList<>();
	private final List<VoidHazard> hazardsToAdd = new ArrayList<>();
	private final Set<Asteroid> asteroidsToRemove = new HashSet<>();
	private final Set<VoidLaser> lasersToRemove = new HashSet<>();
	private final Set<Bullet> bulletsToRemove = new HashSet<>();
	private final Set<VoidHazard> voidHazardsToRemove = new HashSet<>();
	private final Set<BlackHole> blackHolesToRemove = new HashSet<>();
	private final Set<CosmicEntity> cosmicEntitiesToRemove = new HashSet<>();
	private final Set<VoidCreature> creaturesToRemove = new HashSet<>();
	
	// ====== Asthetics ======
	private final GalaxyBackground galaxyBackground;
	private final SpaceGrid spaceGrid;
	private final StarField starField;
	private final ParticleSystem particleSystem;
	// Death screen animation fields
	private int deathExplosionFrame;
	private boolean deathExplosionDone = false;    
    
	// ====== Ship ======
	// ------ Thrust ------
    private boolean thrusting;
    private boolean rotatingLeft;
    private boolean rotatingRight;
	// ------ Abilities ------
	// Ability Manager
	private AbilityManager abilityManager;
	// Fade
	private boolean fadeActive;
	private int fadeTimer;
	private final int maxFadeTime = 5 * 60;
	// Teleport
	private TeleportAnchor teleportAnchor;
	private int teleportFlashFrame = 0;

	// ====== Reward Systems ======
	private int xpGain;
	private int fuelGain;
	private int scoreGain;
	// Score and asteroid destruction counters
	private int score;
	private int largeDestroyed;
	private int mediumDestroyed;
	private int smallDestroyed;
	// Best Score & Duration
	private int highestScore, highestDuration;


    // ====== Game General ======
	private static int WIDTH = 1200, HEIGHT = 700;
    private final Random rand = new Random();
    private boolean noSavedGame;
    private Ship ship;
    private String gameState = "menu"; // "menu", "playing", "paused", "death"
	private int hoveredButton;
	private FloatingTextManager floatingTextManager;
	// Deaths
	private boolean voidDeath;
	private boolean bulletDeath;
    private boolean asteroidDeath;
	private boolean voidHazardDeath;
	private boolean blackHoleDeath;
	private boolean cosmicEntityDeath;
	private boolean voidCreatureDeath;
	// Save system
	private final GameSaveStore saveStore;
	// Control Config
	private ControlConfig controlConfig;
	private AbilityLoadout abilityLoadout;
	private SettingsPanel settingsPanel;
	private boolean showingSettings = false;
	// Game Stats
	private final int maxBlackHoles = 3;
	private int targetTotalCreatureSizeValue = 0;
	private int currentTotalCreatureSizeValue = 0;
	private int targetTotalEntitySizeValue = 0;
	private int currentTotalEntitySizeValue = 0;
	// Story Terminal
	private StoryTerminal storyTerminal;
	private boolean showingStory = false;
	private boolean menuListenersInstalled;
	private final Font menuTitleFont = FontLoader.loadAquireBold(100f);
	private final Font menuButtonFont = FontLoader.loadAquireLight(24f);
	private final Font menuSubtitleFont = new Font("Courier New", Font.ITALIC, 20);
	private Shape menuTitleOutline;
	private FontRenderContext menuTitleContext;
	private int menuTitleWidth = -1;
	private int menuTitleX;
	private BufferedImage menuBackgroundLayer;
	private boolean menuBackgroundDirty = true;


	public AsteroidGame() {
		timer = new Timer(16, this); // Wake up to advance elapsed simulation time, then repaint.
		saveStore = new GameSaveStore(AppPaths.saveFile());

		// Initialize persistent configuration objects before resetVars
		abilityLoadout = new AbilityLoadout();
		controlConfig = new ControlConfig();
		
		resetVars();

		starField = new StarField(WIDTH, HEIGHT, 150);
		particleSystem = new ParticleSystem();
		galaxyBackground = new GalaxyBackground();
		spaceGrid = new SpaceGrid();

		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setFocusable(true);

		setupMenu();
		addKeyListener(this);

		addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(java.awt.event.ComponentEvent e) {
				updateDimensions();
				repaint();
			}
		});

		gameClock.reset(System.nanoTime());
		timer.start();
	}

	private void resetVars() {
	    score = 0;
	    largeDestroyed = 0;
	    mediumDestroyed = 0;
	    smallDestroyed = 0;
		level = 0;
		spawnCreaturesCooldown = 0;
		spawnEntitiesCooldown = 0;
		targetTotalCreatureSizeValue = 0;
		currentTotalCreatureSizeValue = 0;
		targetTotalEntitySizeValue = 0;
		currentTotalEntitySizeValue = 0;
		clearPendingChanges();
	    deathCountdown = 5 * 60;		// 5 seconds at 60 FPS
	    levelStartCountdown = 3 * 60; 	// countdown before level starts (1 second)
	    levelDisplayAlpha = 255; 		// for fade out effect
	    levelTimer = 30 * 60; 			// 30 seconds * 60 FPS (assuming 60 FPS)
	    levelStarting = false;
	    thrusting = false;
	    rotatingLeft = false;
	    rotatingRight = false;
	    voidDeath = false;
	    bulletDeath = false;
	    asteroidDeath = false;
		voidHazardDeath = false;
		blackHoleDeath = false;
		cosmicEntityDeath = false;
		voidCreatureDeath = false;
	    deathExplosionFrame = 0;
	    deathExplosionDone = false;
		fadeActive = false;
		fadeTimer = 5 * 60; // 5 seconds
		ship = new Ship();
		shipLevel = new ShipLevel();
		teleportAnchor = new TeleportAnchor();
		abilityManager = new AbilityManager();
		abilityManager.syncWithLoadout(abilityLoadout, shipLevel);
		floatingTextManager = new FloatingTextManager();
		voidHazards.clear();
		blackHoles.clear();
		cosmicEntities.clear();
		teleportFlashFrame = 0;
		levelUpFrame = 0;
		showingLevelUp = false;
		voidCooldownTimer = 0;
		fadeCooldownTimer = 0;
		hoveredButton = -1;
		settingsPanel = new SettingsPanel(WIDTH, HEIGHT, controlConfig, abilityLoadout);
		showingSettings = false;
		storyTerminal = new StoryTerminal();
		showingStory = false;
		refreshSaveMetadata();
	}

    void setupMenu() {
        setLayout(null);
        setBackground(Color.BLACK);
        menuBackgroundDirty = true;
        spaceGrid.releaseCache();
		if (menuListenersInstalled) return;
		menuListenersInstalled = true;
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!gameState.equals("menu")) return;
				
				if (showingSettings) {
					settingsPanel.handleMousePress(e.getX(), e.getY(),
						(WIDTH - (int)(WIDTH * 0.85)) / 2,
						(HEIGHT - (int)(HEIGHT * 0.75)) / 2,
						(int)(WIDTH * 0.85),
						(int)(HEIGHT * 0.75));
					repaint();
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (!gameState.equals("menu")) return;
				
				if (showingSettings) {
					settingsPanel.handleMouseRelease();
				}
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!gameState.equals("menu")) return;
				
				int mx = e.getX();
				int my = e.getY();
				
				if (showingSettings) {
					boolean closeClicked = settingsPanel.handleClick(mx, my, AsteroidGame.this);
					
					if (closeClicked) {
						showingSettings = false;
						abilityManager.syncWithLoadout(abilityLoadout, shipLevel);
					}
					repaint();
					return;
				}
				
				// New Game button
				if (mx >= WIDTH/2 - 150 && mx <= WIDTH/2 + 150 &&
					my >= HEIGHT/2 - 20 && my <= HEIGHT/2 + 30) {
					startNewGame();
				}
				
				// Resume button
				if (mx >= WIDTH/2 - 150 && mx <= WIDTH/2 + 150 &&
					my >= HEIGHT/2 + 50 && my <= HEIGHT/2 + 100) {
					resumeGame();
				}
				
				// Settings button
				if (mx >= WIDTH/2 - 150 && mx <= WIDTH/2 + 150 &&
					my >= HEIGHT/2 + 120 && my <= HEIGHT/2 + 170) {
					showingSettings = !showingSettings;
				}

				// Story button
				if (mx >= WIDTH/2 - 150 && mx <= WIDTH/2 + 150 &&
					my >= HEIGHT/2 + 190 && my <= HEIGHT/2 + 240) {
					showingStory = true;
					storyTerminal = new StoryTerminal(); // Reset story each time
				}
				
				repaint();
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (!gameState.equals("menu")) return;
				
				int mx = e.getX();
				int my = e.getY();
				
				if (showingSettings) {
					settingsPanel.handleMouseMove(mx, my);
					repaint();
					return;
				}
				
				if (showingSettings) return;
				
				hoveredButton = -1;
				if (mx >= WIDTH/2 - 150 && mx <= WIDTH/2 + 150 && 
						my >= HEIGHT/2 - 20 && my <= HEIGHT/2 + 30) {
					hoveredButton = 0; // New Game
				} else if (mx >= WIDTH/2 - 150 && mx <= WIDTH/2 + 150 && 
						my >= HEIGHT/2 + 50 && my <= HEIGHT/2 + 100) {
					hoveredButton = 1; // Resume
				} else if (mx >= WIDTH/2 - 150 && mx <= WIDTH/2 + 150 && 
						my >= HEIGHT/2 + 120 && my <= HEIGHT/2 + 170) {
					hoveredButton = 2; // Settings
				} else if (mx >= WIDTH/2 - 150 && mx <= WIDTH/2 + 150 && 
						my >= HEIGHT/2 + 190 && my <= HEIGHT/2 + 240) {
					hoveredButton = 3; // Story
				}
				repaint();
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (showingSettings) {
					settingsPanel.handleMouseDrag(e.getX(), e.getY(),
						(WIDTH - (int)(WIDTH * 0.85)) / 2,
						(HEIGHT - (int)(HEIGHT * 0.75)) / 2,
						(int)(WIDTH * 0.85),
						(int)(HEIGHT * 0.75));
					repaint();
				}
			}
		});
		
		addMouseWheelListener(e -> {
			if (showingSettings) {
				int mx = e.getX();
				settingsPanel.handleScroll(e.getWheelRotation(), mx, 
					(WIDTH - (int)(WIDTH * 0.85)) / 2,
					(HEIGHT - (int)(HEIGHT * 0.75)) / 2,
					(int)(WIDTH * 0.85),
					(int)(HEIGHT * 0.75));
				repaint();
			}
		});
	}
    
	void startNewGame() {
	    gameState = "playing";
        gameClock.reset(System.nanoTime());
		menuBackgroundLayer = null;
		menuBackgroundDirty = true;
	    resetVars();
	    ship = new Ship();
	    asteroids.clear();
	    bullets.clear();
		voidLasers.clear();
		voidHazards.clear();
		voidCreatures.clear();
		blackHoles.clear();
		cosmicEntities.clear();

		updateDimensions();
	
	    levelStarting = true;

	    // Starting a fresh run should only clear resumable progress, not best stats.
		saveStore.clearActiveSession(highestScore, highestDuration);
		noSavedGame = true;

	    revalidate();
	    repaint();
	    requestFocusInWindow();
	}

    void resumeGame() {
		GameSaveState saveState = saveStore.loadState();
		highestScore = saveState.highestScore();
		highestDuration = saveState.highestDuration();
		GameSessionSnapshot session = saveState.activeSession();

		if (session == null) {
			noSavedGame = true;
			return;
		}

		level = session.level();
		score = session.score();
		ship.setFuel(session.fuel());
		levelStartCountdown = session.levelStartCountdown();
		levelDisplayAlpha = session.levelDisplayAlpha();
		levelStarting = session.levelStarting();
		ship.setX(session.shipX());
		ship.setY(session.shipY());
		ship.setAngle(session.shipAngle());
		shipLevel.setLevel(session.shipLevel());
		shipLevel.setXP(session.shipXP());
		shipLevel.setXPToNextLevel(session.shipXPToNextLevel());

		noSavedGame = false;
		gameState = "playing";
        gameClock.reset(System.nanoTime());
		menuBackgroundLayer = null;
		menuBackgroundDirty = true;
		requestFocusInWindow();
    }

    void saveGame() {
		saveStore.saveActiveSession(
			highestScore,
			highestDuration,
			new GameSessionSnapshot(
				level,
				score,
				ship.getFuel(),
				levelStartCountdown,
				levelDisplayAlpha,
				levelStarting,
				ship.getX(),
				ship.getY(),
				ship.getAngle(),
				shipLevel.getLevel(),
				shipLevel.getXP(),
				shipLevel.getXPToNextLevel()
			)
		);
		noSavedGame = false;
    }

    private void startDeathSequence() {
		// calculate current session's score & duration
		int currentScore = score;
		int currentDuration = (level - 1) * 33 + (30 * 60 - levelTimer) / 60;

		// compare and set highest
		highestScore = Math.max(highestScore, currentScore);
		highestDuration = Math.max(highestDuration, currentDuration);
		saveStore.clearActiveSession(highestScore, highestDuration);
		noSavedGame = true;

	    deathCountdown = 5 * 60;  // reset to 5 seconds at 60 FPS
	    gameState = "death";  // new game state for death screen
	    bullets.clear();
		voidLasers.clear();
	    asteroids.clear();
	    teleportAnchor.reset();
	    abilityManager.reset();
		cosmicEntities.clear();
		voidCreatures.clear();
		voidHazards.clear();
		blackHoles.clear();
			currentTotalCreatureSizeValue = 0;
			currentTotalEntitySizeValue = 0;
			clearPendingChanges();
			floatingTextManager.clear();
			ship.getTrail().clear();
			galaxyBackground.releaseDisplayLayer();
			spaceGrid.releaseCache();
		    repaint();
	}

	public void updateDimensions() {
		WIDTH = Math.max(1, getWidth());
		HEIGHT = Math.max(1, getHeight());
		menuBackgroundDirty = true;
		
		// Update starfield
		starField.updateDimensions(WIDTH, HEIGHT);
		
		// Update Ship
		ship.updateDimensions(WIDTH, HEIGHT);
		if (level==0 && ship.getVelocity() == 0) {
			ship.setX(WIDTH / 2);
			ship.setY(HEIGHT / 2);
		}

		for (VoidCreature creature : voidCreatures) {
			creature.updateDimensions(WIDTH, HEIGHT);
		}

		// Update Settings Panel
		if (settingsPanel != null) {
            settingsPanel.updateDimensions(WIDTH, HEIGHT);
        }
	}

	private void clearPendingChanges() {
		asteroidsToAdd.clear();
		hazardsToAdd.clear();
		asteroidsToRemove.clear();
		lasersToRemove.clear();
		bulletsToRemove.clear();
		voidHazardsToRemove.clear();
		blackHolesToRemove.clear();
		cosmicEntitiesToRemove.clear();
		creaturesToRemove.clear();
	}

	private void refreshPopulationTotals() {
		currentTotalCreatureSizeValue = 0;
		for (VoidCreature creature : voidCreatures) {
			currentTotalCreatureSizeValue += creature.getSizeValue();
		}
		currentTotalEntitySizeValue = 0;
		for (CosmicEntity entity : cosmicEntities) {
			currentTotalEntitySizeValue += entity.getSizeValue();
		}
	}

	private void applyPendingChanges() {
		voidLasers.removeAll(lasersToRemove);
		bullets.removeAll(bulletsToRemove);
		asteroids.removeAll(asteroidsToRemove);
		asteroids.addAll(asteroidsToAdd);
		voidHazards.removeAll(voidHazardsToRemove);
		blackHoles.removeAll(blackHolesToRemove);
		cosmicEntities.removeAll(cosmicEntitiesToRemove);
		voidCreatures.removeAll(creaturesToRemove);
		refreshPopulationTotals();
		clearPendingChanges();
	}

	private void trySpawnCreatures() {
		if (levelStarting || level == 0) return; // Don't spawn at level 0
		
		// Check if we need to spawn a centipede (always one in void)
		boolean hasCentipede = false;
		for (VoidCreature vc : voidCreatures) {
			if (vc.getType() == VoidCreature.CreatureType.CENTIPEDE) {
				hasCentipede = true;
				break;
			}
		}
		
		// Spawn centipede if not present and in void mode
		if (!hasCentipede && ship.getVoidEnergy().isActive()) {
			VoidCreature centipede = VoidCreature.createCentipede(WIDTH, HEIGHT, level);
			voidCreatures.add(centipede);
		}
		
		if (spawnCreaturesCooldown > 0) { spawnCreaturesCooldown--; return; }
		// Spawn specters based on size limit
		if (currentTotalCreatureSizeValue >= targetTotalCreatureSizeValue) return;
		
		for (int i = 0; i < 2 && currentTotalCreatureSizeValue < targetTotalCreatureSizeValue; i++) {
			VoidCreature vc = VoidCreature.createSpecter(WIDTH, HEIGHT, level);
			voidCreatures.add(vc);
			currentTotalCreatureSizeValue += vc.getSizeValue();
		}

		spawnCreaturesCooldown = 60;
	}

	private void trySpawnEntities() {
		if (spawnEntitiesCooldown > 0) { spawnEntitiesCooldown--; return; }
		if (levelStarting) return;
		if (currentTotalEntitySizeValue >= targetTotalEntitySizeValue) return;

		for (int i = 0; i < 2 && currentTotalEntitySizeValue < targetTotalEntitySizeValue; i++) {
			CosmicEntity vc = new CosmicEntity(WIDTH, HEIGHT, level);
			cosmicEntities.add(vc);
			currentTotalEntitySizeValue += vc.getSizeValue();
		}
		spawnEntitiesCooldown = 60; // cooldown of 60 frames (1 sec) before next spawn
	}

	@Override
	public void actionPerformed(ActionEvent e) {
        int ticks = gameClock.advance(System.nanoTime());
        for (int i = 0; i < ticks; i++) {
            String previousState = gameState;
            updateGameTick();
            if (!gameState.equals(previousState)) {
                gameClock.reset(System.nanoTime());
                break;
            }
        }
        repaint();
    }

    private void updateGameTick() {
        switch (gameState) {
            case "menu" -> {
				if (showingStory) {
					storyTerminal.update();
					if (storyTerminal.isExitRequested()) showingStory = false;
				}
        		return;
            }
            case "playing" -> {
                // Decrement the level timer every tick (frame)
                if (levelStarting) {
                    levelStartCountdown--;
					levelDisplayAlpha = (int) (255 * ((float)levelStartCountdown / 180));
                    if (levelDisplayAlpha <= 0) levelDisplayAlpha = 255;
                    if (levelStartCountdown <= 0) {
                        levelStarting = false;
                        level++;
                        levelStartCountdown = 3 * 60;
                        // Generate more asteroids than before
                        for (int i = 0; i < level + 2; i++) {
                            Asteroid newAsteroid = Asteroid.randomAsteroid(WIDTH, HEIGHT);
							if (level > 3 && level < 10) { newAsteroid.setMultiplier(level * 0.5); }
							asteroids.add(newAsteroid);
                        }

						// Clear Black Holes and Respawn
						blackHoles.clear();

						// Calculate target total size value for this level
						targetTotalEntitySizeValue = Math.min(5 + level * 3, 40);
						targetTotalCreatureSizeValue = Math.min(2 + level * 2, 30);
						refreshPopulationTotals();

						// Spawn Black Holes at level 5+
						// if (level >= 5) {
						if (level >= 0) {
							// Number of black holes increases with level, capped at maxBlackHoles
							// int numBlackHoles = Math.min((level - 4) / 3 + 1, maxBlackHoles);
							int numBlackHoles = Math.min(1, maxBlackHoles);
							
							for (int i = 0; i < numBlackHoles; i++) {
								// Random position avoiding edges
								double bx = 150 + rand.nextDouble() * (WIDTH - 300);
								double by = 150 + rand.nextDouble() * (HEIGHT - 300);
								
								// Random size based on level (60-100)
								int bhSize = 60 + rand.nextInt(20) + Math.min(level, 10) * 2;
								
								blackHoles.add(new BlackHole(bx, by, bhSize));
							}
						}
						// Spawn void hazards 
						// Chance increases with level, max 10%
						if (rand.nextInt(150) < Math.min(level, 10)) {
							int hazardCount = Math.min(Math.min(level, MAX_VOID_HAZARDS),
									MAX_VOID_HAZARDS - voidHazards.size());
							VoidHazard.Type[] types = VoidHazard.Type.values();
							for (int i = 0; i < hazardCount; i++) {
								VoidHazard.Type randomType = types[rand.nextInt(types.length)];
								voidHazards.add(new VoidHazard(WIDTH, HEIGHT, randomType));
							}
						}
                    } 
				} else {
					if (rand.nextInt(100) < level) {  // increase spawn rate with level
						asteroids.add(Asteroid.randomAsteroid(WIDTH, HEIGHT));
					}
					// Now countdown level timer after start countdown finished
					if (levelTimer > 0) {
						levelTimer--;
					} else {
						levelTimer = 30 * 60;  // reset 30 second countdown for next level
						// Show level text for 3 seconds (180 frames)
						levelStarting = true;
					}
				}
				// Update level up animation
				if (showingLevelUp) {
					levelUpFrame++;
					if (levelUpFrame >= 60) {
						showingLevelUp = false;
						levelUpFrame = 0;
					}
				}
				// Galaxy background update
				galaxyBackground.update();
				// Ship trail update
				ship.getTrail().update();

				// ====== Ability Effects ======
				// Update fade timer
				if (fadeActive) {
					fadeTimer--;
					if (fadeTimer <= 0) {
						fadeActive = false;
						fadeTimer = maxFadeTime;
					}
					
					// Dissipate energy while in fade (acts as if in reality)
					if (ship.getVoidEnergy().getEnergy() > 0) {
						ship.getVoidEnergy().dissipateForAction(0.05);
					}
				}
				if (fadeActive && fadeTimer <= 0) {
					fadeActive = false;
					fadeTimer = maxFadeTime;
					fadeCooldownTimer = fadeMaxCooldown;
					abilityManager.startFadeCooldown();
				}

				ship.setFadeMode(fadeActive);

				// Update ability systems
				abilityManager.update();
				teleportAnchor.update();
				floatingTextManager.update();

				// Update cooldowns
				if (voidCooldownTimer > 0) voidCooldownTimer--;
				if (fadeCooldownTimer > 0) fadeCooldownTimer--;

				// Sync ability manager with actual states
				abilityManager.setVoidUnlocked(shipLevel.isAbilityUnlocked("void"));
				abilityManager.setFadeUnlocked(shipLevel.isAbilityUnlocked("fade"));
				abilityManager.setTeleportUnlocked(shipLevel.isAbilityUnlocked("teleport"));
				abilityManager.setShieldUnlocked(shipLevel.isAbilityUnlocked("shield"));
				abilityManager.setTimeSlowUnlocked(shipLevel.isAbilityUnlocked("timeslow"));
				abilityManager.setDroneUnlocked(shipLevel.isAbilityUnlocked("drone"));
				abilityManager.setNovaUnlocked(shipLevel.isAbilityUnlocked("nova"));
				
				abilityManager.setVoidActive(ship.getVoidEnergy().isActive());
				abilityManager.setFadeActive(fadeActive);
				abilityManager.setTeleportActive(teleportAnchor.isAnchorPlaced());

				// Update teleport flash
				if (teleportFlashFrame > 0) teleportFlashFrame--;

				// Update visual effects
                starField.setVoidMode(ship.getVoidEnergy().isActive());
                starField.update(ship.getX() - WIDTH / 2.0, ship.getY() - HEIGHT / 2.0,
                        ship.getVelocity() * Math.sin(ship.getAngle()),
                        -ship.getVelocity() * Math.cos(ship.getAngle()));
                particleSystem.update();
                
                // Update void energy
                ship.getVoidEnergy().update();
                // Check if void energy exceeded threshold
                if (ship.getVoidEnergy().isOverThreshold()) {
                    voidDeath = true;
                    startDeathSequence();
                    return;
                }
                // Deactivate void mode if energy depleted
                if (ship.getVoidEnergy().getEnergy() <= 0) { abilityManager.setVoidActive(false); }

                // Thrust & Rotation
                if (thrusting) ship.applyThrust();
                if (rotatingLeft) ship.rotateLeft();
                if (rotatingRight) ship.rotateRight();
                ship.updatePhysics();
                
                // Asteroid Collision with Ship & Time Slow effect				
				for (Asteroid a : asteroids) {
					if (asteroidsToRemove.contains(a)) continue;
					a.setSlowedVelocity(abilityManager.getTimeScale());
					a.toggleTimeSlow(abilityManager.isTimeSlowActive());
				}
				
				// Shield destroys asteroids
				if (abilityManager.isShieldActive()) {
					for (Asteroid asteroid : asteroids) {
						if (asteroidsToRemove.contains(asteroid)) continue;
						double dx = asteroid.getBoundingBox().getCenterX() - ship.getX();
						double dy = asteroid.getBoundingBox().getCenterY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						
						if (dist < abilityManager.getShieldRadius()) {
							// Animation and particle effect
							asteroid.triggerHitFlash();
							particleSystem.createAsteroidExplosion(
								asteroid.getBoundingBox().getCenterX(),
								asteroid.getBoundingBox().getCenterY(),
								asteroid.getSize()
							);
							
							// Award full points and resources
							scoreGain = 35;
							xpGain = 18;
							fuelGain = 35;
							score += scoreGain;
							int oldLevel = shipLevel.getLevel();
							shipLevel.addXP(xpGain);
							ship.addFuel(fuelGain);
							// Show floating text at collision point
							Rectangle asteroidBounds = asteroid.getBoundingBox();
							double centerX = asteroidBounds.getCenterX();
							double centerY = asteroidBounds.getCenterY();
							
							floatingTextManager.add(FloatingTextManager.createFuel(fuelGain, centerX, centerY + 15));
							
							// Show XP gain near ship
							floatingTextManager.add(FloatingTextManager.createXP(xpGain, ship.getX() + 30, ship.getY() - 20));
									

							// Check for level up
							if (shipLevel.getLevel() > oldLevel) {
								showingLevelUp = true;
								levelUpFrame = 0;
								
								// Create level up particle effect
								particleSystem.createExplosion(
									ship.getX(), ship.getY(),
									shipLevel.getLevelColor(),
									100, 8.0
								);
							}

							asteroidsToRemove.add(asteroid);
						}
					}
					
					// Damage void creatures
					for (VoidCreature vc : voidCreatures) {
						if (creaturesToRemove.contains(vc)) continue;
						if (vc.isDead()) { creaturesToRemove.add(vc); continue; }
						double dx = vc.getX() - ship.getX();
						double dy = vc.getY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						
						if (ship.getVoidEnergy().isActive() && dist < abilityManager.getShieldRadius()) {
							vc.takeDamage(100); // Shield does 100 damage
							if (vc.isDead()) {
								particleSystem.createVoidCreatureExplosion(vc.getX(), vc.getY());
								creaturesToRemove.add(vc);
							}
						}
					}
					
					// Damage cosmic entities
					for (CosmicEntity ce : cosmicEntities) {
						if (cosmicEntitiesToRemove.contains(ce)) continue;
						if (ce.isDead()) { cosmicEntitiesToRemove.add(ce); continue; }
						double dx = ce.getX() - ship.getX();
						double dy = ce.getY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						
						if (dist < abilityManager.getShieldRadius()) {
							ce.takeDamage(100);
							if (ce.isDead()) {
								particleSystem.createCosmicEntityExplosion(ce.getX(), ce.getY());
								cosmicEntitiesToRemove.add(ce);
							}
						}
					}
				}

				// Drone auto-fire
				if (abilityManager.isDroneDeployed() && abilityManager.canDroneFire()) {
					Point dronePos = abilityManager.getDronePosition(ship.getX(), ship.getY());
					if (dronePos != null) {
						// Find nearest asteroid within 200 pixels
						Asteroid nearest = null;
						double nearestDist = 200;
						
						for (Asteroid a : asteroids) {
							if (asteroidsToRemove.contains(a)) continue;
							double dx = a.getBoundingBox().getCenterX() - dronePos.x;
							double dy = a.getBoundingBox().getCenterY() - dronePos.y;
							double dist = Math.sqrt(dx * dx + dy * dy);
							
							if (dist < nearestDist) {
								nearest = a;
								nearestDist = dist;
							}
						}
						
						if (nearest != null) {
							// Calculate angle to target
							double dx = nearest.getBoundingBox().getCenterX() - dronePos.x;
							double dy = nearest.getBoundingBox().getCenterY() - dronePos.y;
							double angleToTarget = Math.atan2(dx, -dy);
							
							// Fire bullet from drone
							bullets.add(new Bullet(dronePos.x + 30, dronePos.y + 30, angleToTarget));
							abilityManager.droneDidFire();
						}
					}
				}

				// ====== Collisions & Physics ======
				// Spawn creatures and entities to meet target total size value
				if (!levelStarting) trySpawnCreatures();
				if (!levelStarting) trySpawnEntities();

				// Update void hazards
				Iterator<VoidHazard> vhIt = voidHazards.iterator();
				while (vhIt.hasNext()) {
					VoidHazard vh = vhIt.next();
					vh.update();

					if (!vh.isAlive()) {
						vhIt.remove();

						// Keep the total stable without preserving an oversized set from an older run.
						if (voidHazards.size() + hazardsToAdd.size() < MAX_VOID_HAZARDS) {
							VoidHazard.Type[] types = VoidHazard.Type.values();
							hazardsToAdd.add(new VoidHazard(WIDTH, HEIGHT, types[rand.nextInt(types.length)]));
						}
					} else if (!fadeActive && ship.getVoidEnergy().isActive() && vh.intersects(ship.getBounds())) {
						voidHazardDeath = true;
						startDeathSequence();
						return;
					}
				}

				// Apply additions after iteration ends
				if (!hazardsToAdd.isEmpty()) {
					voidHazards.addAll(hazardsToAdd);
					hazardsToAdd.clear();
				}
								
				// Update void creatures 
				for (VoidCreature vc : voidCreatures) {
					if (creaturesToRemove.contains(vc)) continue;
					if (vc.isDead()) { creaturesToRemove.add(vc); continue; }
					vc.update(ship.getX(), ship.getY(), ship.getVoidEnergy().isActive());
					
					if (vc.getType() == VoidCreature.CreatureType.CENTIPEDE) {
						if (!ship.getVoidEnergy().isActive()) vc.startExitingVoid();
						if (vc.isExitingVoid() && vc.isFullyOffscreen()) {
							creaturesToRemove.add(vc);
							continue;
						}
					}

					if (!fadeActive && ship.getVoidEnergy().isActive() && vc.intersects(ship.getBounds())) {
						voidCreatureDeath = true;
						startDeathSequence();
						return;
					}
				}

				// Update cosmic entities
				for (CosmicEntity ce : cosmicEntities) {
					if (cosmicEntitiesToRemove.contains(ce)) continue;
					if (ce.isDead()) { cosmicEntitiesToRemove.add(ce); continue; }
					ce.update(ship.getX(), ship.getY(), ship.getVoidEnergy().isActive());
					
					if (!fadeActive && !ship.getVoidEnergy().isActive() && ce.isNearShip(ship.getBounds())) {
						cosmicEntityDeath = true;
						startDeathSequence();
						return;
					}
				}

				// Update black holes
				for (BlackHole bh : blackHoles) {
					bh.update();
					
					// Apply gravity to ship
					Point2D.Double gravity = bh.applyGravity(ship.getX(), ship.getY());
					ship.setX(ship.getX() + gravity.x);
					ship.setY(ship.getY() + gravity.y);
					
					// Check if consumed
					if (!fadeActive && bh.isShipConsumed(ship.getX(), ship.getY())) {
						blackHoleDeath = true;
						startDeathSequence();
						return;
					}
				}
				// ------ Asteroid Physics & Collision with Ship ------
				Polygon droneBounds = abilityManager.isDroneDeployed()
						? abilityManager.getDroneBounds(ship.getX(), ship.getY()) : null;
				for (Asteroid asteroid : asteroids) {
					if (asteroidsToRemove.contains(asteroid)) continue;
					asteroid.update();
					if (asteroid.hasExited(WIDTH, HEIGHT)) {
						asteroidsToRemove.add(asteroid);
						continue;
					}
					if (!fadeActive && !ship.getVoidEnergy().isActive()) {
						Rectangle aBounds = asteroid.getBoundingBox();
						double quickDist = Math.abs(aBounds.getCenterX() - ship.getX()) 
										+ Math.abs(aBounds.getCenterY() - ship.getY());
						
						if (quickDist < 100) { // Only do precise check if nearby
							if (asteroid.intersects(ship.getBounds())) {
								asteroid.triggerHitFlash();
								asteroidDeath = true;
								startDeathSequence();
								return;
							}
						}
					}
					// Drone hit by Asteroid
					if (abilityManager.isDroneDeployed()
							&& asteroid.intersects(droneBounds)) {
	                	abilityManager.damageDrone();
	                	asteroid.triggerHitFlash();
	                	asteroidsToRemove.add(asteroid);
	                	
	                	// Add particle effect
                        particleSystem.createAsteroidExplosion(
								asteroid.getBoundingBox().getCenterX(),
                            asteroid.getBoundingBox().getCenterY(),
                            asteroid.getSize()
                        );
                        particleSystem.createDroneExplosion(
	                		droneBounds.getBounds().getCenterX(),
	                		droneBounds.getBounds().getCenterY()
	                	);
	                	/*
	                	// Handle asteroid destruction
						if (asteroid.getSize() == Asteroid.Size.SMALL) {
							asteroidsToRemove.add(asteroid);
						} else {
							asteroidsToRemove.add(asteroid);
							asteroidsToAdd.addAll(asteroid.split());
						}
						*/
						
						switch (asteroid.getSize()) {
							case LARGE -> {
								xpGain = 10;
								fuelGain = 20;
								scoreGain = 20;
								largeDestroyed++;
							}
							case MEDIUM -> {
								xpGain = 5;
								fuelGain = 10;
								scoreGain = 10;
								mediumDestroyed++;
							}
							case SMALL -> {
								xpGain = 3;
								fuelGain = 5;
								scoreGain = 5;
								smallDestroyed++;
							}
						}
						// Track old level
						int oldLevel = shipLevel.getLevel();
							
						// Award rewards
						score += scoreGain;
						shipLevel.addXP(xpGain);
						ship.addFuel(fuelGain);
						
						// Show floating text at collision point
						Rectangle asteroidBounds = asteroid.getBoundingBox();
						double centerX = asteroidBounds.getCenterX();
						double centerY = asteroidBounds.getCenterY();
						
						floatingTextManager.add(FloatingTextManager.createFuel(fuelGain, centerX, centerY + 15));
						
						// Show XP gain near ship
						floatingTextManager.add(FloatingTextManager.createXP(xpGain, ship.getX() + 30, ship.getY() - 20));
								

						// Check for level up
						if (shipLevel.getLevel() > oldLevel) {
							showingLevelUp = true;
							levelUpFrame = 0;
							
							// Create level up particle effect
							particleSystem.createExplosion(
								ship.getX(), ship.getY(),
								shipLevel.getLevelColor(),
								100, 8.0
							);
						}
	                }
				}
				
                // ------ Void Laser Collisions ------
                for (VoidLaser laser : voidLasers) {
                    laser.update();
                    if (!laser.isAlive()) {
                        lasersToRemove.add(laser);
                        continue;
                    }
                    // Drone hit by laser
	                if (abilityManager.isDroneDeployed() && laser.intersects(droneBounds)) {
	                	abilityManager.damageDrone();
	                }

					// Damage void creatures with lasers
					for (VoidCreature vc : voidCreatures) {
						if (creaturesToRemove.contains(vc)) continue;
						if (vc.isDead()) { creaturesToRemove.add(vc); continue; }
						if (vc.intersects(laser.getLaserLine())) {
							vc.takeDamage(50); // Lasers do 50 damage
							if (vc.isDead()) {
								particleSystem.createVoidCreatureExplosion(vc.getX(), vc.getY());
								creaturesToRemove.add(vc);
							}
							lasersToRemove.add(laser);
							break;
						}
					}

					// Damage cosmic entities with lasers
					for (CosmicEntity ce : cosmicEntities) {
						if (cosmicEntitiesToRemove.contains(ce)) continue;
						if (ce.isDead()) { cosmicEntitiesToRemove.add(ce); continue; }
						if (laser.getLaserLine().intersects((int)ce.getX() - 50, (int)ce.getY() - 50, 100, 100)) {
							ce.takeDamage(50);
							if (ce.isDead()) {
								particleSystem.createCosmicEntityExplosion(ce.getX(), ce.getY());
								cosmicEntitiesToRemove.add(ce);
							}
							lasersToRemove.add(laser);
							break;
						}
					}
                    
					if (!ship.getVoidEnergy().isActive()) {
						for (Asteroid asteroid : asteroids) {
							if (asteroidsToRemove.contains(asteroid)) continue;
							if (laser.intersects(asteroid.getBounds())) {
								asteroid.triggerHitFlash();
								// Add particle effect
								particleSystem.createLaserExplosion(
										asteroid.getBoundingBox().getCenterX(),
										asteroid.getBoundingBox().getCenterY(),
										asteroid.getSize()
								);
								
								// Handle asteroid destruction
								if (asteroid.getSize() == Asteroid.Size.SMALL) {
									asteroidsToRemove.add(asteroid);
								} else {
									asteroidsToRemove.add(asteroid);
									asteroidsToAdd.addAll(asteroid.split());
								}
								
								switch (asteroid.getSize()) {
									case LARGE -> {
										xpGain = 10;
										fuelGain = 20;
										scoreGain = 20;
										largeDestroyed++;
									}
									case MEDIUM -> {
										xpGain = 5;
										fuelGain = 10;
										scoreGain = 10;
										mediumDestroyed++;
									}
									case SMALL -> {
										xpGain = 3;
										fuelGain = 5;
										scoreGain = 5;
										smallDestroyed++;
									}
								}

								// Track old level
								int oldLevel = shipLevel.getLevel();

								// Award rewards
								score += scoreGain;
								shipLevel.addXP(xpGain);
								ship.addFuel(fuelGain);

								// Show floating text at collision point
								Rectangle asteroidBounds = asteroid.getBoundingBox();
								double centerX = asteroidBounds.getCenterX();
								double centerY = asteroidBounds.getCenterY();

								floatingTextManager.add(FloatingTextManager.createFuel(fuelGain, centerX, centerY + 15));

								// Show XP gain near ship
								floatingTextManager.add(FloatingTextManager.createXP(xpGain, ship.getX() + 30, ship.getY() - 20));

								// Check for level up
								if (shipLevel.getLevel() > oldLevel) {
									showingLevelUp = true;
									levelUpFrame = 0;
									
									// Create level up particle effect
									particleSystem.createExplosion(
										ship.getX(), ship.getY(),
										shipLevel.getLevelColor(),
										100, 8.0
									);
								}
							}
						}
					}
                }
                
                // ------ Bullet Collision ------
				// Update bullets and check for bullet collisions
                for (Bullet bullet : bullets) {
                    bullet.update(WIDTH, HEIGHT);
                    if (!bullet.isAlive()) {
                        bulletsToRemove.add(bullet);
                        continue;
                    }
                    /*
					// Ship hit by bullet
	                if (!fadeActive && !ship.getVoidEnergy().isActive() 
	                	&& bullet.getBounds().intersects(ship.getBounds().getBounds2D())) {
	                    bulletDeath = true;
	                    startDeathSequence();
	                    return;
	                }*/
	                
	                // Drone hit by bullet
	                if (abilityManager.isDroneDeployed()
							&& bullet.getBounds().intersects(droneBounds.getBounds2D())) {
	                	abilityManager.damageDrone();
	                	bulletsToRemove.add(bullet);
					continue;
	                }

					// Damage void creatures with bullets
					for (VoidCreature vc : voidCreatures) {
						if (creaturesToRemove.contains(vc)) continue;
						if (vc.isDead()) { creaturesToRemove.add(vc); continue; }
						if (vc.intersects(bullet.getBoundingBox())) {
							vc.takeDamage(25);
							if (vc.isDead()) {
								particleSystem.createVoidCreatureExplosion(vc.getX(), vc.getY());
								creaturesToRemove.add(vc);
							}
							bulletsToRemove.add(bullet);
							break;
						}
					}

					if (bulletsToRemove.contains(bullet)) continue;

					// Damage cosmic entities with bullets
					for (CosmicEntity ce : cosmicEntities) {
						if (cosmicEntitiesToRemove.contains(ce)) continue;
						if (ce.isDead()) { cosmicEntitiesToRemove.add(ce); continue; }
						if (bullet.getBoundingBox().intersects((int)ce.getX() - 50, (int)ce.getY() - 50, 100, 100)) {
							ce.takeDamage(25);
							if (ce.isDead()) {
								particleSystem.createCosmicEntityExplosion(ce.getX(), ce.getY());
								cosmicEntitiesToRemove.add(ce);
							}
							bulletsToRemove.add(bullet);
							break;
						}
					}

                    
                    if (bulletsToRemove.contains(bullet)) continue;
                    for (Asteroid asteroid : asteroids) {
						if (asteroidsToRemove.contains(asteroid)) continue;
                        if (asteroid.getBounds().intersects(bullet.getBoundingBox())) {
                            bulletsToRemove.add(bullet);
							asteroid.triggerHitFlash();
                            
                            // Add particle effect
                            particleSystem.createAsteroidExplosion(
                                    asteroid.getBoundingBox().getCenterX(),
                                    asteroid.getBoundingBox().getCenterY(),
                                    asteroid.getSize()
                            );
                            
                            // Handle asteroid destruction
                            if (asteroid.getSize() == Asteroid.Size.SMALL) {
                                asteroidsToRemove.add(asteroid);
                            } else {
                                asteroidsToRemove.add(asteroid);
                                asteroidsToAdd.addAll(asteroid.split());
                            }
                            switch (asteroid.getSize()) {
								case LARGE -> {
									xpGain = 10;
									fuelGain = 20;
									scoreGain = 20;
									largeDestroyed++;
								}
								case MEDIUM -> {
									xpGain = 5;
									fuelGain = 10;
									scoreGain = 10;
									mediumDestroyed++;
								}
								case SMALL -> {
									xpGain = 3;
									fuelGain = 5;
									scoreGain = 5;
									smallDestroyed++;
								}
							}
                            // Track old level
							int oldLevel = shipLevel.getLevel();

							// Award rewards
							score += scoreGain;
							shipLevel.addXP(xpGain);
							ship.addFuel(fuelGain);

							// Show floating text at collision point
							Rectangle asteroidBounds = asteroid.getBoundingBox();
							double centerX = asteroidBounds.getCenterX();
							double centerY = asteroidBounds.getCenterY();

							floatingTextManager.add(FloatingTextManager.createFuel(fuelGain, centerX, centerY + 15));

							// Show XP gain near ship
							floatingTextManager.add(FloatingTextManager.createXP(xpGain, ship.getX() + 30, ship.getY() - 20));
								

							// Check for level up
							if (shipLevel.getLevel() > oldLevel) {
								showingLevelUp = true;
								levelUpFrame = 0;
									
								// Create level up particle effect
								particleSystem.createExplosion(
									ship.getX(), ship.getY(),
									shipLevel.getLevelColor(),
									100, 8.0
								);
							}
                            break;
                        }
                    }
                }
					
				// ------ Nova Blast Collisions ------
				// Nova Blast Destroying Asteroids
				if (abilityManager.isNovaExploding()) {
					double novaRadius = abilityManager.getNovaExplosionRadius();
					for (Asteroid asteroid : asteroids) {
						if (asteroidsToRemove.contains(asteroid)) continue;
						double dx = asteroid.getBoundingBox().getCenterX() - ship.getX();
						double dy = asteroid.getBoundingBox().getCenterY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						if (dist < novaRadius) {
							particleSystem.createAsteroidExplosion(
								asteroid.getBoundingBox().getCenterX(),
								asteroid.getBoundingBox().getCenterY(),
								asteroid.getSize()
							);
								
							switch (asteroid.getSize()) {
								case LARGE -> { scoreGain = 20; xpGain = 10; fuelGain = 20; largeDestroyed++; }
								case MEDIUM -> { scoreGain = 10; xpGain = 5; fuelGain = 10; mediumDestroyed++; }
								case SMALL -> { scoreGain = 5; xpGain = 3; fuelGain = 5; smallDestroyed++; }
							}
							score += scoreGain;
							shipLevel.addXP(xpGain);
							ship.addFuel(fuelGain);
								
							// Track old level
							int oldLevel = shipLevel.getLevel();
							
							// Show floating text at collision point
							Rectangle asteroidBounds = asteroid.getBoundingBox();
							double centerX = asteroidBounds.getCenterX();
							double centerY = asteroidBounds.getCenterY();

							floatingTextManager.add(FloatingTextManager.createFuel(fuelGain, centerX, centerY + 15));

							// Show XP gain near ship
							floatingTextManager.add(FloatingTextManager.createXP(xpGain, ship.getX() + 30, ship.getY() - 20));
							
							// Check for level up
							if (shipLevel.getLevel() > oldLevel) {
								showingLevelUp = true;
								levelUpFrame = 0;
									
								// Create level up particle effect
								particleSystem.createExplosion(
									ship.getX(), ship.getY(),
									shipLevel.getLevelColor(),
									100, 8.0
								);
							}
							asteroid.triggerHitFlash();
							asteroidsToRemove.add(asteroid);
						}
					}
					
					// Nova Blast Destroying Bullets
					for (Bullet b : bullets) {
						if (bulletsToRemove.contains(b)) continue;
						double dx = b.getBounds().getBounds().getCenterX() - ship.getX();
						double dy = b.getBounds().getBounds().getCenterY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						if (dist < novaRadius) {
							particleSystem.createBulletExplosion(
								b.getBounds().getBounds().getCenterX(),
								b.getBounds().getBounds().getCenterY()
							);
							bulletsToRemove.add(b);
						}
					}
					
					// Nova Blast Destroying Void Hazards
					for (VoidHazard v : voidHazards) {
						double dx = v.getCenterX() - ship.getX();
						double dy = v.getCenterY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						if (dist < novaRadius) {
							particleSystem.createVoidExplosion(
								v.getCenterX(),
								v.getCenterY()
							);
							voidHazardsToRemove.add(v);
						}
					}

					// Nova Blast Destroying Void Creatures
					for (VoidCreature vc : voidCreatures) {
						if (creaturesToRemove.contains(vc)) continue;
						if (vc.isDead()) { creaturesToRemove.add(vc); continue; }
						double dx = vc.getX() - ship.getX();
						double dy = vc.getY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						if (dist < novaRadius) {
							particleSystem.createVoidCreatureExplosion(vc.getX(), vc.getY());
							creaturesToRemove.add(vc);
						}
					}
					
					// Nova Blast Destroying Black Holes
					for (BlackHole bh : blackHoles) {
						double dx = bh.getX() - ship.getX();
						double dy = bh.getY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						if (dist < novaRadius) {
							particleSystem.createBlackHoleExplosion(
								bh.getX(),
								bh.getY()
							);
							blackHolesToRemove.add(bh);
						}
					}
					
					// Nova Blast Destroying Cosmic Entities
					for (CosmicEntity ce : cosmicEntities) {
						if (cosmicEntitiesToRemove.contains(ce)) continue;
						if (ce.isDead()) { cosmicEntitiesToRemove.add(ce); continue; }
						double dx = ce.getX() - ship.getX();
						double dy = ce.getY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						if (dist < novaRadius) {
							particleSystem.createCosmicEntityExplosion(
								ce.getX(),
								ce.getY()
							);
							cosmicEntitiesToRemove.add(ce);
						}
					}
					
					// Consume void energy
					if (abilityManager.getNovaExplosionRadius() > 50) { // First frame
						ship.getVoidEnergy().reset();
					}
				}
				// Check Fuel
				if (ship.getFuel() > 1000) {
					ship.setFuel(1000);
				}
                
				applyPendingChanges();

            	
            }
            case "death" -> {
                deathCountdown--;
                if (deathCountdown <= 0) {
                    gameState = "menu";
                    setupMenu();
                }
                // Death explosion animation
                if (!deathExplosionDone && deathExplosionFrame < 60) {
                    if (deathExplosionFrame % 5 == 0) {
                        particleSystem.createShipExplosion(ship.getX(), ship.getY());
                    }
                    deathExplosionFrame++;
                } else {
                    deathExplosionDone = true;
                }
                particleSystem.update();
            }
            default -> {
            }
        }
	}
	
	@Override
	protected void paintComponent(Graphics g) {
	    super.paintComponent(g);
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
	    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
	    g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
	
        switch (gameState) {
            case "menu" -> {
				drawMenuBackground(g2);
				
				if (showingStory) {
					// Draw story terminal (full screen)
					storyTerminal.draw(g2, WIDTH, HEIGHT);
				} else if (showingSettings) {
					settingsPanel.draw((Graphics2D)g, controlConfig, abilityLoadout);
				} else {
					// Buttons
					drawNeonMenuButton(g2, "NEW GAME", HEIGHT/2 - 20, hoveredButton == 0, true);
					drawNeonMenuButton(g2, "RESUME", HEIGHT/2 + 50, hoveredButton == 1, !noSavedGame);
					drawNeonMenuButton(g2, "SETTINGS", HEIGHT/2 + 120, hoveredButton == 2, true);
					drawNeonMenuButton(g2, "STORY", HEIGHT/2 + 190, hoveredButton == 3, true);
					
					// Error message
					if (noSavedGame) {
						g2.setFont(new Font("Courier New", Font.PLAIN, 24));
						int pulse = (int)(200 + 55 * Math.sin(System.currentTimeMillis() / 200.0));
						
						// Neon glow effect
						g2.setColor(new Color(255, 0, 100, pulse / 3));
						String errorMessage = "⚠ NO SAVED PROGRESS FOUND";
						int errorX = WIDTH / 2 - g2.getFontMetrics().stringWidth(errorMessage) / 2;
						g2.drawString(errorMessage, errorX - 2, HEIGHT/2 - 50 - 2);
						g2.drawString(errorMessage, errorX + 2, HEIGHT/2 - 50 + 2);
						
						g2.setColor(new Color(0, 255, 255, pulse));
						g2.drawString(errorMessage, errorX, HEIGHT/2 - 50);
					}
				}
			}
            case "playing" -> {
                    // GalaxyBackground paints the full background, including its
                    // gradient; a separate full-screen gradient here was overwritten.
                    galaxyBackground.draw(g2, WIDTH, HEIGHT);
				
				// Space grid
				spaceGrid.draw(g2, WIDTH, HEIGHT, asteroids, blackHoles);
				
				// Ship trail (before ship)
				ship.getTrail().draw(g2);
				
				// Draw screen overlays (simulate ship's HUD/window)
				if (fadeActive) {
					drawFadeScreenOverlay(g2);
				} else if (ship.getVoidEnergy().isActive()) {
					drawVoidScreenOverlay(g2);
				}
				
				// Draw void hazards
				for (VoidHazard vh : voidHazards) { vh.draw(g2, ship.getVoidEnergy().isActive()); }

				// Draw void creatures
				for (VoidCreature vc : voidCreatures) { vc.draw(g2, ship.getVoidEnergy().isActive()); }

				// Draw black holes
				for (BlackHole bh : blackHoles) { bh.draw(g2); }

				// Draw cosmic entities
				for (CosmicEntity ce : cosmicEntities) { ce.draw(g2, ship.getVoidEnergy().isActive()); }
				
				// Draw starfield
				starField.draw(g2);
				
				// Draw particles
				particleSystem.draw(g2);

				//Draw teleport anchor
				teleportAnchor.drawAnchor(g2);

				// Draw shield effect
				abilityManager.drawShieldEffect(g2, ship.getX(), ship.getY());

				// Draw time slow effect
				abilityManager.drawTimeSlowEffect(g2, WIDTH, HEIGHT);

				// Draw drone
				abilityManager.drawDrone(g2, ship.getX(), ship.getY());

				// Draw nova blast
				abilityManager.drawNovaBlast(g2, WIDTH, HEIGHT, ship.getX(), ship.getY());

				// Draw floating texts
				floatingTextManager.draw(g2);

				// Draw ability bar
				abilityManager.drawAbilityBar(g2, WIDTH, HEIGHT, abilityLoadout);

				// Draw level UI
				shipLevel.drawLevelUI(g2, WIDTH, HEIGHT);

				// Draw teleport flash
				if (teleportFlashFrame > 0) { teleportAnchor.drawTeleportFlash(g2, WIDTH, HEIGHT, teleportFlashFrame); }

				// Draw level up effect
				if (showingLevelUp) { shipLevel.drawLevelUpEffect(g2, (int) ship.getX(), (int) ship.getY(), WIDTH, HEIGHT, levelUpFrame); }

				// Draw asteroids with enhanced effects
				if (ship.getVoidEnergy().isActive()) {
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
				}
				for (Asteroid a : asteroids) { 
					a.draw(g2);
					
					// Add glow to asteroids in void mode
					if (ship.getVoidEnergy().isActive()) {
						Rectangle bounds = a.getBoundingBox();
						int glowSize = bounds.width + 10;
						g2.setColor(new Color(100, 0, 150, 30));
						g2.fillOval(
							bounds.x + bounds.width / 2 - glowSize / 2,
							bounds.y + bounds.height / 2 - glowSize / 2,
							glowSize, glowSize
						);
					}
				}
				if (ship.getVoidEnergy().isActive()) {
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}

				// Draw ship
				ship.draw(g);

				// Draw bullets
				if (ship.getVoidEnergy().isActive()) {
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
				}
				for (Bullet b : bullets) { 
					b.draw(g2); 
				}
				if (ship.getVoidEnergy().isActive()) {
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}

				// Draw void lasers
				for (VoidLaser laser : voidLasers) {
					laser.draw(g2);
				}
				
				// Enhanced HUD
				drawEnhancedHUD(g2);
				
				// Level starting countdown
				if (levelStarting) {
					drawLevelTransition(g2);
				}
				
				// Animated void overlay effect
				if (ship.getVoidEnergy().isActive()) {
					// Animated void overlay with wave effect
					long time = System.currentTimeMillis();
					for (int i = 0; i < HEIGHT; i += 10) {
						int waveOffset = (int) (Math.sin((i + time / 50.0) / 20.0) * 5);
						int alpha = 50 + (int) (Math.sin((i + time / 100.0) / 15.0) * 20);
						
						// Clamp alpha to valid range
						alpha = Math.max(0, Math.min(255, alpha));
						
						g2.setColor(new Color(80, 0, 120, alpha));
						g2.fillRect(0, i + waveOffset, WIDTH, 10);
					}
				}
            }
            case "paused" -> {
                // Dark overlay
				g2.setColor(new Color(0, 0, 0, 200));
				g2.fillRect(0, 0, WIDTH, HEIGHT);
				
				// Centered panel
				int panelWidth = 400;
				int panelHeight = 300;
				int panelX = WIDTH / 2 - panelWidth / 2;
				int panelY = HEIGHT / 2 - panelHeight / 2;
				
				g2.setColor(new Color(20, 25, 40, 250));
				g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);
				
				g2.setColor(new Color(100, 150, 255));
				g2.setStroke(new BasicStroke(2));
				g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);
				g2.setStroke(new BasicStroke(1));
				
				// Title
				Font pausedFont = new Font("Arial", Font.BOLD, 40);
				g.setFont(pausedFont);
				g.setColor(new Color(150, 200, 255));
				String pausedText = "PAUSED";
				FontMetrics pfm = g.getFontMetrics();
				g.drawString(pausedText, WIDTH / 2 - pfm.stringWidth(pausedText) / 2, panelY + 80);
				
				// Stats
				g2.setFont(new Font("Arial", Font.PLAIN, 16));
				g2.setColor(new Color(200, 200, 220));
				int statsY = panelY + 140;
				
				String[] stats = {
					"Level: " + level,
					"Score: " + score,
					"Fuel: " + (int)ship.getFuel(),
					"Ship Level: " + shipLevel.getLevel()
				};
				
				for (String stat : stats) {
					FontMetrics fm = g2.getFontMetrics();
					g2.drawString(stat, WIDTH / 2 - fm.stringWidth(stat) / 2, statsY);
					statsY += 25;
				}
				
				// Resume instruction
				Font resumeFont = new Font("Arial", Font.PLAIN, 15);
				g.setFont(resumeFont);
				g.setColor(new Color(150, 200, 255));
				String resumeText = "Press ESC to Resume";
				FontMetrics metrics = g.getFontMetrics(resumeFont);
				g.drawString(resumeText, WIDTH / 2 - metrics.stringWidth(resumeText) / 2, panelY + panelHeight - 40);
				String menuText = "Press ENTER to Return to Menu";
				FontMetrics menuMetrics = g.getFontMetrics(resumeFont);
				g.drawString(menuText, WIDTH / 2 - menuMetrics.stringWidth(menuText) / 2, panelY + panelHeight - 15);
            }
            case "death" -> {
				// Dark gradient background
				drawGradientBackground(g2, 
					new Color(20, 5, 10), 
					new Color(40, 10, 20)
				);
				
				// Draw particles (explosion debris)
				particleSystem.draw(g2);
				
				// Flashing red overlay for first second
				if (deathCountdown > 4 * 60) {
					int flashAlpha = (int) (100 * Math.sin((5 * 60 - deathCountdown) * 0.5));
					// Clamp alpha to valid range
					flashAlpha = Math.max(0, Math.min(255, flashAlpha));
					g2.setColor(new Color(255, 0, 0, flashAlpha));
					g2.fillRect(0, 0, WIDTH, HEIGHT);
				}
				
				// "YOU DIED" with dramatic effect
				g2.setFont(new Font("Serif", Font.BOLD, 90));
				FontMetrics deathFm = g2.getFontMetrics();
				String deathText = "YOU DIED";
				int deathTextX = WIDTH / 2 - deathFm.stringWidth(deathText) / 2;
				int deathTextY = HEIGHT / 4;
				
				// Glitch effect for first 2 seconds
				if (deathCountdown > 3 * 60) {
					int glitchOffset = rand.nextInt(10) - 5;
					g2.setColor(new Color(255, 0, 0, 100));
					g2.drawString(deathText, deathTextX + glitchOffset, deathTextY);
					g2.setColor(new Color(0, 255, 255, 100));
					g2.drawString(deathText, deathTextX - glitchOffset, deathTextY);
				}
				
				// Main death text with shadow
				g2.setColor(new Color(100, 0, 0));
				g2.drawString(deathText, deathTextX + 4, deathTextY + 4);
				g2.setColor(new Color(255, 50, 50));
				g2.drawString(deathText, deathTextX, deathTextY);
				
				// Death cause with color coding
				g2.setFont(new Font("Serif", Font.PLAIN, 28));
				FontMetrics causeFm = g2.getFontMetrics();
				
				String deathCause;
				Color causeColor;
				if (voidDeath) {
					deathCause = "CONSUMED BY THE VOID";
					causeColor = new Color(180, 0, 255);
				} else if (bulletDeath) {
					deathCause = "FRIENDLY FIRE";
					causeColor = new Color(255, 200, 0);
				} else if (asteroidDeath) {
					deathCause = "ASTEROID IMPACT";
					causeColor = new Color(150, 150, 150);
				} else if (voidHazardDeath) {
					deathCause = "CONSUMED BY VOID HAZARD";
					causeColor = new Color(180, 0, 255);
				} else if (blackHoleDeath) {
					deathCause = "CONSUMED BY BLACK HOLE";
					causeColor = new Color(0, 0, 0);
				} else if (cosmicEntityDeath) {
					deathCause = "CONSUMED BY COSMIC ENTITY";
					causeColor = new Color(255, 0, 255);
				} else {
					deathCause = "UNKNOWN CAUSE";
					causeColor = new Color(200, 200, 200);
				}
				
				int causeX = WIDTH / 2 - causeFm.stringWidth(deathCause) / 2;
				int causeY = deathTextY + 60;
				
				// Cause with glow
				for (int i = 3; i > 0; i--) {
					g2.setColor(new Color(causeColor.getRed(), causeColor.getGreen(), causeColor.getBlue(), 30));
					g2.drawString(deathCause, causeX - i, causeY - i);
					g2.drawString(deathCause, causeX + i, causeY + i);
				}
				g2.setColor(causeColor);
				g2.drawString(deathCause, causeX, causeY);
				
				// Stats panel with improved styling
				int panelY = HEIGHT / 2 + 20;
				int panelWidth = 500;
				int panelHeight = 250;
				int panelX = WIDTH / 2 - panelWidth / 2;
				
				// Panel background with border
				g2.setColor(new Color(30, 20, 40, 200));
				g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);
				g2.setColor(new Color(150, 100, 200));
				g2.setStroke(new BasicStroke(2));
				g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);
				g2.setStroke(new BasicStroke(1));
				
				// Stats header
				g2.setFont(new Font("Serif", Font.BOLD, 26));
				g2.setColor(new Color(200, 180, 255));
				String statsHeader = "MISSION STATISTICS";
				int headerWidth = g2.getFontMetrics().stringWidth(statsHeader);
				g2.drawString(statsHeader, WIDTH / 2 - headerWidth / 2, panelY + 35);
				
				// Separator line
				g2.setColor(new Color(150, 100, 200));
				g2.drawLine(panelX + 30, panelY + 50, panelX + panelWidth - 30, panelY + 50);
				
				// Stats content
				g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
				int statsStartY = panelY + 80;
				int lineSpacing = 30;
				
				String[][] stats = {
					{"Final Score:", String.valueOf(score), "255, 215, 0"},
					{"Large Destroyed:", String.valueOf(largeDestroyed), "255, 100, 100"},
					{"Medium Destroyed:", String.valueOf(mediumDestroyed), "100, 255, 100"},
					{"Small Destroyed:", String.valueOf(smallDestroyed), "100, 200, 255"},
					{"Fuel Remaining:", String.valueOf((int) ship.getFuel()), "255, 150, 50"}
				};
				
				for (int i = 0; i < stats.length; i++) {
					String label = stats[i][0];
					String value = stats[i][1];
					String[] rgb = stats[i][2].split(", ");
					Color valueColor = new Color(
						Integer.parseInt(rgb[0]), 
						Integer.parseInt(rgb[1]), 
						Integer.parseInt(rgb[2])
					);
					
					int y = statsStartY + i * lineSpacing;
					
					// Label
					g2.setColor(new Color(180, 180, 200));
					g2.drawString(label, panelX + 40, y);
					
					// Value with highlight
					FontMetrics statsFm = g2.getFontMetrics();
					int labelWidth = statsFm.stringWidth(label);
					g2.setColor(valueColor);
					g2.drawString(value, panelX + 40 + labelWidth + 10, y);
				}
				
				// Countdown timer at bottom
				g2.setFont(new Font("Serif", Font.PLAIN, 20));
				int secondsLeft = deathCountdown / 60 + 1;
				String countdown = "Returning in " + secondsLeft + "s (Press ENTER to skip)";
				FontMetrics countdownFm = g2.getFontMetrics();
				
				// Pulsing countdown
				float pulseAlpha = 0.5f + (float) Math.sin(deathCountdown * 0.1) * 0.3f;
				g2.setColor(new Color(200, 200, 200, (int) (255 * pulseAlpha)));
				g2.drawString(countdown, 
					WIDTH / 2 - countdownFm.stringWidth(countdown) / 2, 
					HEIGHT - 60);
            }
            default -> {
            }
        }
	}

	private void drawNeonGridOverlay(Graphics2D g2) {
		g2.setColor(new Color(0, 255, 255, 20));
		g2.setStroke(new BasicStroke(1));
		
		// Vertical lines
		for (int x = 0; x < WIDTH; x += 60) {
			g2.drawLine(x, 0, x, HEIGHT);
		}
		
		// Horizontal lines
		for (int y = 0; y < HEIGHT; y += 60) {
			g2.drawLine(0, y, WIDTH, y);
		}
		
		g2.setStroke(new BasicStroke(1));
	}

	private void drawNeonTitle(Graphics2D g2) {
		g2.setFont(menuTitleFont);
		String title = "ASTEROIDS";
		int titleY = 150;
		FontRenderContext context = g2.getFontRenderContext();
		if (menuTitleOutline == null || menuTitleWidth != WIDTH || !context.equals(menuTitleContext)) {
			menuTitleX = WIDTH / 2 - g2.getFontMetrics().stringWidth(title) / 2;
			menuTitleOutline = menuTitleFont.createGlyphVector(context, title).getOutline(menuTitleX, titleY);
			menuTitleWidth = WIDTH;
			menuTitleContext = context;
		}
		int titleX = menuTitleX;
		
		// Chromatic aberration effect
		g2.setColor(new Color(255, 0, 100, 150));
		g2.drawString(title, titleX - 4, titleY - 4);
		
		g2.setColor(new Color(0, 255, 255, 150));
		g2.drawString(title, titleX + 4, titleY + 4);
		
		g2.setColor(new Color(0, 255, 100, 150));
		g2.drawString(title, titleX - 2, titleY + 2);
		
		// Main title
		g2.setColor(Color.WHITE);
		g2.drawString(title, titleX, titleY);
		
		// Neon glow
		g2.setColor(new Color(0, 255, 255, 100));
		g2.setStroke(new BasicStroke(3));
		g2.draw(menuTitleOutline);
		g2.setStroke(new BasicStroke(1));
		
		// Subtitle
		g2.setFont(menuSubtitleFont);
		g2.setColor(new Color(0, 255, 200));
		String subtitle = "[ NAVIGATE THE VOID ]";
		int subtitleWidth = g2.getFontMetrics().stringWidth(subtitle);
		g2.drawString(subtitle, WIDTH / 2 - subtitleWidth / 2, titleY + 50);
	}

	private void drawNeonMenuButton(Graphics2D g2, String text, int y, boolean hovered, boolean enabled) {
		int buttonWidth = 300;
		int buttonHeight = 50;
		int buttonX = WIDTH / 2 - buttonWidth / 2;
		
		// Corner brackets
		int bracketSize = 15;
		
		if (enabled) {
			// Hover glow
			if (hovered) {
				g2.setColor(new Color(0, 255, 255, 60));
				g2.fillRect(buttonX - 5, y - 5, buttonWidth + 10, buttonHeight + 10);
			}
			
			// Button background (semi-transparent)
			g2.setColor(new Color(0, 20, 40, hovered ? 180 : 120));
			g2.fillRect(buttonX, y, buttonWidth, buttonHeight);
			
			// Neon border
			g2.setColor(hovered ? new Color(0, 255, 255) : new Color(0, 200, 200));
			g2.setStroke(new BasicStroke(hovered ? 3 : 2));
			
			// Corner brackets instead of full rectangle
			// Top-left
			g2.drawLine(buttonX, y, buttonX + bracketSize, y);
			g2.drawLine(buttonX, y, buttonX, y + bracketSize);
			
			// Top-right
			g2.drawLine(buttonX + buttonWidth - bracketSize, y, buttonX + buttonWidth, y);
			g2.drawLine(buttonX + buttonWidth, y, buttonX + buttonWidth, y + bracketSize);
			
			// Bottom-left
			g2.drawLine(buttonX, y + buttonHeight - bracketSize, buttonX, y + buttonHeight);
			g2.drawLine(buttonX, y + buttonHeight, buttonX + bracketSize, y + buttonHeight);
			
			// Bottom-right
			g2.drawLine(buttonX + buttonWidth - bracketSize, y + buttonHeight, buttonX + buttonWidth, y + buttonHeight);
			g2.drawLine(buttonX + buttonWidth, y + buttonHeight - bracketSize, buttonX + buttonWidth, y + buttonHeight);
			
			// Text with glow
			g2.setFont(menuButtonFont);
			FontMetrics fm = g2.getFontMetrics();
			
			if (hovered) {
				// Text glow
				g2.setColor(new Color(0, 255, 255, 100));
				g2.drawString(text, buttonX + buttonWidth/2 - fm.stringWidth(text)/2 - 2, y + buttonHeight/2 + fm.getAscent()/2 - 2);
				g2.drawString(text, buttonX + buttonWidth/2 - fm.stringWidth(text)/2 + 2, y + buttonHeight/2 + fm.getAscent()/2 + 2);
			}
			
			g2.setColor(hovered ? Color.WHITE : new Color(0, 255, 255));
			g2.drawString(text, buttonX + buttonWidth/2 - fm.stringWidth(text)/2, y + buttonHeight/2 + fm.getAscent()/2);
			
		} else {
			// Disabled state
			g2.setColor(new Color(20, 20, 30, 100));
			g2.fillRect(buttonX, y, buttonWidth, buttonHeight);
			
			g2.setColor(new Color(80, 80, 100));
			g2.setStroke(new BasicStroke(1));
			g2.drawRect(buttonX, y, buttonWidth, buttonHeight);
			
			g2.setFont(menuButtonFont);
			FontMetrics fm = g2.getFontMetrics();
			g2.setColor(new Color(100, 100, 120));
			g2.drawString(text, buttonX + buttonWidth/2 - fm.stringWidth(text)/2, y + buttonHeight/2 + fm.getAscent()/2);
		}
		
		g2.setStroke(new BasicStroke(1));
	}

	private void drawEnhancedHUD(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		if (!levelStarting) {
			// Top bar
			g2.setColor(new Color(20, 10, 40, 160));
			g2.fillRect(10, 10, WIDTH - 20, 40);
			g2.setColor(new Color(100, 50, 150));
			g2.setStroke(new BasicStroke(1));
			g2.drawRect(10, 10, WIDTH - 20, 40);

			// Level text
			g2.setFont(new Font("Arial", Font.BOLD, 20));
			g2.setColor(new Color(255, 215, 0));
			g2.drawString("LVL " + level, 30, 40);
			
			// --- Time bar (center) ---
			int timerBarWidth = 250;
			int timerBarHeight = 20;
			int timerBarX = WIDTH / 2 - timerBarWidth / 2;
			int timerBarY = 20;
			
			// Fill based on time remaining
			float timePercent = (float) levelTimer / (30 * 60);
			int fillWidth = (int) (timerBarWidth * timePercent);
			int secondsLeft = levelTimer / 60;

			// Outer border with glow
			g2.setColor(new Color(0, 200, 255, 150));
			g2.setStroke(new BasicStroke(2));
			g2.drawRect(timerBarX - 2, timerBarY - 2, timerBarWidth + 4, timerBarHeight + 4);

			// Corner accents
			int cornerSize = 8;
			g2.setColor(new Color(0, 255, 255, 200));
			g2.fillRect(timerBarX - 2, timerBarY - 2, cornerSize, 2);
			g2.fillRect(timerBarX - 2, timerBarY - 2, 2, cornerSize);
			g2.fillRect(timerBarX + timerBarWidth - cornerSize + 2, timerBarY - 2, cornerSize, 2);
			g2.fillRect(timerBarX + timerBarWidth, timerBarY - 2, 2, cornerSize);
			g2.fillRect(timerBarX - 2, timerBarY + timerBarHeight - cornerSize + 2, 2, cornerSize);
			g2.fillRect(timerBarX - 2, timerBarY + timerBarHeight, cornerSize, 2);
			g2.fillRect(timerBarX + timerBarWidth, timerBarY + timerBarHeight - cornerSize + 2, 2, cornerSize);
			g2.fillRect(timerBarX + timerBarWidth - cornerSize + 2, timerBarY + timerBarHeight, cornerSize, 2);
			
			// Background with grid pattern
			g2.setColor(new Color(10, 20, 30, 200));
			g2.fillRect(timerBarX, timerBarY, timerBarWidth, timerBarHeight);

			// Grid lines
			g2.setColor(new Color(0, 100, 150, 50));
			for (int i = 0; i < timerBarWidth; i += 20) {
				g2.drawLine(timerBarX + i, timerBarY, timerBarX + i, timerBarY + timerBarHeight);
			}

			// Determine color based on time
			Color timeColor;
			if (timePercent > 0.5f) {
				timeColor = new Color(0, 200, 255); // Cyan
			} else if (timePercent > 0.25f) {
				timeColor = new Color(255, 150, 0); // Orange
			} else {
				// Pulsing red when critical
				int pulse = (int) (150 + 105 * Math.sin(System.currentTimeMillis() / 200.0));
				timeColor = new Color(255, 0, 0, pulse);
			}

			// Filled portion with gradient
			GradientPaint fillGradient = new GradientPaint(
				timerBarX, timerBarY, timeColor,
				timerBarX, timerBarY + timerBarHeight, new Color(timeColor.getRed() / 2, timeColor.getGreen() / 2, timeColor.getBlue() / 2)
			);
			g2.setPaint(fillGradient);
			g2.fillRect(timerBarX, timerBarY, fillWidth, timerBarHeight);
			
			// Animated scan line
			long time = System.currentTimeMillis();
			int scanPos = (int) ((time / 10) % timerBarWidth);
			g2.setColor(new Color(0, 255, 255, 100));
			g2.fillRect(timerBarX + scanPos, timerBarY, 2, timerBarHeight);
			
			// Animated clock icon
			int iconX = timerBarX - 20;
			int iconY = timerBarY + timerBarHeight / 2;

			// Clock circle
			g2.setColor(new Color(90, 110, 70));
			g2.fillOval(iconX - 10, iconY - 10, 20, 20);
			g2.setColor(new Color(200, 200, 180));
			g2.fillOval(iconX - 9, iconY - 9, 18, 18);

			// Rotating clock hand (pointer)
			double handAngle = (1.0 - timePercent) * 2 * Math.PI - Math.PI / 2; // Start at 12 o'clock, go clockwise
			int handLength = 5;
			int handX = iconX + (int)(handLength * Math.cos(handAngle));
			int handY = iconY + (int)(handLength * Math.sin(handAngle));

			g2.setColor(new Color(90, 110, 70));
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawLine(iconX, iconY, handX, handY);
			g2.setStroke(new BasicStroke(1));

			// Time text with digital style
			g2.setFont(new Font("Times New Roman", Font.BOLD, 20));
			//g2.setColor(new Color(0, 255, 255));
			String timeText = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60);
			FontMetrics fm = g2.getFontMetrics();

			// Time Text
			g2.setColor(new Color(200, 200, 180));
			g2.drawString(timeText, timerBarX + timerBarWidth + 8, timerBarY + timerBarHeight - 4);

			g2.setStroke(new BasicStroke(1)); // Reset stroke
			
			// --- Score and Fuel (right) ---
			g2.setFont(new Font("Arial", Font.PLAIN, 15));
			String scoreText = "Score: " + score;
			String fuelText = "Fuel: " + (int) ship.getFuel();

			int rightX = WIDTH - 30;
			FontMetrics rightFm = g2.getFontMetrics();

			g2.setColor(new Color(255, 215, 0));
			g2.drawString(scoreText, rightX - rightFm.stringWidth(scoreText), 25);

			g2.setColor(new Color(255, 150, 50));
			g2.drawString(fuelText, rightX - rightFm.stringWidth(fuelText), 40);
		}
		
		// --- Void energy bar (always show if > 0) ---
		ship.getVoidEnergy().drawBar(g2, WIDTH, HEIGHT);

		// --- Fade timer display ---
		if (fadeActive) {
			g2.setFont(new Font("Serif", Font.BOLD, 20));
			int fadeSecondsLeft = fadeTimer / 60 + 1;
			String fadeText = "FADE: " + fadeSecondsLeft + "s";
			
			// Pulsing effect
			int pulse = (int) (200 + 55 * Math.sin(System.currentTimeMillis() / 150.0));
			g2.setColor(new Color(255, 255, 0, pulse));
			
			FontMetrics fadeFm = g2.getFontMetrics();
			int fadeTextX = WIDTH / 2 - fadeFm.stringWidth(fadeText) / 2;
			int fadeTextY = HEIGHT - 100;
			
			// Background
			g2.setColor(new Color(0, 0, 0, 150));
			g2.fillRoundRect(fadeTextX - 10, fadeTextY - fadeFm.getAscent(), 
				fadeFm.stringWidth(fadeText) + 20, fadeFm.getHeight(), 10, 10);
			
			// Text
			g2.setColor(new Color(255, 255, 0, pulse));
			g2.drawString(fadeText, fadeTextX, fadeTextY);
			}
		}

		// Level transitions
		private void drawLevelTransition(Graphics2D g2) {
		// Minimal dark overlay
		int overlayAlpha = Math.min(200, levelDisplayAlpha);
		g2.setColor(new Color(0, 0, 0, overlayAlpha / 2));
		g2.fillRect(0, 0, WIDTH, HEIGHT);
		
		// Clean level text
		String levelText = "LEVEL " + (level + 1);
		g2.setFont(new Font("Times New Roman", Font.BOLD, 80));
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, levelDisplayAlpha / 255f));
		
		FontMetrics levelFm = g2.getFontMetrics();
		int textWidth = levelFm.stringWidth(levelText);
		int levelY = HEIGHT / 2;
		
		g2.setColor(new Color(255, 215, 0));
		g2.drawString(levelText, WIDTH / 2 - textWidth / 2, levelY);
		
		// Simple countdown
		g2.setFont(new Font("Arial", Font.PLAIN, 20));
		int secondsLeft = (levelStartCountdown / 60) + 1;
		String countdownText = "Starting in " + secondsLeft;
		FontMetrics fm = g2.getFontMetrics();
		g2.setColor(Color.WHITE);
		g2.drawString(countdownText, WIDTH / 2 - fm.stringWidth(countdownText) / 2, levelY + 60);
		
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			
		// Speed warning
		if (level >= 3 && level <= 10) {
			g2.setFont(new Font("Serif", Font.ITALIC, 30));
			String warning = "⚠ Asteroids accelerating...";
			FontMetrics warnFm = g2.getFontMetrics();
			g2.setColor(new Color(255, 100, 100, levelDisplayAlpha));
			g2.drawString(warning, 
				WIDTH / 2 - warnFm.stringWidth(warning) / 2, 
				levelY + 70);
		}
		
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
	}

	private void toggleVoidWorld() {
		VoidEnergy ve = ship.getVoidEnergy();
		abilityManager.setVoidActive(!ve.isActive());
		if (ve.isActive()) {		// Check immediate collision with asteroids
			ve.deactivate();
			for (Asteroid a : asteroids) {
				if (a.intersects(ship.getBounds())) {
					asteroidDeath = true;
					startDeathSequence();
					return;
				}
			}
		} else {
			// Entering void world
			ve.activate();
		}
	}

	private void drawGradientBackground(Graphics2D g2d, Color top, Color bottom) {
		GradientPaint gradient = new GradientPaint(
			0, 0, top,
			0, HEIGHT, bottom
		);
		g2d.setPaint(gradient);
		g2d.fillRect(0, 0, WIDTH, HEIGHT);
	}

	private void drawVoidScreenOverlay(Graphics2D g2) {
		long time = System.currentTimeMillis();
		
		// Purple hexagonal grid pattern
		g2.setColor(new Color(150, 0, 255, 30));
		g2.setStroke(new BasicStroke(1));
		
		int hexSize = 40;
		for (int y = 0; y < HEIGHT + hexSize; y += hexSize * 1.5) {
			for (int x = 0; x < WIDTH + hexSize; x += hexSize * 1.732) {
				int offsetX = (y / (int)(hexSize * 1.5)) % 2 == 0 ? 0 : (int)(hexSize * 0.866);
				drawHexagon(g2, x + offsetX, y, hexSize / 2);
			}
		}
		
		// Animated purple scan lines
		int scanY = (int) ((time / 15) % HEIGHT);
		for (int i = 0; i < 5; i++) {
			int y = (scanY + i * 20) % HEIGHT;
			int alpha = 100 - i * 15;
			g2.setColor(new Color(180, 0, 255, alpha));
			g2.drawLine(0, y, WIDTH, y);
		}
		
		// Corner HUD elements (purple)
		drawCornerHUD(g2, 20, 80, new Color(150, 0, 255, 150), "VOID");
		
		// Vignette effect
		RadialGradientPaint vignette = new RadialGradientPaint(
			WIDTH / 2, HEIGHT / 2, 
			Math.max(WIDTH, HEIGHT) / 1.5f,
			new float[]{0.0f, 1.0f},
			new Color[]{new Color(0, 0, 0, 0), new Color(80, 0, 120, 100)}
		);
		g2.setPaint(vignette);
		g2.fillRect(0, 0, WIDTH, HEIGHT);
	}

	private void drawFadeScreenOverlay(Graphics2D g2) {
		long time = System.currentTimeMillis();
		
		// Golden circuit-like pattern
		g2.setColor(new Color(255, 215, 0, 40));
		g2.setStroke(new BasicStroke(1));
		
		// Horizontal lines with nodes
		for (int y = 50; y < HEIGHT; y += 80) {
			int alpha = (int) (30 + 20 * Math.sin(time / 200.0 + y / 50.0));
			g2.setColor(new Color(255, 215, 0, alpha));
			g2.drawLine(0, y, WIDTH, y);
			
			// Nodes
			for (int x = 100; x < WIDTH; x += 150) {
				g2.fillOval(x - 3, y - 3, 6, 6);
			}
		}
		
		// Vertical lines with nodes
		for (int x = 80; x < WIDTH; x += 100) {
			int alpha = (int) (30 + 20 * Math.sin(time / 250.0 + x / 50.0));
			g2.setColor(new Color(255, 215, 0, alpha));
			g2.drawLine(x, 0, x, HEIGHT);
			
			// Nodes
			for (int y = 100; y < HEIGHT; y += 150) {
				g2.fillOval(x - 3, y - 3, 6, 6);
			}
		}
		
		// Animated yellow scan lines (faster than void)
		int scanY = (int) ((time / 10) % HEIGHT);
		for (int i = 0; i < 3; i++) {
			int y = (scanY + i * 30) % HEIGHT;
			int alpha = 80 - i * 20;
			g2.setColor(new Color(255, 255, 0, alpha));
			g2.drawLine(0, y, WIDTH, y);
		}
		
		// Corner HUD elements (yellow/golden)
		drawCornerHUD(g2, 20, 80, new Color(255, 215, 0, 150), "FADE");
		
		// Lighter vignette for fade
		RadialGradientPaint vignette = new RadialGradientPaint(
			WIDTH / 2, HEIGHT / 2,
			Math.max(WIDTH, HEIGHT) / 1.3f,
			new float[]{0.0f, 1.0f},
			new Color[]{new Color(0, 0, 0, 0), new Color(255, 215, 0, 60)}
		);
		g2.setPaint(vignette);
		g2.fillRect(0, 0, WIDTH, HEIGHT);
	}

	private void drawHexagon(Graphics2D g2, int centerX, int centerY, int radius) {
		Polygon hexagon = new Polygon();
		for (int i = 0; i < 6; i++) {
			double angle = Math.PI / 3 * i;
			int x = (int) (centerX + radius * Math.cos(angle));
			int y = (int) (centerY + radius * Math.sin(angle));
			hexagon.addPoint(x, y);
		}
		g2.drawPolygon(hexagon);
	}

	private void drawCornerHUD(Graphics2D g2, int cornerSize, int offset, Color color, String mode) {
		// Top-left
		g2.setColor(color);
		g2.setStroke(new BasicStroke(2));
		g2.drawLine(offset, offset, offset + cornerSize, offset);
		g2.drawLine(offset, offset, offset, offset + cornerSize);
		
		// Top-right
		g2.drawLine(WIDTH - offset, offset, WIDTH - offset - cornerSize, offset);
		g2.drawLine(WIDTH - offset, offset, WIDTH - offset, offset + cornerSize);
		
		// Bottom-left
		g2.drawLine(offset, HEIGHT - offset, offset + cornerSize, HEIGHT - offset);
		g2.drawLine(offset, HEIGHT - offset, offset, HEIGHT - offset - cornerSize);
		
		// Bottom-right
		g2.drawLine(WIDTH - offset, HEIGHT - offset, WIDTH - offset - cornerSize, HEIGHT - offset);
		g2.drawLine(WIDTH - offset, HEIGHT - offset, WIDTH - offset, HEIGHT - offset - cornerSize);
		
		// Mode indicator (top-left corner)
		g2.setFont(new Font("Monospaced", Font.BOLD, 12));
		g2.drawString(mode, offset + 5, offset + cornerSize + 15);
		
		g2.setStroke(new BasicStroke(1));
	}

	private void activateAbility(int slot) {
		String ability = abilityLoadout.getSlot(slot);
		if (ability == null) return;

		ability = ability.toLowerCase();
		
		switch (ability) {
			case "void" -> { 
				// Void toggle - only if unlocked and not on cooldown
				if (shipLevel.isAbilityUnlocked("void") && voidCooldownTimer == 0) {
					toggleVoidWorld();
					if (!ship.getVoidEnergy().isActive()) {
						// Started cooldown when exiting
						voidCooldownTimer = voidMaxCooldown;
						abilityManager.startVoidCooldown();
					}
				}
			}
			case "fade" -> { 
				// Fade toggle - only if unlocked and not on cooldown
				if (shipLevel.isAbilityUnlocked("fade") && !fadeActive && fadeCooldownTimer == 0) {
					fadeActive = true;
					fadeTimer = maxFadeTime;
				}
			}
			case "teleport" -> {
				if (shipLevel.isAbilityUnlocked("teleport")) {
					if (!teleportAnchor.isAnchorPlaced() && !teleportAnchor.isOnCooldown()) {
						// Place anchor
						teleportAnchor.placeAnchor(ship.getX(), ship.getY());
					} else if (teleportAnchor.isAnchorPlaced()) {
						// Teleport to anchor
						if (teleportAnchor.teleportToAnchor(ship)) {
							teleportFlashFrame = 15;
							abilityManager.startTeleportCooldown();
						}
					}
				}
			}
			case "shield" -> { if (shipLevel.isAbilityUnlocked("shield")) abilityManager.activateShield(); }
			case "timeslow" -> { if (shipLevel.isAbilityUnlocked("timeslow")) abilityManager.activateTimeSlow(); }
			case "drone" -> { if (shipLevel.isAbilityUnlocked("drone")) abilityManager.toggleDrone(); }
			case "nova" -> { if (shipLevel.isAbilityUnlocked("nova")) abilityManager.activateNova(ship.getVoidEnergy()); }
		}
	}

	private void handleShoot() {
		if (fadeActive) return;
		if (ship.getVoidEnergy().getEnergy() > 0) {
			Point2D.Double tip = ship.getTipPosition();
			voidLasers.add(new VoidLaser(tip.x, tip.y, ship.getAngle(), WIDTH, HEIGHT));
			if (ship.getVoidEnergy().isActive()) ship.getVoidEnergy().absorbForAction(2.0);
			else ship.getVoidEnergy().dissipateForAction(5.0);
		} else {
			Point2D.Double tip = ship.getTipPosition();
			double spawnX = tip.x + 30 * Math.sin(ship.getAngle());
			double spawnY = tip.y - 30 * Math.cos(ship.getAngle());
			bullets.add(new Bullet(spawnX, spawnY, ship.getAngle()));
		}
	}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (gameState) {
            case "menu" -> {
				if (showingSettings) {
					settingsPanel.handleKeyPress(e.getKeyCode());
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						showingSettings = false;
						abilityManager.syncWithLoadout(abilityLoadout, shipLevel);
						repaint();
					}
					return;
				}
				
				int key = e.getKeyCode();
				switch (key) {
					case KeyEvent.VK_ENTER -> {
						if (!showingStory) startNewGame();
					}
					case KeyEvent.VK_ESCAPE -> {
						if (showingStory) {
							showingStory = false;
						}
					}
					case KeyEvent.VK_S -> {
						if (noSavedGame || showingStory) break;
						resumeGame();
						gameState = "playing";
        gameClock.reset(System.nanoTime());
                	}
				}
            }
			case "playing" -> {
				int key = e.getKeyCode();
				if (key == controlConfig.getKey("thrust")) thrusting = true;
				else if (key == controlConfig.getKey("left")) rotatingLeft = true;
				else if (key == controlConfig.getKey("right")) rotatingRight = true;
				else if (key == controlConfig.getKey("hyper")) ship.setHyper(true);
				else if (key == controlConfig.getKey("shoot")) handleShoot();
				else if (key == controlConfig.getKey("ability1")) activateAbility(0);
				else if (key == controlConfig.getKey("ability2")) activateAbility(1);
				else if (key == controlConfig.getKey("ability3")) activateAbility(2);
				else if (key == controlConfig.getKey("ability4")) activateAbility(3);
				else if (key == KeyEvent.VK_ESCAPE) {
					gameState = "paused";
                    gameClock.reset(System.nanoTime());
                    noSavedGame = false;
                    saveGame();
				}
			}
            case "paused" -> {
				int key = e.getKeyCode();
                switch (key) {
                    case KeyEvent.VK_ESCAPE -> {
                        gameState = "playing";
                        gameClock.reset(System.nanoTime());
                    }
					case KeyEvent.VK_ENTER -> {
						gameState = "menu";
						setupMenu();
						revalidate();
						repaint();
					}
                }
            }
            case "death" -> {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    deathCountdown = 0;
                    if (deathCountdown < 0) deathCountdown = 0;
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    	if (gameState.equals("playing")) {
    	    int key = e.getKeyCode();
	        if (key == controlConfig.getKey("thrust")) thrusting = false;
	        else if (key == controlConfig.getKey("left")) rotatingLeft = false;
	        else if (key == controlConfig.getKey("right")) rotatingRight = false;
	        else if (key == controlConfig.getKey("hyper")) ship.setHyper(false);
    	}
    }

	@Override
    public void keyTyped(KeyEvent e) {
		if (gameState.equals("menu") && showingStory) {
			storyTerminal.addChar(e.getKeyChar());
			return;
		}
	}

	private void drawMenuBackground(Graphics2D g2) {
		if (menuBackgroundLayer == null
				|| menuBackgroundLayer.getWidth() != WIDTH
				|| menuBackgroundLayer.getHeight() != HEIGHT) {
			menuBackgroundLayer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
			menuBackgroundDirty = true;
		}
		if (menuBackgroundDirty) {
			Graphics2D layerGraphics = menuBackgroundLayer.createGraphics();
			try {
				layerGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				layerGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
						RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
				layerGraphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
						RenderingHints.VALUE_COLOR_RENDER_SPEED);
				galaxyBackground.draw(layerGraphics, WIDTH, HEIGHT);
				starField.draw(layerGraphics);
                drawNeonGridOverlay(layerGraphics);
                drawNeonTitle(layerGraphics);
            } finally {
                layerGraphics.dispose();
            }
            galaxyBackground.releaseDisplayLayer();
            menuBackgroundDirty = false;
		}
		g2.drawImage(menuBackgroundLayer, 0, 0, null);
	}

	private void refreshSaveMetadata() {
		GameSaveState saveState = saveStore.loadState();
		highestScore = saveState.highestScore();
		highestDuration = saveState.highestDuration();
		noSavedGame = !saveState.hasActiveSession();
	}

    public static void main(String[] args) {
		DesktopLauncher.main(args);
    }
}
