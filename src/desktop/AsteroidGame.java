import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

public final class AsteroidGame extends JPanel implements ActionListener, KeyListener {
    private static final int COSMIC_FIRST_LEVEL = 3;
    private static final int MAX_COSMIC_WEIGHT = 10;
    private static final int MAX_ASTEROIDS = 24;
    private static final int MAX_LEVEL_START_ASTEROIDS = 5;
    private static final int BLACK_HOLE_FIRST_LEVEL = 8;
	private static final int MAX_NATIVE_BLACK_HOLES = 3;
	private static final int SHOOT_INTERVAL_FRAMES = 8;
    private static final Font BLACK_HOLE_WARNING_FONT = new Font("SansSerif", Font.BOLD, 15);
    // Alpha Fades and Timers
    private int level;
	private final Timer timer;
	private final GameClock gameClock = new GameClock(System.nanoTime());
	private final GameAudio audio = new GameAudio();
	private int spawnCreaturesCooldown = 0;
	private int spawnEntitiesCooldown = 0;
	private int deathCountdown;
    private int levelStartCountdown;
	private int levelDisplayAlpha;
	private int levelTimer;
	private boolean levelStarting = false; 
	private int voidCooldownTimer = 0;
	private final int voidMaxCooldown = 3 * 60;
		
	// Game Objects
	private final List<Asteroid> asteroids = new ArrayList<>();
	private final List<Bullet> bullets = new ArrayList<>();
	private final List<VoidLaser> voidLasers = new ArrayList<>();
	private final List<BlackHole> blackHoles = new ArrayList<>();
	private final List<CosmicEntity> cosmicEntities = new ArrayList<>();
	private final List<VoidCreature> voidCreatures = new ArrayList<>();
	// Collision & Physics Handling
	private final List<Asteroid> asteroidsToAdd = new ArrayList<>();
	private final Set<Asteroid> asteroidsToRemove = new HashSet<>();
	private final Set<VoidLaser> lasersToRemove = new HashSet<>();
	private final Set<Bullet> bulletsToRemove = new HashSet<>();
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
	private boolean shooting;
	private int shootIntervalRemaining;
	// ------ Abilities ------
	// Ability Manager
	private AbilityManager abilityManager;
	// Teleport
	private TeleportAnchor teleportAnchor;
	private int teleportFlashFrame = 0;

	// ====== Reward Systems ======
	private int fuelGain;
	private int scoreGain;
	// Legacy score storage and asteroid destruction counters
	private int score;
	private int incidentEssence;
	private WeaponSystem weaponSystem;
	private ConstructPanel constructPanel;
	private UtilitySystem utilitySystem;
	private boolean showingConstruct;
	private int pendingAbility = -1;
	private int pendingUtility = -1;
	private int absorbedBlackHoles;
	private boolean chargingBlackHole;
	private boolean blackHoleChargeCanceled;
	private int blackHoleChargeFrames;
	private int blackHoleChargeSlot = -1;
	private double blackHoleLaunchAngle;
	private String shortcutMessage = "";
	private int shortcutMessageTicks;
	private boolean shortcutMessageError;
	private static final String[] CONSTRUCT_ABILITIES = {"teleport", "drone", "nova", "blackhole"};
	private int largeDestroyed;
	private int mediumDestroyed;
	private int smallDestroyed;
	// Legacy record and best duration
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
	private InstructionsPanel instructionsPanel;
	private boolean showingInstructions;
	private DeveloperTerminal developerTerminal;
	private boolean showingDeveloperTerminal;
	private boolean suppressTerminalOpenTyped;
	private boolean cosmicWeightOverride;
	private boolean voidWeightOverride;
	// Game Stats
	private int targetTotalCreatureWeight = 0;
	private int currentTotalCreatureWeight = 0;
	private int targetTotalEntityWeight = 0;
	private int currentTotalEntityWeight = 0;
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

		starField = new StarField(WIDTH, HEIGHT, 110);
		particleSystem = new ParticleSystem();
		galaxyBackground = new GalaxyBackground();
		spaceGrid = new SpaceGrid();

		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setFocusable(true);
		setFocusTraversalKeysEnabled(false);

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

	@Override public void addNotify() {
		super.addNotify();
		audio.start();
		syncAudio();
	}

	@Override public void removeNotify() {
		audio.close();
		super.removeNotify();
	}

	private void syncAudio() {
		if (settingsPanel != null) audio.setVolumes(settingsPanel.getMusicVolume(),
				settingsPanel.getSFXVolume());
		GameAudio.Track track = switch (gameState) {
			case "menu" -> showingStory ? GameAudio.Track.STORY : GameAudio.Track.MENU;
			case "playing" -> ship.getVoidEnergy().isActive()
					? GameAudio.Track.VOID : GameAudio.Track.BATTLE;
			default -> null;
		};
		if (audio.selected() != track) audio.select(track);
	}

	private void exitStory() {
		showingStory = false;
		audio.forget(GameAudio.Track.STORY);
		syncAudio();
	}

	private void resetVars() {
	    score = 0;
	    incidentEssence = 0;
	    largeDestroyed = 0;
	    mediumDestroyed = 0;
	    smallDestroyed = 0;
		level = 0;
		spawnCreaturesCooldown = 0;
		spawnEntitiesCooldown = 0;
		targetTotalCreatureWeight = 0;
		currentTotalCreatureWeight = 0;
		targetTotalEntityWeight = 0;
		currentTotalEntityWeight = 0;
		cosmicWeightOverride = false;
		voidWeightOverride = false;
		clearPendingChanges();
	    deathCountdown = 5 * 60;		// 5 seconds at 60 FPS
	    levelStartCountdown = 3 * 60; 	// countdown before level starts (1 second)
	    levelDisplayAlpha = 255; 		// for fade out effect
	    levelTimer = 30 * 60; 			// 30 seconds * 60 FPS (assuming 60 FPS)
	    levelStarting = false;
	    thrusting = false;
	    rotatingLeft = false;
	    rotatingRight = false;
		shooting = false;
		shootIntervalRemaining = 0;
	    voidDeath = false;
	    bulletDeath = false;
	    asteroidDeath = false;
		blackHoleDeath = false;
		cosmicEntityDeath = false;
		voidCreatureDeath = false;
	    deathExplosionFrame = 0;
	    deathExplosionDone = false;
		ship = new Ship();
		audio.setHyperActive(false);
		teleportAnchor = new TeleportAnchor();
		abilityManager = new AbilityManager();
		abilityLoadout.resetForNewRun();
		weaponSystem = new WeaponSystem();
		utilitySystem = new UtilitySystem();
		constructPanel = new ConstructPanel();
		showingConstruct = false;
		pendingAbility = -1;
		pendingUtility = -1;
		absorbedBlackHoles = 0;
		chargingBlackHole = false;
		blackHoleChargeCanceled = false;
		blackHoleChargeFrames = 0;
		blackHoleChargeSlot = -1;
		shortcutMessage = "";
		shortcutMessageTicks = 0;
		floatingTextManager = new FloatingTextManager();
		blackHoles.clear();
		cosmicEntities.clear();
		teleportFlashFrame = 0;
		voidCooldownTimer = 0;
		hoveredButton = -1;
		settingsPanel = new SettingsPanel(WIDTH, HEIGHT, controlConfig);
		showingSettings = false;
		instructionsPanel = new InstructionsPanel(controlConfig);
		showingInstructions = false;
		developerTerminal = createDeveloperTerminal();
		showingDeveloperTerminal = false;
		suppressTerminalOpenTyped = false;
		storyTerminal = new StoryTerminal();
		showingStory = false;
		refreshSaveMetadata();
	}

	private void changeScene(String nextState) {
		if (!nextState.equals("playing")) setHyperMode(false);
		particleSystem.clear();
		floatingTextManager.clear();
		ship.getTrail().clear();
		showingConstruct = false;
		chargingBlackHole = false;
		blackHoleChargeSlot = -1;
		shortcutMessageTicks = 0;
		showingDeveloperTerminal = false;
		shooting = false;
		if (nextState.equals("menu") || nextState.equals("death")) {
			starField.setVoidMode(false);
			menuBackgroundDirty = true;
		}
		gameState = nextState;
		syncAudio();
	}

	private DeveloperTerminal createDeveloperTerminal() {
		return new DeveloperTerminal(new DeveloperTerminal.Backend() {
			@Override public String set(String target, int value) { return developerSet(target, value); }
			@Override public String reset(String target) { return developerReset(target); }
			@Override public String assignSlot(int slot, int abilityId, boolean unlock) {
				String ability = developerAbilityName(abilityId);
				if (ability == null) return "Invalid ability ID.";
				if (unlock && ability.equals("blackhole") && absorbedBlackHoles < 1)
					return "Absorb a black hole before enabling this ability.";
				if (!abilityLoadout.setSlotForDeveloper(slot - 1, ability))
					return "Invalid ability slot.";
				if (unlock && abilityManager.getLevel(ability) == 0) {
					abilityManager.setLevel(ability, 1);
					if (ability.equals("teleport")) teleportAnchor.setLevel(1);
				}
				return "Slot " + slot + " = " + ability + (abilityManager.getLevel(ability) == 0
						? " (locked; icon gray)" : " (available)");
			}
			@Override public boolean isAbilityLocked(int abilityId) {
				String ability = developerAbilityName(abilityId);
				return ability == null || abilityManager.getLevel(ability) == 0;
			}
			@Override public List<Integer> availableSlots() {
				List<Integer> slots = new ArrayList<>();
				for (int i = 0; i < AbilityLoadout.SLOT_COUNT; i++)
					if (abilityLoadout.isSlotAvailable(i)) slots.add(i + 1);
				return slots;
			}
			@Override public String constructAbility(String ability, int abilityLevel, int slot) {
				if (!availableSlots().contains(slot) || abilityManager.getLevel(ability) != 0
						|| abilityLevel < 1 || abilityLevel > 3)
					return "Construction canceled; slot or ability is unavailable.";
				if (ability.equals("blackhole") && absorbedBlackHoles < abilityLevel)
					return "Absorb " + abilityLevel + " black holes before this construction.";
				if (!abilityLoadout.setSlot(slot - 1, ability)) return "Construction canceled; slot is unavailable.";
				abilityManager.setLevel(ability, abilityLevel);
				if (ability.equals("teleport")) teleportAnchor.setLevel(abilityLevel);
				return ability + " level = " + abilityLevel + "; assigned to slot " + slot + ".";
			}
			@Override public String clear(String target) { return developerClear(target); }
			@Override public String spawn(String target, String variant, int count) {
				return developerSpawn(target, variant, count);
			}
			@Override public Map<String, String> status() { return developerStatus(); }
		});
	}

	private String developerAbilityName(int id) {
		return switch (id) {
			case 1 -> "teleport";
			case 2 -> "drone";
			case 3 -> "nova";
			case 4 -> "blackhole";
			default -> null;
		};
	}

	private String developerSet(String target, int value) {
		switch (target) {
			case "level" -> {
				level = value;
				levelStarting = false;
				if (level < BLACK_HOLE_FIRST_LEVEL) blackHoles.clear();
				configureLevelHazards();
				developerSpawnToTargets();
				return "Level set to " + level + "; spawns updated.";
			}
			case "time" -> {
				levelStarting = false;
				levelTimer = (30 - value) * 60;
				return "Elapsed level time set to " + value + " s.";
			}
			case "cosmic-weight" -> {
				cosmicWeightOverride = true;
				targetTotalEntityWeight = value;
				cosmicEntities.clear();
				refreshPopulationTotals();
				developerSpawnToTargets();
				return "Cosmic weight = " + value + ". WARNING: later levels will not change cosmic weight this run.";
			}
			case "void-weight" -> {
				voidWeightOverride = true;
				targetTotalCreatureWeight = value;
				voidCreatures.clear();
				refreshPopulationTotals();
				developerSpawnToTargets();
				return "Void weight = " + value + ". WARNING: later levels will not change void weight this run.";
			}
			case "essence" -> {
				incidentEssence = value;
				return "Incident Essence = " + value + ".";
			}
			case "fuel" -> {
				ship.setFuel(value);
				return "M-AM = " + value + ".";
			}
			case "void-energy" -> {
				ship.getVoidEnergy().setEnergyForDeveloper(value);
				return "Void energy = " + value + ".";
			}
			case "black-holes-absorbed" -> {
				absorbedBlackHoles = value;
				return "Black holes absorbed = " + value + ".";
			}
			case "ship.x" -> {
				if (value > WIDTH) return "X exceeds screen width " + WIDTH + ". Usage: set ship.x <0.." + WIDTH + ">";
				ship.setX(value);
				return "Ship X = " + value + ".";
			}
			case "ship.y" -> {
				if (value > HEIGHT) return "Y exceeds screen height " + HEIGHT + ". Usage: set ship.y <0.." + HEIGHT + ">";
				ship.setY(value);
				return "Ship Y = " + value + ".";
			}
			default -> { return developerSetAbility(target, value); }
		}
	}

	private String developerReset(String target) {
		switch (target) {
			case "level" -> { return developerSet("level", 0); }
			case "time" -> { return developerSet("time", 0); }
			case "cosmic-weight" -> {
				cosmicWeightOverride = false;
				targetTotalEntityWeight = defaultPopulationWeight();
				developerSpawnToTargets();
				return "Cosmic weight reset to level " + level + " default: "
						+ targetTotalEntityWeight + ". Later levels update it normally.";
			}
			case "void-weight" -> {
				voidWeightOverride = false;
				targetTotalCreatureWeight = defaultPopulationWeight();
				developerSpawnToTargets();
				return "Void weight reset to level " + level + " default: "
						+ targetTotalCreatureWeight + ". Later levels update it normally.";
			}
			case "essence" -> { incidentEssence = 0; return "Incident Essence reset to 0."; }
			case "fuel" -> { ship.setFuel(500); return "M-AM reset to 500."; }
			case "void-energy" -> {
				ship.getVoidEnergy().reset();
				abilityManager.setVoidActive(false);
				starField.setVoidMode(false);
				return "Void energy reset to 0; Void mode ended.";
			}
			case "black-holes-absorbed" -> {
				absorbedBlackHoles = 0;
				return "Black holes absorbed reset to 0.";
			}
			case "ship.x" -> { ship.setX(WIDTH / 2.0); return "Ship X reset to center."; }
			case "ship.y" -> { ship.setY(HEIGHT / 2.0); return "Ship Y reset to center."; }
			default -> {
				if (target.matches("slot\\.[1-4]")) {
					int slot = target.charAt(5) - '0';
					abilityLoadout.clearSlot(slot - 1);
					return "Slot " + slot + " reset to empty.";
				}
				if (target.matches("weapon\\.(front|rear|piercing|enlargement)\\.level"))
					return developerSetAbility(target, 0);
				if (target.matches("ability\\.(void|teleport|blackhole|drone|nova)\\.level"))
					return developerSetAbility(target, target.startsWith("ability.void.") ? 1 : 0);
				if (target.matches("ability\\.(void|teleport|blackhole|drone|nova)\\.cooldown")) {
					String ability = target.split("\\.")[1];
					if (abilityManager.getLevel(ability) == 0)
						return ability + " is locked; cooldown is already at its default of 0.";
					return developerSetAbility(target, 0);
				}
				return "Unknown reset target. Use ls --targets.";
			}
		}
	}

	private String developerSetAbility(String target, int value) {
		String[] parts = target.split("\\.");
		if (parts.length != 3) return "Unknown target. Use ls --targets.";
		if (parts[0].equals("weapon")) {
			int option = switch (parts[1]) {
				case "front" -> ConstructPanel.FRONT_NOZZLES;
				case "rear" -> ConstructPanel.REAR_NOZZLES;
				case "piercing" -> ConstructPanel.PIERCING;
				case "enlargement" -> ConstructPanel.BULLET_ENLARGEMENT;
				default -> -1;
			};
			if (weaponSystem.setLevelForDeveloper(option, value))
				return parts[1] + " weapon level = " + value + ".";
			return "Invalid weapon level. Use set " + target + " --help.";
		}
		String name = parts[1];
		if (parts[0].equals("utility") && parts[2].equals("level")) {
			int utility = UtilitySystem.indexOf(name);
			if (utility < 0 || value < 0 || value > 3) return "Invalid utility level.";
			utilitySystem.setLevel(utility, value);
			if (utility == UtilitySystem.MODULATOR) ship.getVoidEnergy().setModulatorLevel(value);
			if (utility == UtilitySystem.SHIELD) abilityManager.setLevel("shield", value);
			return name + " utility level = " + value + ".";
		}
		switch (parts[2]) {
			case "level" -> {
				if (name.equals("blackhole") && value > absorbedBlackHoles)
					return "Absorb " + value + " black holes before this level.";
				if (value == 0 && !name.equals("void")) {
					int oldSlot = abilityLoadout.findSlot(name);
					if (oldSlot >= 0) abilityLoadout.clearSlot(oldSlot);
					if (name.equals("drone")) abilityManager.clearDrones();
					if (name.equals("nova")) abilityManager.resetNova();
					if (name.equals("teleport")) teleportAnchor.reset();
				}
				abilityManager.setLevel(name, value);
				if (name.equals("teleport") && value > 0) teleportAnchor.setLevel(value);
				if (value == 0 && name.equals("void")) return "Void is permanent. Usage: set ability.void.level 1";
				return name + " level = " + abilityManager.getLevel(name) + ".";
			}
			case "cooldown" -> {
				if (abilityManager.getLevel(name) == 0) return name + " is locked. Construct or set its level first.";
				if (!abilityManager.setCooldownRemainingFrames(name, value * 60)) {
					int maxSeconds = name.equals("shield") ? switch (abilityManager.getLevel("shield")) {
						case 2 -> 12;
						case 3 -> 7;
						default -> 18;
					} : name.equals("drone") ? 12 : 3600;
					return "Invalid cooldown for " + name + ". Usage: set ability." + name
							+ ".cooldown <0.." + maxSeconds + ">";
				}
				if (name.equals("void")) voidCooldownTimer = value * 60;
				if (name.equals("teleport")) teleportAnchor.setCooldownTimer(value * 60);
				return name + " cooldown remaining = " + value + " s.";
			}
			default -> { return "Unknown ability target. Use ls --targets."; }
		}
	}

	private void developerSpawnToTargets() {
		refreshPopulationTotals();
		while (!cosmicEntities.isEmpty() && currentTotalEntityWeight > targetTotalEntityWeight) {
			CosmicEntity removed = cosmicEntities.remove(cosmicEntities.size() - 1);
			currentTotalEntityWeight -= removed.getWeight();
		}
		while (!voidCreatures.isEmpty() && currentTotalCreatureWeight > targetTotalCreatureWeight) {
			VoidCreature removed = voidCreatures.remove(voidCreatures.size() - 1);
			currentTotalCreatureWeight -= removed.getWeight();
		}
		while (currentTotalEntityWeight < targetTotalEntityWeight) {
			CosmicEntity entity = new CosmicEntity(WIDTH, HEIGHT, Math.max(1, level),
					targetTotalEntityWeight - currentTotalEntityWeight);
			cosmicEntities.add(entity);
			currentTotalEntityWeight += entity.getWeight();
		}
		while (currentTotalCreatureWeight < targetTotalCreatureWeight) {
			VoidCreature creature = VoidCreature.createSpecter(WIDTH, HEIGHT, Math.max(1, level),
					targetTotalCreatureWeight - currentTotalCreatureWeight);
			voidCreatures.add(creature);
			currentTotalCreatureWeight += creature.getWeight();
		}
		spawnEntitiesCooldown = spawnCreaturesCooldown = 60;
	}

	private String developerClear(String target) {
		int removed;
		switch (target) {
			case "bullets" -> { removed = bullets.size(); bullets.clear(); bulletsToRemove.clear(); }
			case "lasers" -> { removed = voidLasers.size(); voidLasers.clear(); lasersToRemove.clear(); }
			case "drones" -> { removed = abilityManager.getActiveDroneCount(); abilityManager.clearDrones(); }
			case "asteroids" -> {
				removed = asteroids.size(); asteroids.clear(); asteroidsToAdd.clear(); asteroidsToRemove.clear();
			}
			case "cosmic entities" -> {
				removed = cosmicEntities.size(); cosmicEntities.clear(); cosmicEntitiesToRemove.clear();
				spawnEntitiesCooldown = 60;
			}
			case "void creatures" -> {
				removed = voidCreatures.size(); voidCreatures.clear(); creaturesToRemove.clear();
				spawnCreaturesCooldown = 60;
			}
			case "black holes" -> { removed = blackHoles.size(); blackHoles.clear(); blackHolesToRemove.clear(); }
			case "particles" -> { particleSystem.clear(); return "Particles cleared."; }
			default -> { return "Unknown clear target."; }
		}
		refreshPopulationTotals();
		return "Cleared " + removed + " " + target + ".";
	}

	private String developerSpawn(String target, String variant, int count) {
		switch (target) {
			case "asteroid" -> {
				if (asteroids.size() + asteroidsToAdd.size() + count > MAX_ASTEROIDS)
					return "Asteroid cap is " + MAX_ASTEROIDS + "; no objects spawned.";
				Asteroid.Size size = Asteroid.Size.valueOf(variant.toUpperCase(Locale.ROOT));
				for (int i = 0; i < count; i++) asteroids.add(Asteroid.randomAsteroid(WIDTH, HEIGHT, size));
			}
			case "cosmic", "void" -> {
				int weight = switch (variant) { case "small" -> 1; case "medium" -> 2; default -> 3; };
				refreshPopulationTotals();
				int current = target.equals("cosmic") ? currentTotalEntityWeight : currentTotalCreatureWeight;
				if (current + weight * count > MAX_COSMIC_WEIGHT)
					return "Weight cap is " + MAX_COSMIC_WEIGHT + " per population; no objects spawned.";
				for (int i = 0; i < count; i++) {
					if (target.equals("cosmic")) cosmicEntities.add(new CosmicEntity(WIDTH, HEIGHT,
							Math.max(1, level), CosmicEntity.SizeTier.valueOf(variant.toUpperCase(Locale.ROOT))));
					else voidCreatures.add(VoidCreature.createTier(WIDTH, HEIGHT,
							VoidCreature.SizeTier.valueOf(variant.toUpperCase(Locale.ROOT))));
				}
				refreshPopulationTotals();
			}
			case "black-hole" -> {
				if (blackHoles.size() + count > 3) return "Developer black-hole cap is 3; no objects spawned.";
				for (int i = 0; i < count; i++) blackHoles.add(new BlackHole(
						100 + rand.nextDouble() * Math.max(1, WIDTH - 200),
						100 + rand.nextDouble() * Math.max(1, HEIGHT - 200), 80));
			}
			case "drone" -> {
				if (!abilityManager.isEnabled("drone")) return "Drone ability is locked or disabled; no drones spawned.";
				if (count > abilityManager.getDroneMaxCharges()) return "Count exceeds drone level's active cap; no drones spawned.";
				for (int i = 0; i < count; i++) abilityManager.spawnDroneForDeveloper(ship);
			}
			default -> { return "Unknown spawn target. Use spawn --help."; }
		}
		return "Spawned " + count + " " + (variant.isEmpty() ? "" : variant + " ") + target + ".";
	}

	private Map<String, String> developerStatus() {
		Map<String, String> status = new LinkedHashMap<>();
		status.put("scene", gameState);
		status.put("level", Integer.toString(level));
		status.put("time", Integer.toString((30 * 60 - levelTimer) / 60));
		status.put("level-starting", Boolean.toString(levelStarting));
		status.put("level.timer.frames", Integer.toString(levelTimer));
		status.put("level.start.frames", Integer.toString(levelStartCountdown));
		status.put("death.countdown.frames", Integer.toString(deathCountdown));
		status.put("essence", Integer.toString(incidentEssence));
		status.put("fuel", Integer.toString((int) ship.getFuel()));
		status.put("fuel.infinite", Boolean.toString(ship.isInfiniteFuel()));
		status.put("black-holes-absorbed", Integer.toString(absorbedBlackHoles));
		status.put("ship.x", Integer.toString((int) ship.getX()));
		status.put("ship.y", Integer.toString((int) ship.getY()));
		status.put("ship.speed", String.format(Locale.ROOT, "%.2f", ship.getVelocity()));
		status.put("ship.angle", String.format(Locale.ROOT, "%.3f", ship.getAngle()));
		status.put("void-energy", Integer.toString((int) ship.getVoidEnergy().getEnergy()));
		status.put("void-active", Boolean.toString(ship.getVoidEnergy().isActive()));
		status.put("void.cooldown.frames", Integer.toString(voidCooldownTimer));
		status.put("teleport.pending", Boolean.toString(teleportAnchor.isPending()));
		status.put("teleport.cooldown.frames", Integer.toString(teleportAnchor.getCooldownTimer()));
		status.put("shield.ready", Boolean.toString(abilityManager.isShieldReady()));
		status.put("shield.charge.percent", Integer.toString((int) (100 * abilityManager.getShieldChargePercent())));
		status.put("nova.state", abilityManager.getNovaState().name().toLowerCase(Locale.ROOT));
		status.put("cosmic-weight", Integer.toString(targetTotalEntityWeight));
		status.put("void-weight", Integer.toString(targetTotalCreatureWeight));
		status.put("cosmic-weight-frozen", Boolean.toString(cosmicWeightOverride));
		status.put("void-weight-frozen", Boolean.toString(voidWeightOverride));
		status.put("cosmic-weight-current", Integer.toString(currentTotalEntityWeight));
		status.put("void-weight-current", Integer.toString(currentTotalCreatureWeight));
		status.put("cosmic-spawn.cooldown.frames", Integer.toString(spawnEntitiesCooldown));
		status.put("void-spawn.cooldown.frames", Integer.toString(spawnCreaturesCooldown));
		status.put("bullets", Integer.toString(bullets.size()));
		status.put("lasers", Integer.toString(voidLasers.size()));
		status.put("drones", Integer.toString(abilityManager.getActiveDroneCount()));
		status.put("drone-charges", Integer.toString(abilityManager.getDroneCharges()));
		status.put("asteroids", Integer.toString(asteroids.size()));
		status.put("cosmic-entities", Integer.toString(cosmicEntities.size()));
		status.put("void-creatures", Integer.toString(voidCreatures.size()));
		status.put("black-holes", Integer.toString(blackHoles.size()));
		status.put("particles", Integer.toString(particleSystem.getParticleCount()));
		status.put("pending-ability", Integer.toString(pendingAbility));
		status.put("construct-open", Boolean.toString(showingConstruct));
		status.put("developer-open", Boolean.toString(showingDeveloperTerminal));
		status.put("weapon.front.level", Integer.toString(weaponSystem.level(ConstructPanel.FRONT_NOZZLES)));
		status.put("weapon.rear.level", Integer.toString(weaponSystem.level(ConstructPanel.REAR_NOZZLES)));
		status.put("weapon.piercing.level", Integer.toString(weaponSystem.level(ConstructPanel.PIERCING)));
		status.put("weapon.enlargement.level", Integer.toString(weaponSystem.level(ConstructPanel.BULLET_ENLARGEMENT)));
		for (String ability : new String[]{"void", "teleport", "blackhole", "drone", "nova"}) {
			status.put("ability." + ability + ".level", Integer.toString(abilityManager.getLevel(ability)));
			status.put("ability." + ability + ".cooldown", Integer.toString(
					(ability.equals("void") ? voidCooldownTimer : ability.equals("teleport")
						? teleportAnchor.getCooldownTimer() : abilityManager.getCooldownRemainingFrames(ability)) / 60));
		}
		for (String utility : UtilitySystem.NAMES)
			status.put("utility." + utility + ".level",
					Integer.toString(utilitySystem.level(UtilitySystem.indexOf(utility))));
		for (int i = 0; i < AbilityLoadout.SLOT_COUNT; i++) {
			status.put("slot." + (i + 1), String.valueOf(abilityLoadout.getSlot(i)));
		}
		return status;
	}

	private ConstructPanel.Model constructModel() {
		int[] abilityLevels = new int[CONSTRUCT_ABILITIES.length];
		for (int i = 0; i < abilityLevels.length; i++) {
			abilityLevels[i] = abilityManager.getLevel(CONSTRUCT_ABILITIES[i]);
		}
		String[] slots = new String[AbilityLoadout.SLOT_COUNT];
		for (int i = 0; i < slots.length; i++) slots[i] = abilityLoadout.getSlot(i);
		return new ConstructPanel.Model(incidentEssence, weaponSystem.levels(),
				abilityLevels, slots, pendingAbility, utilitySystem.levels(),
				utilitySystem.slots(), pendingUtility, absorbedBlackHoles);
	}

	private Rectangle constructButtonBounds() {
		int barWidth = Math.min(400, WIDTH - 32);
		int barX = (WIDTH - barWidth) / 2;
		int x = barX >= 108 ? barX - 100 : 12;
		int y = barX >= 108 ? HEIGHT - 90 : HEIGHT - 132;
		return new Rectangle(x, y, 88, 70);
	}

	private void toggleConstruct() {
		if ((pendingAbility >= 0 || pendingUtility >= 0) && showingConstruct) {
			constructPanel.showError("Choose an available slot before closing.");
			return;
		}
		showingConstruct = !showingConstruct;
		if (showingConstruct) constructPanel.reset();
		if (showingConstruct) {
			chargingBlackHole = false;
			blackHoleChargeSlot = -1;
		}
		thrusting = rotatingLeft = rotatingRight = false;
		shooting = false;
		setHyperMode(false);
		gameClock.reset(System.nanoTime());
		repaint();
	}

	private void openConstructPage(boolean weapons) {
		if (pendingAbility >= 0 || pendingUtility >= 0) {
			if (!showingConstruct) toggleConstruct();
			constructPanel.showError("Choose an available slot before changing pages.");
			repaint();
			return;
		}
		if (!showingConstruct) toggleConstruct();
		if (weapons) constructPanel.showWeapons();
		else constructPanel.showAbilities();
		shortcutMessageTicks = 0;
		repaint();
	}

	private boolean handleConstructionShortcut(KeyEvent event) {
		if (!event.isControlDown()) return false;
		int key = event.getKeyCode();
		if (key == KeyEvent.VK_A) {
			openConstructPage(true);
			return true;
		}
		for (int slot = 0; slot < AbilityLoadout.SLOT_COUNT; slot++) {
			if (key != controlConfig.getKey("ability" + (slot + 1))) continue;
			String ability = abilityLoadout.getSlot(slot);
			if (ability == null || abilityManager.getLevel(ability) == 0) {
				openConstructPage(false);
			} else {
				for (int i = 0; i < CONSTRUCT_ABILITIES.length; i++) {
					if (CONSTRUCT_ABILITIES[i].equals(ability)) {
						purchaseAbility(i, true);
						break;
					}
				}
				repaint();
			}
			return true;
		}
		return false;
	}

	private void constructionFeedback(String message, boolean error, boolean shortcut) {
		if (error) constructPanel.showError(message);
		else constructPanel.showNotice(message);
		if (shortcut) {
			shortcutMessage = message;
			shortcutMessageError = error;
			shortcutMessageTicks = 120;
		}
	}

	private void purchaseAbility(int index, boolean shortcut) {
		if (index < 0 || index >= CONSTRUCT_ABILITIES.length) return;
		String name = CONSTRUCT_ABILITIES[index];
		int current = abilityManager.getLevel(name);
		if (current >= 3) {
			constructionFeedback("This ability is fully constructed.", true, shortcut);
		} else if (incidentEssence < 10) {
			constructionFeedback("Insufficient Incident Essence (need 10).", true, shortcut);
		} else if ("blackhole".equals(name) && absorbedBlackHoles < current + 1) {
			constructionFeedback("Absorb " + (current + 1)
					+ " black holes before constructing this level.", true, shortcut);
		} else if (current == 0 && !hasAvailableAbilitySlot()) {
			constructionFeedback("All ability slots are occupied.", true, shortcut);
		} else {
			incidentEssence -= 10;
			abilityManager.setLevel(name, current + 1);
			if ("teleport".equals(name)) teleportAnchor.setLevel(current + 1);
			if (current == 0) {
				pendingAbility = index;
				constructionFeedback("Choose an available ability slot.", false, shortcut);
			} else {
				constructionFeedback(name.substring(0, 1).toUpperCase() + name.substring(1)
						+ " upgraded to level " + (current + 1) + ".", false, shortcut);
			}
		}
	}

	private void handlePlayingClick(int x, int y) {
		if (showingDeveloperTerminal) return;
		if (showingConstruct) {
			applyConstructAction(constructPanel.handleClick(x, y, constructModel()));
		} else if (constructButtonBounds().contains(x, y)) {
			toggleConstruct();
		}
		repaint();
	}

	private void applyConstructAction(ConstructPanel.Action action) {
		switch (action.type) {
			case CLOSE -> toggleConstruct();
			case PURCHASE_WEAPON -> {
				int cost = 5;
				if (incidentEssence < cost) {
					constructPanel.showError("Need " + cost + " incident essence.");
				} else if (weaponSystem.construct(action.index)) {
					incidentEssence -= cost;
					constructPanel.showNotice("Construction complete.");
				} else {
					constructPanel.showError("This construction is complete.");
				}
			}
			case PURCHASE_ABILITY -> purchaseAbility(action.index, false);
			case PURCHASE_UTILITY -> {
				int current = utilitySystem.level(action.index);
				if (current >= 3) {
					constructPanel.showError("This utility is fully constructed.");
				} else if (incidentEssence < 10) {
					constructPanel.showError("Need 10 incident essence.");
				} else if (current == 0 && !utilitySystem.hasAvailableSlot()) {
					constructPanel.showError("All utility slots are occupied.");
				} else {
					incidentEssence -= 10;
					utilitySystem.setLevel(action.index, current + 1);
					if (action.index == UtilitySystem.MODULATOR)
						ship.getVoidEnergy().setModulatorLevel(current + 1);
					if (action.index == UtilitySystem.SHIELD)
						abilityManager.setLevel("shield", current + 1);
					if (current == 0) {
						pendingUtility = action.index;
						constructPanel.showNotice("Choose an available utility slot.");
					} else constructPanel.showNotice("Utility upgraded to level " + (current + 1) + ".");
				}
			}
			case ASSIGN_UTILITY_SLOT -> {
				if (pendingUtility < 0) break;
				if (utilitySystem.assignSlot(action.index, UtilitySystem.NAMES[pendingUtility])) {
					pendingUtility = -1;
					constructPanel.showNotice("Utility assigned to slot " + (action.index + 1) + ".");
				} else constructPanel.showError("Choose an available utility slot.");
			}
			case ASSIGN_SLOT -> {
				if (pendingAbility < 0) break;
				if (!abilityLoadout.isSlotAvailable(action.index)
						|| !abilityLoadout.setSlot(action.index, CONSTRUCT_ABILITIES[pendingAbility])) {
					constructPanel.showError("Choose an available slot.");
				} else {
					pendingAbility = -1;
					constructPanel.showNotice("Ability assigned to slot " + (action.index + 1) + ".");
				}
			}
			case NONE -> { }
		}
	}

	private boolean hasAvailableAbilitySlot() {
		for (int i = 0; i < AbilityLoadout.SLOT_COUNT; i++) {
			if (abilityLoadout.isSlotAvailable(i)) return true;
		}
		return false;
	}

    private int menuButtonY(int index) {
        return Math.max(210, HEIGHT / 2 - 95) + index * 60;
    }

    private int menuButtonAt(int x, int y) {
        if (x < WIDTH / 2 - 150 || x > WIDTH / 2 + 150) return -1;
        for (int i = 0; i < 5; i++) {
            int top = menuButtonY(i);
            if (y >= top && y <= top + 50) return i;
        }
        return -1;
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
				if (gameState.equals("playing")) {
					if (showingConstruct || constructButtonBounds().contains(e.getPoint())) audio.playClick();
					handlePlayingClick(e.getX(), e.getY());
					return;
				}
				if (!gameState.equals("menu")) return;
				
				int mx = e.getX();
				int my = e.getY();
				if (showingInstructions) {
					if (instructionsPanel.click(mx, my)) {
						audio.playClick();
						showingInstructions = false;
					}
					repaint();
					return;
				}
				
				if (showingSettings) {
					audio.playClick();
					boolean closeClicked = settingsPanel.handleClick(mx, my, AsteroidGame.this);
					
					if (closeClicked) {
						showingSettings = false;
					}
					repaint();
					return;
				}
				if (showingStory) return;
				
				int button = menuButtonAt(mx, my);
				if (button >= 0 && (button != 1 || !noSavedGame)) audio.playClick();
				switch (button) {
					case 0 -> startNewGame();
					case 1 -> resumeGame();
					case 2 -> showingInstructions = true;
					case 3 -> showingSettings = true;
					case 4 -> {
						showingStory = true;
						storyTerminal = new StoryTerminal();
						audio.restart(GameAudio.Track.STORY);
					}
					default -> { }
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
					String previous = settingsPanel.hoveredActionId();
					settingsPanel.handleMouseMove(mx, my);
					String hovered = settingsPanel.hoveredActionId();
					if (hovered != null && !hovered.equals(previous)) audio.playSelect();
					repaint();
					return;
				}
				
				if (showingInstructions || showingStory) return;
				int nextButton = menuButtonAt(mx, my);
				if (nextButton >= 0 && nextButton != hoveredButton) audio.playSelect();
				hoveredButton = nextButton;
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
			if (showingDeveloperTerminal) {
				developerTerminal.scroll(e.getWheelRotation());
				repaint();
				return;
			}
			if (showingInstructions) {
				instructionsPanel.scroll(e.getWheelRotation());
				repaint();
				return;
			}
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
		audio.forget(GameAudio.Track.BATTLE);
		audio.forget(GameAudio.Track.VOID);
	    changeScene("playing");
        gameClock.reset(System.nanoTime());
		menuBackgroundLayer = null;
		menuBackgroundDirty = true;
	    resetVars();
		syncAudio();
	    ship = new Ship();
	    asteroids.clear();
	    bullets.clear();
		voidLasers.clear();
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
		abilityLoadout.reset();
		abilityManager = new AbilityManager();
		weaponSystem = new WeaponSystem();
		utilitySystem = new UtilitySystem();
		teleportAnchor = new TeleportAnchor();
		pendingAbility = pendingUtility = -1;

		level = session.level();
		score = session.score();
		ship.setFuel(session.fuel());
		levelStartCountdown = session.levelStartCountdown();
		levelDisplayAlpha = session.levelDisplayAlpha();
		levelStarting = session.levelStarting();
		ship.setX(session.shipX());
		ship.setY(session.shipY());
		ship.setAngle(session.shipAngle());
		incidentEssence = session.incidentEssence();
		absorbedBlackHoles = session.absorbedBlackHoles();
		ship.setInfiniteFuel(session.infiniteFuel());
		for (int i = 0; i < 4; i++) {
			weaponSystem.setLevelForDeveloper(i, session.weaponLevels()[i]);
			abilityManager.setLevel(CONSTRUCT_ABILITIES[i], session.abilityLevels()[i]);
			utilitySystem.setLevel(i, session.utilityLevels()[i]);
		}
		teleportAnchor.setLevel(abilityManager.getLevel("teleport"));
		ship.getVoidEnergy().setModulatorLevel(utilitySystem.level(UtilitySystem.MODULATOR));
		abilityManager.setLevel("shield", utilitySystem.level(UtilitySystem.SHIELD));
		for (int i = 0; i < 4; i++) {
			if (session.abilitySlots()[i] != null)
				abilityLoadout.setSlot(i, session.abilitySlots()[i]);
			if (session.utilitySlots()[i] != null)
				utilitySystem.assignSlot(i, session.utilitySlots()[i]);
		}
		configureLevelHazards();

		noSavedGame = false;
		changeScene("playing");
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
				incidentEssence,
				absorbedBlackHoles,
				ship.isInfiniteFuel(),
				weaponSystem.levels(),
				new int[]{abilityManager.getLevel("teleport"), abilityManager.getLevel("drone"),
					abilityManager.getLevel("nova"), abilityManager.getLevel("blackhole")},
				new String[]{abilityLoadout.getSlot(0), abilityLoadout.getSlot(1),
					abilityLoadout.getSlot(2), abilityLoadout.getSlot(3)},
				utilitySystem.levels(), utilitySystem.slots()
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
	    changeScene("death");
	    bullets.clear();
		voidLasers.clear();
	    asteroids.clear();
	    teleportAnchor.reset();
	    abilityManager.reset();
		cosmicEntities.clear();
		voidCreatures.clear();
		blackHoles.clear();
			currentTotalCreatureWeight = 0;
			currentTotalEntityWeight = 0;
			clearPendingChanges();
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

		// Update Settings Panel
		if (settingsPanel != null) {
            settingsPanel.updateDimensions(WIDTH, HEIGHT);
        }
	}

	private void clearPendingChanges() {
		asteroidsToAdd.clear();
		asteroidsToRemove.clear();
		lasersToRemove.clear();
		bulletsToRemove.clear();
		blackHolesToRemove.clear();
		cosmicEntitiesToRemove.clear();
		creaturesToRemove.clear();
	}

	private void refreshPopulationTotals() {
		currentTotalCreatureWeight = 0;
		for (VoidCreature creature : voidCreatures) {
			currentTotalCreatureWeight += creature.getWeight();
		}
		currentTotalEntityWeight = 0;
		for (CosmicEntity entity : cosmicEntities) {
			currentTotalEntityWeight += entity.getWeight();
		}
	}

	private void applyPendingChanges() {
		voidLasers.removeAll(lasersToRemove);
		bullets.removeAll(bulletsToRemove);
		asteroids.removeAll(asteroidsToRemove);
		for (UtilitySystem.Blast blast : utilitySystem.activeBlasts())
			destroyPendingSwarmFragments(blast);
		int availableAsteroidSlots = Math.max(0, MAX_ASTEROIDS - asteroids.size());
		asteroids.addAll(asteroidsToAdd.subList(0, Math.min(availableAsteroidSlots, asteroidsToAdd.size())));
		blackHoles.removeAll(blackHolesToRemove);
		cosmicEntities.removeAll(cosmicEntitiesToRemove);
		voidCreatures.removeAll(creaturesToRemove);
		refreshPopulationTotals();
		clearPendingChanges();
	}

	private void trySpawnCreatures() {
		if (levelStarting || (level == 0 && !voidWeightOverride)) return;
		
		if (spawnCreaturesCooldown > 0) { spawnCreaturesCooldown--; return; }
		// Spawn specters based on size limit
		if (currentTotalCreatureWeight >= targetTotalCreatureWeight) return;
		
		for (int i = 0; i < 2 && currentTotalCreatureWeight < targetTotalCreatureWeight; i++) {
			int remainingWeight = targetTotalCreatureWeight - currentTotalCreatureWeight;
			VoidCreature vc = VoidCreature.createSpecter(WIDTH, HEIGHT, level, remainingWeight);
			voidCreatures.add(vc);
			currentTotalCreatureWeight += vc.getWeight();
		}

		spawnCreaturesCooldown = 60;
	}

	private void trySpawnEntities() {
		if ((level < COSMIC_FIRST_LEVEL && !cosmicWeightOverride) || levelStarting) return;
		if (spawnEntitiesCooldown > 0) { spawnEntitiesCooldown--; return; }
		if (currentTotalEntityWeight >= targetTotalEntityWeight) return;

		for (int i = 0; i < 2 && currentTotalEntityWeight < targetTotalEntityWeight; i++) {
			int remainingWeight = targetTotalEntityWeight - currentTotalEntityWeight;
			CosmicEntity entity = new CosmicEntity(WIDTH, HEIGHT, level, remainingWeight);
			cosmicEntities.add(entity);
			currentTotalEntityWeight += entity.getWeight();
		}
		spawnEntitiesCooldown = 60; // cooldown of 60 frames (1 sec) before next spawn
	}

	private void configureLevelHazards() {
		if (!cosmicWeightOverride) targetTotalEntityWeight = defaultPopulationWeight();
		if (!voidWeightOverride) targetTotalCreatureWeight = defaultPopulationWeight();
		refreshPopulationTotals();
		if (level < BLACK_HOLE_FIRST_LEVEL) return;
		int nativeHoles = 0;
		for (BlackHole hole : blackHoles)
			if (!hole.isPlayerOwned() && !blackHolesToRemove.contains(hole)) nativeHoles++;
		if (nativeHoles >= MAX_NATIVE_BLACK_HOLES) return;

		// One new warning per level allows native holes to meet and merge.
		double bx = 150 + rand.nextDouble() * (WIDTH - 300);
		double by = 150 + rand.nextDouble() * (HEIGHT - 300);
		int bhSize = 60 + rand.nextInt(20) + Math.min(level, 10) * 2;
		blackHoles.add(new BlackHole(bx, by, bhSize));
	}

	private int defaultPopulationWeight() {
		return level < COSMIC_FIRST_LEVEL ? 0
				: Math.min(level - COSMIC_FIRST_LEVEL + 1, MAX_COSMIC_WEIGHT);
	}

	private void consumeBlackHoleContacts(BlackHole blackHole) {
		if (!blackHole.isActive()) return;
		for (Asteroid asteroid : asteroids) {
			if (asteroidsToRemove.contains(asteroid)) continue;
			if (blackHole.touches(asteroid.getBoundingBox())) {
				consumeAsteroid(asteroid);
				blackHole.grow(switch (asteroid.getSize()) {
					case LARGE -> 7; case MEDIUM -> 5; case SMALL -> 3;
				});
			}
		}
		for (CosmicEntity entity : cosmicEntities) {
			if (!cosmicEntitiesToRemove.contains(entity) && blackHole.touches(entity.getBounds())) {
				consumeCosmicEntity(entity);
				blackHole.grow(4);
			}
		}
		for (VoidCreature creature : voidCreatures) {
			if (!creaturesToRemove.contains(creature) && blackHole.touches(creature.getBounds())) {
				consumeVoidCreature(creature);
				blackHole.grow(4);
			}
		}
		if (!blackHole.isPlayerOwned()) {
			for (Bullet bullet : bullets) {
				if (!bulletsToRemove.contains(bullet) && blackHole.touches(bullet.getBoundingBox())) {
					bulletsToRemove.add(bullet);
					particleSystem.createBulletExplosion(bullet.getBoundingBox().getCenterX(),
							bullet.getBoundingBox().getCenterY());
					blackHole.grow(0.5);
				}
			}
			for (DroneSwarm.Drone drone : abilityManager.getDrones()) {
				if (blackHole.touchesCircle(drone.getX(), drone.getY(), 6)) {
					abilityManager.destroyDrone(drone);
					blackHole.grow(2.5);
				}
			}
		}
	}

	private void updateBlackHoles() {
		for (int i = 0; i < blackHoles.size(); i++) {
			BlackHole a = blackHoles.get(i);
			for (int j = i + 1; j < blackHoles.size(); j++) {
				BlackHole b = blackHoles.get(j);
				a.attractToward(b);
				b.attractToward(a);
			}
		}
		for (BlackHole hole : blackHoles) {
			hole.update(WIDTH, HEIGHT);
			if (hole.isExpired()) {
				particleSystem.createBlackHoleExplosion(hole.getX(), hole.getY());
				blackHolesToRemove.add(hole);
			}
		}
		List<BlackHole> joined = new ArrayList<>();
		for (int i = 0; i < blackHoles.size(); i++) {
			BlackHole a = blackHoles.get(i);
			if (!a.isActive() || blackHolesToRemove.contains(a)) continue;
			for (int j = i + 1; j < blackHoles.size(); j++) {
				BlackHole b = blackHoles.get(j);
				if (!b.isActive() || blackHolesToRemove.contains(b)) continue;
				if (Math.hypot(a.getX() - b.getX(), a.getY() - b.getY())
						<= a.getEventHorizonRadius() + b.getEventHorizonRadius()) {
					joined.add(BlackHole.merge(a, b));
					blackHolesToRemove.add(a);
					blackHolesToRemove.add(b);
					break;
				}
			}
		}
		blackHoles.addAll(joined);
	}

	private boolean clipLaserToBlackHole(VoidLaser laser) {
		BlackHole nearest = null;
		Point2D.Double impact = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		for (BlackHole hole : blackHoles) {
			if (blackHolesToRemove.contains(hole)) continue;
			Point2D.Double hit = hole.firstIntersection(laser.getLaserLine());
			if (hit == null) continue;
			double distance = hit.distanceSq(laser.getLaserLine().getP1());
			if (distance < bestDistance) {
				bestDistance = distance;
				nearest = hole;
				impact = hit;
			}
		}
		if (nearest == null) return false;
		laser.endAt(impact.x, impact.y);
		laser.markSpent();
		particleSystem.createBlackHoleLaserImpact(impact.x, impact.y);
		nearest.shrinkFromLaser();
		if (nearest.shouldBreak(utilitySystem.level(UtilitySystem.MODULATOR)))
			nearest.beginDeath();
		return true;
	}

	private boolean touchesBlackHole(Rectangle2D bounds) {
		for (BlackHole hole : blackHoles)
			if (!blackHolesToRemove.contains(hole) && hole.touches(bounds)) return true;
		return false;
	}

	private boolean touchesBlackHole(Line2D line) {
		for (BlackHole hole : blackHoles)
			if (!blackHolesToRemove.contains(hole) && hole.touches(line)) return true;
		return false;
	}

	private void consumeAsteroid(Asteroid asteroid) {
		if (!asteroidsToRemove.add(asteroid)) return;
		Rectangle bounds = asteroid.getBoundingBox();
		particleSystem.createAsteroidExplosion(bounds.getCenterX(), bounds.getCenterY(), asteroid.getSize());
	}

	private void consumeCosmicEntity(CosmicEntity entity) {
		if (cosmicEntitiesToRemove.add(entity))
			particleSystem.createCosmicEntityExplosion(entity.getX(), entity.getY());
	}

	private void consumeVoidCreature(VoidCreature creature) {
		if (creaturesToRemove.add(creature))
			particleSystem.createVoidCreatureExplosion(creature.getX(), creature.getY());
	}

	private void awardIncidentEssence(int weight) {
		int maxReward = weight == 1 ? 2 : weight == 2 ? 3 : 5;
		int reward = 1 + rand.nextInt(maxReward);
		incidentEssence = (int) Math.min(Integer.MAX_VALUE, (long) incidentEssence + reward);
	}

	private void destroyVoidCreature(VoidCreature creature) {
		if (!creaturesToRemove.add(creature)) return;
		particleSystem.createVoidCreatureExplosion(creature.getX(), creature.getY());
		awardIncidentEssence(creature.getWeight());
	}

	private void destroyCosmicEntity(CosmicEntity entity) {
		if (!cosmicEntitiesToRemove.add(entity)) return;
		particleSystem.createCosmicEntityExplosion(entity.getX(), entity.getY());
		awardIncidentEssence(entity.getWeight());
	}

	private void destroyAsteroid(Asteroid asteroid, boolean split) {
		if (!asteroidsToRemove.add(asteroid)) return;
		asteroid.triggerHitFlash();
		Rectangle bounds = asteroid.getBoundingBox();
		particleSystem.createAsteroidExplosion(bounds.getCenterX(), bounds.getCenterY(), asteroid.getSize());
		if (split && asteroid.getSize() != Asteroid.Size.SMALL) {
			asteroidsToAdd.addAll(asteroid.split());
		}
		int fuel = switch (asteroid.getSize()) {
			case LARGE -> { largeDestroyed++; yield 20; }
			case MEDIUM -> { mediumDestroyed++; yield 10; }
			case SMALL -> { smallDestroyed++; yield 5; }
		};
		score += fuel;
		ship.addFuel(fuel);
		floatingTextManager.add(FloatingTextManager.createFuel(fuel,
				bounds.getCenterX(), bounds.getCenterY() + 15));
	}

	private void destroyPendingSwarmFragments(UtilitySystem.Blast blast) {
		for (Iterator<Asteroid> it = asteroidsToAdd.iterator(); it.hasNext();) {
			Asteroid fragment = it.next();
			if (!circleTouches(blast.x(), blast.y(), blast.radius(),
					fragment.getBoundingBox())) continue;
			destroyAsteroid(fragment, false);
			it.remove();
		}
	}

	private boolean shieldTouchesObject() {
		double radius = abilityManager.getShieldBoundaryRadius();
		for (Asteroid asteroid : asteroids) {
			if (!asteroidsToRemove.contains(asteroid)
					&& circleTouches(ship.getX(), ship.getY(), radius, asteroid.getBoundingBox())) return true;
		}
		for (CosmicEntity entity : cosmicEntities) {
			if (!cosmicEntitiesToRemove.contains(entity)
					&& circleTouches(ship.getX(), ship.getY(), radius, entity.getBounds())) return true;
		}
		for (VoidCreature creature : voidCreatures) {
			if (!creaturesToRemove.contains(creature)
					&& circleTouches(ship.getX(), ship.getY(), radius, creature.getBounds())) return true;
		}
		return false;
	}

	private static boolean circleTouches(double cx, double cy, double radius, Rectangle rectangle) {
		double nearestX = Math.max(rectangle.getMinX(), Math.min(cx, rectangle.getMaxX()));
		double nearestY = Math.max(rectangle.getMinY(), Math.min(cy, rectangle.getMaxY()));
		double dx = cx - nearestX, dy = cy - nearestY;
		return dx * dx + dy * dy <= radius * radius;
	}

	private void updatePassiveShield() {
		if (!abilityManager.isShieldReady()) return;
		boolean contact = shieldTouchesObject();
		boolean clearEnergy = ship.getVoidEnergy().isOverThreshold();
		if (!contact && !clearEnergy) return;
		abilityManager.rememberShieldBurstRadius(WIDTH, HEIGHT);
		if (!abilityManager.triggerShield()) return;
		double burstRadius = abilityManager.getShieldBurstRadius(WIDTH, HEIGHT);
		for (Asteroid asteroid : asteroids) {
			if (!asteroidsToRemove.contains(asteroid)
					&& circleTouches(ship.getX(), ship.getY(), burstRadius, asteroid.getBoundingBox())) {
				destroyAsteroid(asteroid, false);
			}
		}
		for (CosmicEntity entity : cosmicEntities) {
			if (!cosmicEntitiesToRemove.contains(entity)
					&& circleTouches(ship.getX(), ship.getY(), burstRadius, entity.getBounds())) {
				destroyCosmicEntity(entity);
			}
		}
		for (VoidCreature creature : voidCreatures) {
			if (!creaturesToRemove.contains(creature)
					&& circleTouches(ship.getX(), ship.getY(), burstRadius, creature.getBounds())) {
				destroyVoidCreature(creature);
			}
		}
		if (clearEnergy) ship.getVoidEnergy().reset();
	}

	private List<DroneSwarm.Target> droneTargets() {
		List<DroneSwarm.Target> targets = new ArrayList<>(
				asteroids.size() + cosmicEntities.size() + voidCreatures.size());
		for (Asteroid asteroid : asteroids) {
			if (asteroidsToRemove.contains(asteroid)) continue;
			Rectangle r = asteroid.getBoundingBox();
			targets.add(new DroneSwarm.Target(r.getCenterX(), r.getCenterY(), Math.max(r.width, r.height) / 2.0));
		}
		for (CosmicEntity entity : cosmicEntities) {
			if (!cosmicEntitiesToRemove.contains(entity))
				targets.add(new DroneSwarm.Target(entity.getX(), entity.getY(), entity.getSize() / 2.0));
		}
		for (VoidCreature creature : voidCreatures) {
			if (ship.getVoidEnergy().isActive() && !creaturesToRemove.contains(creature))
				targets.add(new DroneSwarm.Target(creature.getX(), creature.getY(), creature.getSize() / 2.0));
		}
		return targets;
	}

	private void updateSwarm() {
		List<UtilitySystem.Target> targets = new ArrayList<>();
		for (Asteroid asteroid : asteroids) {
			if (asteroidsToRemove.contains(asteroid)) continue;
			Rectangle bounds = asteroid.getBoundingBox();
			targets.add(new UtilitySystem.Target(asteroid, bounds.getCenterX(),
					bounds.getCenterY(), Math.max(bounds.width, bounds.height) / 2.0));
		}
		for (CosmicEntity entity : cosmicEntities) {
			if (!cosmicEntitiesToRemove.contains(entity))
				targets.add(new UtilitySystem.Target(entity, entity.getX(), entity.getY(), entity.getSize() / 2.0));
		}
		for (VoidCreature creature : voidCreatures) {
			if (!creaturesToRemove.contains(creature))
				targets.add(new UtilitySystem.Target(creature, creature.getX(), creature.getY(), creature.getSize() / 2.0));
		}
		utilitySystem.updateSwarm(ship, targets, blackHoles, WIDTH, HEIGHT);
		for (UtilitySystem.Destruction destroyed : utilitySystem.drainDestructions())
			particleSystem.createDroneExplosion(destroyed.x(), destroyed.y());
		for (UtilitySystem.Blast blast : utilitySystem.drainBlasts())
			particleSystem.createDroneExplosion(blast.x(), blast.y());
		for (UtilitySystem.Blast blast : utilitySystem.activeBlasts()) {
			for (Asteroid asteroid : asteroids) {
				if (!asteroidsToRemove.contains(asteroid) && circleTouches(blast.x(), blast.y(),
						blast.radius(), asteroid.getBoundingBox())) destroyAsteroid(asteroid, false);
			}
			for (CosmicEntity entity : cosmicEntities) {
				if (!cosmicEntitiesToRemove.contains(entity) && circleTouches(blast.x(), blast.y(),
						blast.radius(), entity.getBounds())) destroyCosmicEntity(entity);
			}
			for (VoidCreature creature : voidCreatures) {
				if (!creaturesToRemove.contains(creature) && circleTouches(blast.x(), blast.y(),
						blast.radius(), creature.getBounds())) destroyVoidCreature(creature);
			}
			// Split fragments created earlier this tick grant the same M-AM reward.
			destroyPendingSwarmFragments(blast);
		}
	}

	private List<Rectangle> teleportObstacles() {
		List<Rectangle> obstacles = new ArrayList<>(
				asteroids.size() + cosmicEntities.size() + voidCreatures.size());
		for (Asteroid asteroid : asteroids) obstacles.add(new Rectangle(asteroid.getBoundingBox()));
		for (CosmicEntity entity : cosmicEntities) obstacles.add(new Rectangle(entity.getBounds()));
		for (VoidCreature creature : voidCreatures) obstacles.add(new Rectangle(creature.getBounds()));
		for (BlackHole hole : blackHoles) {
			if (hole.isWarningActive() || hole.isPlayerOwned()) continue;
			int radius = (int) Math.ceil(hole.getEventHorizonRadius() + 20);
			obstacles.add(new Rectangle((int) hole.getX() - radius,
					(int) hole.getY() - radius, radius * 2, radius * 2));
		}
		return obstacles;
	}

	private void updateDrones() {
		abilityManager.updateDrones(ship, droneTargets(), blackHoles, WIDTH, HEIGHT);
		for (DroneSwarm.Drone drone : abilityManager.getDrones()) {
			for (BlackHole hole : blackHoles) {
				if (!hole.isPlayerOwned() && hole.touchesCircle(drone.getX(), drone.getY(), 6)) {
					abilityManager.destroyDrone(drone);
					hole.grow(2.5);
					break;
				}
			}
		}
		for (DroneSwarm.Shot shot : abilityManager.drainDroneShots()) {
			int firstBullet = bullets.size();
			weaponSystem.fire(bullets, shot.x(), shot.y(), shot.angle(), 0.72);
			if (bullets.size() > firstBullet) audio.playFire();
		}
		for (DroneSwarm.Destruction destruction : abilityManager.drainDroneDestructions()) {
			particleSystem.createDroneExplosion(destruction.x(), destruction.y());
		}
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
		syncAudio();
        repaint();
    }

    private void updateGameTick() {
        switch (gameState) {
            case "menu" -> {
				if (showingStory) {
					storyTerminal.update();
					if (storyTerminal.isExitRequested()) exitStory();
				}
        		return;
            }
            case "playing" -> {
                if (shortcutMessageTicks > 0) shortcutMessageTicks--;
                if (showingConstruct || showingDeveloperTerminal) return;
                // Decrement the level timer every tick (frame)
                if (levelStarting) {
                    levelStartCountdown--;
					levelDisplayAlpha = (int) (255 * ((float)levelStartCountdown / 180));
                    if (levelDisplayAlpha <= 0) levelDisplayAlpha = 255;
                    if (levelStartCountdown <= 0) {
                        levelStarting = false;
                        level++;
                        levelStartCountdown = 3 * 60;
						int levelAsteroids = Math.min(2 + (level - 1) / 3, MAX_LEVEL_START_ASTEROIDS);
						for (int i = 0; i < levelAsteroids && asteroids.size() < MAX_ASTEROIDS; i++) {
                            Asteroid newAsteroid = Asteroid.randomAsteroid(WIDTH, HEIGHT);
							if (level > 3 && level < 10) { newAsteroid.setMultiplier(level * 0.5); }
							asteroids.add(newAsteroid);
                        }

						// Clear Black Holes and Respawn
						// Existing holes retain their lifetime across level boundaries.

						configureLevelHazards();
                    } 
				} else {
					if (asteroids.size() + asteroidsToAdd.size() < MAX_ASTEROIDS
							&& rand.nextInt(400) < Math.min(level, 12)) {
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
				// Galaxy background update
				galaxyBackground.update();
				// Ship trail update
				ship.getTrail().update();

				// Ability effects.

				// Update ability systems
				abilityManager.update();
				if (teleportAnchor.update(ship, teleportAnchor.isPending()
						? teleportObstacles() : null, WIDTH, HEIGHT)) {
                    teleportFlashFrame = 15;
                    abilityManager.startTeleportCooldown();
                }
				floatingTextManager.update();
				if (chargingBlackHole) {
					blackHoleChargeFrames = Math.min(150, blackHoleChargeFrames + 1);
					blackHoleLaunchAngle = ship.getAngle();
				}

				// Update cooldowns
				if (voidCooldownTimer > 0) voidCooldownTimer--;

				// Sync ability manager with actual states
				
				abilityManager.setVoidActive(ship.getVoidEnergy().isActive());
				abilityManager.setTeleportActive(teleportAnchor.isPending());

				// Update teleport flash
				if (teleportFlashFrame > 0) teleportFlashFrame--;

				// Update visual effects
                starField.setVoidMode(ship.getVoidEnergy().isActive());
                starField.update(ship.getX() - WIDTH / 2.0, ship.getY() - HEIGHT / 2.0,
                        ship.getVelocity() * Math.sin(ship.getAngle()),
                        -ship.getVelocity() * Math.cos(ship.getAngle()));
                particleSystem.update(ship.getX(), ship.getY());
                
                // Update void energy
                ship.getVoidEnergy().update();
                // Deactivate void mode if energy depleted
                if (ship.getVoidEnergy().getEnergy() <= 0) { abilityManager.setVoidActive(false); }

                // Thrust & Rotation
                if (thrusting) ship.applyThrust();
                if (rotatingLeft) ship.rotateLeft();
                if (rotatingRight) ship.rotateRight();
                ship.updatePhysics();
				if (shooting && --shootIntervalRemaining <= 0) {
					handleShoot();
					shootIntervalRemaining = SHOOT_INTERVAL_FRAMES;
				}
                
				updatePassiveShield();
				if (!teleportAnchor.isProtected() && ship.getVoidEnergy().isOverThreshold()) {
					voidDeath = true;
					startDeathSequence();
					return;
				}

				// ====== Collisions & Physics ======
				// Spawn creatures by size and entities by weight
				if (!levelStarting) trySpawnCreatures();
				if (!levelStarting) trySpawnEntities();
				updateBlackHoles();
				for (BlackHole hole : blackHoles) {
					if (blackHolesToRemove.contains(hole)) continue;
					consumeBlackHoleContacts(hole);
					if (!hole.isPlayerOwned()) {
						Point2D.Double gravity = hole.applyGravity(ship.getX(), ship.getY());
						ship.setX(ship.getX() + gravity.x);
						ship.setY(ship.getY() + gravity.y);
						if (hole.canAbsorb(utilitySystem.level(UtilitySystem.MODULATOR))
								&& Math.hypot(hole.getX() - ship.getX(), hole.getY() - ship.getY())
								<= hole.getEventHorizonRadius() + 42) {
							blackHolesToRemove.add(hole);
							absorbedBlackHoles++;
							ship.setInfiniteFuel(true);
							particleSystem.createBlackHoleAbsorption(hole.getX(), hole.getY());
							continue;
						}
					}
					if (!hole.isPlayerOwned() && !teleportAnchor.isProtected()
							&& hole.touches(ship.getBounds())) {
						blackHoleDeath = true;
						startDeathSequence();
						return;
					}
				}

				// Update void creatures 
				for (VoidCreature vc : voidCreatures) {
					if (creaturesToRemove.contains(vc)) continue;
					if (vc.isDead()) { destroyVoidCreature(vc); continue; }
					double previousX = vc.getX(), previousY = vc.getY();
					vc.update(ship.getX(), ship.getY(), ship.getVoidEnergy().isActive());
					double dx = vc.getX() - previousX, dy = vc.getY() - previousY;
					double scale = utilitySystem.metricSpeedScale(previousX, previousY, dx, dy,
							ship.getX(), ship.getY());
					vc.moveBy((scale - 1) * dx, (scale - 1) * dy);
					if (touchesBlackHole(vc.getBounds())) { consumeVoidCreature(vc); continue; }
					if (abilityManager.isShieldReady()
							&& circleTouches(ship.getX(), ship.getY(),
								abilityManager.getShieldBoundaryRadius(), vc.getBounds())) updatePassiveShield();
					if (creaturesToRemove.contains(vc)) continue;
					
					if (!teleportAnchor.isProtected() && ship.getVoidEnergy().isActive() && vc.intersects(ship.getBounds())) {
						voidCreatureDeath = true;
						startDeathSequence();
						return;
					}
				}

				// Update cosmic entities
				for (CosmicEntity ce : cosmicEntities) {
					if (cosmicEntitiesToRemove.contains(ce)) continue;
					if (ce.isDead()) { destroyCosmicEntity(ce); continue; }
					double previousX = ce.getX(), previousY = ce.getY();
					ce.update(ship.getX(), ship.getY(), ship.getVoidEnergy().isActive());
					double dx = ce.getX() - previousX, dy = ce.getY() - previousY;
					double scale = utilitySystem.metricSpeedScale(previousX, previousY, dx, dy,
							ship.getX(), ship.getY());
					ce.moveBy((scale - 1) * dx, (scale - 1) * dy);
					if (touchesBlackHole(ce.getBounds())) { consumeCosmicEntity(ce); continue; }
					if (abilityManager.isShieldReady()
							&& circleTouches(ship.getX(), ship.getY(),
								abilityManager.getShieldBoundaryRadius(), ce.getBounds())) updatePassiveShield();
					if (cosmicEntitiesToRemove.contains(ce)) continue;
					
					if (!teleportAnchor.isProtected() && !ship.getVoidEnergy().isActive() && ce.isNearShip(ship.getBounds())) {
						cosmicEntityDeath = true;
						startDeathSequence();
						return;
					}
				}

				// Asteroid movement and ship collision.
				for (Asteroid asteroid : asteroids) {
					if (asteroidsToRemove.contains(asteroid)) continue;
					double previousX = asteroid.getX(), previousY = asteroid.getY();
					asteroid.update();
					double dx = asteroid.getX() - previousX, dy = asteroid.getY() - previousY;
					double scale = utilitySystem.metricSpeedScale(previousX, previousY, dx, dy,
							ship.getX(), ship.getY());
					asteroid.moveBy((scale - 1) * dx, (scale - 1) * dy);
					if (touchesBlackHole(asteroid.getBoundingBox())) { consumeAsteroid(asteroid); continue; }
					if (abilityManager.isShieldReady()
							&& circleTouches(ship.getX(), ship.getY(),
								abilityManager.getShieldBoundaryRadius(), asteroid.getBoundingBox())) updatePassiveShield();
					if (asteroidsToRemove.contains(asteroid)) continue;
					if (asteroid.hasExited(WIDTH, HEIGHT)) {
						asteroidsToRemove.add(asteroid);
						continue;
					}
				if (!teleportAnchor.isProtected() && !ship.getVoidEnergy().isActive()) {
						Rectangle bounds = asteroid.getBoundingBox();
						double quickDist = Math.abs(bounds.getCenterX() - ship.getX())
								+ Math.abs(bounds.getCenterY() - ship.getY());
						if (quickDist < 100 && asteroid.intersects(ship.getBounds())) {
							asteroidDeath = true;
							startDeathSequence();
							return;
						}
					}
				}
				updateDrones();
				updateSwarm();

                // Void laser collisions.
                for (VoidLaser laser : voidLasers) {
                    if (lasersToRemove.contains(laser)) continue;
                    laser.update();
                    if (!laser.isAlive()) {
                        lasersToRemove.add(laser);
                        continue;
                    }
                    if (!laser.isSpent() && clipLaserToBlackHole(laser)) continue;
                    if (laser.isSpent()) continue;
                    for (VoidCreature creature : voidCreatures) {
                        if (creaturesToRemove.contains(creature)) continue;
                        if (creature.intersects(laser.getLaserLine())) {
                            creature.takeDamage(50);
                            if (creature.isDead()) destroyVoidCreature(creature);
                            laser.markSpent();
                            break;
                        }
                    }
                    if (laser.isSpent()) continue;
                    for (CosmicEntity entity : cosmicEntities) {
                        if (cosmicEntitiesToRemove.contains(entity)) continue;
                        if (laser.getLaserLine().intersects(entity.getBounds())) {
                            entity.takeDamage(50);
                            if (entity.isDead()) destroyCosmicEntity(entity);
                            laser.markSpent();
                            break;
                        }
                    }
                    if (laser.isSpent() || ship.getVoidEnergy().isActive()) continue;
                    for (Asteroid asteroid : asteroids) {
                        if (asteroidsToRemove.contains(asteroid)) continue;
                        if (laser.intersects(asteroid.getBounds())) {
                            destroyAsteroid(asteroid, true);
                            laser.markSpent();
                            break;
                        }
                    }
                }

                // Each bullet hits an object only once and survives its purchased hit count.
                for (Bullet bullet : bullets) {
                    if (bulletsToRemove.contains(bullet)) continue;
                    bullet.update(WIDTH, HEIGHT);
                    if (!bullet.isAlive()) {
                        bulletsToRemove.add(bullet);
                        continue;
                    }
                    boolean swallowed = false;
					for (BlackHole hole : blackHoles) {
						if (hole.isPlayerOwned() || blackHolesToRemove.contains(hole)
								|| !hole.touches(bullet.getBoundingBox())) continue;
						bulletsToRemove.add(bullet);
						particleSystem.createBulletExplosion(bullet.getBoundingBox().getCenterX(),
								bullet.getBoundingBox().getCenterY());
						hole.grow(0.5);
						swallowed = true;
						break;
					}
					if (swallowed) continue;
                    boolean hitThisTick = false;
                    for (VoidCreature creature : voidCreatures) {
                        if (creaturesToRemove.contains(creature) || !bullet.canHit(creature)) continue;
                        if (creature.intersects(bullet.getBoundingBox())) {
                            creature.takeDamage(25);
                            if (creature.isDead()) destroyVoidCreature(creature);
                            if (!bullet.registerHit(creature)) bulletsToRemove.add(bullet);
                            hitThisTick = true;
                            if (!bullet.isPiercing()) break;
                        }
                    }
                    if (bulletsToRemove.contains(bullet) || (hitThisTick && !bullet.isPiercing())) continue;
                    for (CosmicEntity entity : cosmicEntities) {
                        if (cosmicEntitiesToRemove.contains(entity) || !bullet.canHit(entity)) continue;
                        if (bullet.getBoundingBox().intersects(entity.getBounds())) {
                            entity.takeDamage(25);
                            if (entity.isDead()) destroyCosmicEntity(entity);
                            if (!bullet.registerHit(entity)) bulletsToRemove.add(bullet);
                            hitThisTick = true;
                            if (!bullet.isPiercing()) break;
                        }
                    }
                    if (bulletsToRemove.contains(bullet) || (hitThisTick && !bullet.isPiercing())) continue;
                    for (Asteroid asteroid : asteroids) {
                        if (asteroidsToRemove.contains(asteroid) || !bullet.canHit(asteroid)) continue;
                        if (asteroid.getBounds().intersects(bullet.getBoundingBox())) {
                            destroyAsteroid(asteroid, true);
                            if (!bullet.registerHit(asteroid)) bulletsToRemove.add(bullet);
                            hitThisTick = true;
                            if (!bullet.isPiercing()) break;
                        }
                    }
                }

				// ------ Nova Blast Collisions ------
				// Nova Blast Destroying Asteroids
				if (abilityManager.isNovaExploding()) {
					double novaRadius = abilityManager.getNovaExplosionRadius();
					for (Asteroid asteroid : asteroids) {
						if (asteroidsToRemove.contains(asteroid)) continue;
						if (circleTouches(ship.getX(), ship.getY(), novaRadius,
								asteroid.getBoundingBox())) destroyAsteroid(asteroid, false);
					}

					// Nova Blast Destroying Void Creatures
					for (VoidCreature vc : voidCreatures) {
						if (creaturesToRemove.contains(vc)) continue;
						if (vc.isDead()) { destroyVoidCreature(vc); continue; }
						double dx = vc.getX() - ship.getX();
						double dy = vc.getY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						if (dist < novaRadius) {
							destroyVoidCreature(vc);
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
						if (ce.isDead()) { destroyCosmicEntity(ce); continue; }
						double dx = ce.getX() - ship.getX();
						double dy = ce.getY() - ship.getY();
						double dist = Math.sqrt(dx * dx + dy * dy);
						if (dist < novaRadius) {
							destroyCosmicEntity(ce);
						}
					}
					
					// Consume void energy
					if (abilityManager.getNovaExplosionRadius() > 50) { // First frame
						ship.getVoidEnergy().reset();
					}
				}
				// Check Fuel
				if (!ship.isInfiniteFuel() && ship.getFuel() > 1000) {
					ship.setFuel(1000);
				}
                
				applyPendingChanges();

            	
            }
            case "death" -> {
                deathCountdown--;
                if (deathCountdown <= 0) {
	                    changeScene("menu");
                    setupMenu();
					return;
                }
                // Death explosion animation
	                if (!deathExplosionDone && deathExplosionFrame < 45) {
	                    if (deathExplosionFrame % 15 == 0) {
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
				} else if (showingInstructions) {
					instructionsPanel.draw(g2, WIDTH, HEIGHT);
				} else if (showingSettings) {
					settingsPanel.draw((Graphics2D)g, controlConfig);
				} else {
					// Buttons
					drawNeonMenuButton(g2, "NEW GAME", menuButtonY(0), hoveredButton == 0, true);
					drawNeonMenuButton(g2, "RESUME", menuButtonY(1), hoveredButton == 1, !noSavedGame);
					drawNeonMenuButton(g2, "INSTRUCTIONS", menuButtonY(2), hoveredButton == 2, true);
					drawNeonMenuButton(g2, "SETTINGS", menuButtonY(3), hoveredButton == 3, true);
					drawNeonMenuButton(g2, "STORY", menuButtonY(4), hoveredButton == 4, true);
					
					// Error message
					if (noSavedGame) {
						g2.setFont(new Font("Courier New", Font.PLAIN, 24));
						int pulse = (int)(200 + 55 * Math.sin(System.currentTimeMillis() / 200.0));
						
						// Neon glow effect
						g2.setColor(new Color(255, 0, 100, pulse / 3));
						String errorMessage = "⚠ NO SAVED PROGRESS FOUND";
						int errorX = WIDTH / 2 - g2.getFontMetrics().stringWidth(errorMessage) / 2;
						g2.drawString(errorMessage, errorX - 2, menuButtonY(0) - 24 - 2);
						g2.drawString(errorMessage, errorX + 2, menuButtonY(0) - 24 + 2);
						
						g2.setColor(new Color(0, 255, 255, pulse));
						g2.drawString(errorMessage, errorX, menuButtonY(0) - 24);
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
				if (ship.getVoidEnergy().isActive()) {
					drawVoidScreenOverlay(g2);
				}
				
				// Draw void creatures
				for (VoidCreature vc : voidCreatures) { vc.draw(g2, ship.getVoidEnergy().isActive()); }

				// Draw black holes
			for (BlackHole bh : blackHoles) {
				bh.draw(g2, bh.canAbsorb(utilitySystem.level(UtilitySystem.MODULATOR)));
			}

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

				// Draw drone
				abilityManager.drawDrone(g2, ship.getX(), ship.getY());
				for (DroneSwarm.Drone drone : abilityManager.getDrones()) {
					weaponSystem.drawNozzles(g2, drone.getX(), drone.getY(), drone.getAngle(), 0.58);
				}

				// Draw nova blast
				abilityManager.drawNovaBlast(g2, WIDTH, HEIGHT, ship.getX(), ship.getY());

				// Draw floating texts
				floatingTextManager.draw(g2);

				// Draw teleport flash
				if (teleportFlashFrame > 0) { teleportAnchor.drawTeleportFlash(g2, WIDTH, HEIGHT, teleportFlashFrame); }

				// Draw level up effect
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
				utilitySystem.drawMetricRings(g2, ship.getX(), ship.getY());
				utilitySystem.drawSwarm(g2);
				drawBlackHoleCharge(g2);
				weaponSystem.drawNozzles(g2, ship.getX(), ship.getY(), ship.getAngle(), 1.0,
						!ship.getVoidEnergy().isActive() && ship.getVoidEnergy().getEnergy() <= 0);

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
				String[] slotKeys = new String[AbilityLoadout.SLOT_COUNT];
				for (int slot = 0; slot < slotKeys.length; slot++)
					slotKeys[slot] = controlConfig.getKeyName(controlConfig.getKey("ability" + (slot + 1)));
				abilityManager.drawAbilityBar(g2, WIDTH, HEIGHT, abilityLoadout,
						controlConfig.getKeyName(controlConfig.getKey("void")), slotKeys);
				utilitySystem.drawStatusPanel(g2, WIDTH, HEIGHT, abilityManager);
				
				// Enhanced HUD
				drawEnhancedHUD(g2);
				
				// Level starting countdown
				if (levelStarting) {
					drawLevelTransition(g2);
				}
				
				drawBlackHoleWarning(g2);
				drawConstructButton(g2);
				if (showingConstruct && !showingDeveloperTerminal) constructPanel.draw(g2, WIDTH, HEIGHT,
						constructModel(), controlConfig);
				if (showingDeveloperTerminal) developerTerminal.draw(g2, WIDTH, HEIGHT);
				if (shortcutMessageTicks > 0 && !showingConstruct && !showingDeveloperTerminal)
					drawShortcutMessage(g2);
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
					"Incident Essence: " + incidentEssence,
					"M-AM: " + (ship.isInfiniteFuel() ? "∞" : (int) ship.getFuel())
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
				String resumeText = "Press ENTER to Resume";
				FontMetrics metrics = g.getFontMetrics(resumeFont);
				g.drawString(resumeText, WIDTH / 2 - metrics.stringWidth(resumeText) / 2, panelY + panelHeight - 40);
				String menuText = "Press ESC to Return to Menu";
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
				} else if (blackHoleDeath) {
					deathCause = "CONSUMED BY BLACK HOLE";
					causeColor = new Color(0, 0, 0);
				} else if (cosmicEntityDeath) {
					deathCause = "CONSUMED BY COSMIC ENTITY";
					causeColor = new Color(255, 0, 255);
				} else if (voidCreatureDeath) {
					deathCause = "CONSUMED BY VOID CREATURE";
					causeColor = new Color(130, 190, 255);
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
				
				// Mission summary uses the same translucent panel language as the level and ability HUD.
				int panelWidth = Math.min(420, WIDTH - 40);
				int panelHeight = 230;
				int panelX = (WIDTH - panelWidth) / 2;
				int panelY = Math.max(215, HEIGHT / 2 - 35);
				drawHudChip(g2, panelX, panelY, panelWidth, panelHeight, new Color(100, 170, 230));

				g2.setFont(new Font("SansSerif", Font.BOLD, 18));
				g2.setColor(Color.WHITE);
				g2.drawString("RUN SUMMARY", panelX + 24, panelY + 30);
				g2.setColor(new Color(100, 170, 230, 130));
				g2.drawLine(panelX + 24, panelY + 43, panelX + panelWidth - 24, panelY + 43);

				String[] labels = {
					"LEVEL REACHED", "LARGE ASTEROIDS", "MEDIUM ASTEROIDS",
					"SMALL ASTEROIDS", "INCIDENT ESSENCE", "M-AM LEFT"
				};
				String[] values = {
					Integer.toString(level), Integer.toString(largeDestroyed),
					Integer.toString(mediumDestroyed), Integer.toString(smallDestroyed),
					Integer.toString(incidentEssence), ship.isInfiniteFuel() ? "∞" : Integer.toString((int) ship.getFuel())
				};
				g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
				FontMetrics statMetrics = g2.getFontMetrics();
				for (int i = 0; i < labels.length; i++) {
					int rowY = panelY + 68 + i * 27;
					g2.setColor(new Color(205, 218, 235));
					g2.drawString(labels[i], panelX + 24, rowY);
					g2.setColor(i == 4 ? new Color(205, 150, 255) : Color.WHITE);
					g2.drawString(values[i], panelX + panelWidth - 24 - statMetrics.stringWidth(values[i]), rowY);
				}

				String countdown = "MENU IN " + Math.max(0, (deathCountdown + 59) / 60) + "s  |  ENTER TO SKIP";
				g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
				g2.setColor(new Color(205, 218, 235));
				FontMetrics countdownMetrics = g2.getFontMetrics();
				g2.drawString(countdown, (WIDTH - countdownMetrics.stringWidth(countdown)) / 2, HEIGHT - 34);
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

	private void drawBlackHoleWarning(Graphics2D g2) {
		for (BlackHole blackHole : blackHoles) {
			if (!blackHole.isWarningActive()) continue;
			String warning = "WARNING: BLACK HOLE IN " + blackHole.getWarningSecondsRemaining() + "s";
			Font originalFont = g2.getFont();
			g2.setFont(BLACK_HOLE_WARNING_FONT);
			int textWidth = g2.getFontMetrics().stringWidth(warning);
			int x = (WIDTH - textWidth) / 2;
			g2.setColor(new Color(18, 8, 20, 220));
			g2.fillRoundRect(x - 14, 63, textWidth + 28, 30, 10, 10);
			g2.setColor(new Color(255, 145, 85));
			g2.drawRoundRect(x - 14, 63, textWidth + 28, 30, 10, 10);
			g2.drawString(warning, x, 84);
			g2.setFont(originalFont);
			return;
		}
	}

	private void drawHudChip(Graphics2D g2, int x, int y, int width, int height, Color accent) {
		g2.setColor(new Color(20, 30, 50, 100));
		g2.fillRoundRect(x, y, width, height, 10, 10);
		g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
		g2.setStroke(new BasicStroke(1.5f));
		g2.drawRoundRect(x, y, width, height, 10, 10);
		g2.setStroke(new BasicStroke(1));
	}

	private void drawShortcutMessage(Graphics2D g2) {
		Font oldFont = g2.getFont();
		g2.setFont(new Font("SansSerif", Font.BOLD, 13));
		FontMetrics metrics = g2.getFontMetrics();
		int width = metrics.stringWidth(shortcutMessage) + 28;
		int x = (WIDTH - width) / 2;
		int y = HEIGHT - 124;
		g2.setColor(new Color(15, 23, 37, 230));
		g2.fillRoundRect(x, y, width, 27, 9, 9);
		g2.setColor(shortcutMessageError ? new Color(255, 129, 139)
				: new Color(145, 225, 175));
		g2.drawString(shortcutMessage, x + 14, y + 18);
		g2.setFont(oldFont);
	}

	private void drawConstructButton(Graphics2D g2) {
		Rectangle button = constructButtonBounds();
		drawHudChip(g2, button.x, button.y, button.width, button.height,
				new Color(130, 216, 232));
		int cx = button.x + button.width / 2;
		int cy = button.y + 24;
		g2.setColor(new Color(160, 230, 242));
		g2.setStroke(new BasicStroke(2.0f));
		g2.drawLine(cx - 11, cy + 6, cx + 11, cy - 6);
		g2.drawLine(cx - 11, cy - 6, cx + 11, cy + 6);
		g2.drawOval(cx - 4, cy - 4, 8, 8);
		g2.setStroke(new BasicStroke(1));
		g2.setFont(new Font("SansSerif", Font.BOLD, 9));
		g2.drawString("CONSTRUCT", button.x + 14, button.y + 48);
		g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
		g2.drawString("C", cx - 3, button.y + 62);
	}

	private void drawEssenceChip(Graphics2D g2) {
		int x = WIDTH - 132;
		int y = 12;
		drawHudChip(g2, x, y, 120, 36, new Color(178, 120, 235));
		Polygon crystal = new Polygon(
			new int[] {x + 15, x + 23, x + 19, x + 11},
			new int[] {y + 10, y + 18, y + 27, y + 18}, 4);
		g2.setColor(new Color(168, 95, 235, 205));
		g2.fillPolygon(crystal);
		g2.setColor(new Color(225, 195, 255));
		g2.drawPolygon(crystal);
		g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
		g2.drawString("INCIDENT", x + 32, y + 14);
		g2.setFont(new Font("SansSerif", Font.BOLD, 15));
		g2.drawString(Integer.toString(incidentEssence), x + 32, y + 30);
	}

	private void drawEnhancedHUD(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (!levelStarting) {
			drawHudChip(g2, 12, 12, 112, 36, new Color(100, 170, 230));
			g2.setFont(new Font("SansSerif", Font.BOLD, 16));
			g2.setColor(Color.WHITE);
			g2.drawString("LEVEL " + level, 25, 36);

			int timerX = WIDTH / 2 - 62;
			drawHudChip(g2, timerX, 12, 124, 36, new Color(100, 170, 230));
			int secondsLeft = Math.max(0, (levelTimer + 59) / 60);
			String timeText = (secondsLeft / 60) + ":"
					+ (secondsLeft % 60 < 10 ? "0" : "") + (secondsLeft % 60);
			g2.setFont(new Font("Monospaced", Font.BOLD, 21));
			g2.setColor(new Color(230, 238, 248));
			FontMetrics timerMetrics = g2.getFontMetrics();
			g2.drawString(timeText, WIDTH / 2 - timerMetrics.stringWidth(timeText) / 2, 36);
			g2.setColor(new Color(45, 65, 85, 140));
			g2.fillRoundRect(timerX + 11, 42, 102, 2, 2, 2);
			g2.setColor(new Color(100, 200, 255, 180));
			g2.fillRoundRect(timerX + 11, 42,
					Math.max(0, Math.min(102, levelTimer * 102 / (30 * 60))), 2, 2, 2);

			int fuelX = WIDTH - 264;
			drawHudChip(g2, fuelX, 12, 120, 36, new Color(225, 165, 100));
			g2.setFont(new Font("SansSerif", Font.BOLD, 13));
			g2.setColor(new Color(255, 220, 175));
			g2.drawString("M-AM " + (ship.isInfiniteFuel() ? "∞" : (int) ship.getFuel()), fuelX + 10, 35);
		}
		drawEssenceChip(g2);
		ship.getVoidEnergy().drawBar(g2, WIDTH, HEIGHT);
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
				if (!teleportAnchor.isProtected() && a.intersects(ship.getBounds())) {
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
		g2.setColor(new Color(90, 10, 130, 28));
		g2.fillRect(0, 0, WIDTH, HEIGHT);
		g2.setFont(new Font("Monospaced", Font.BOLD, 12));
		g2.setColor(new Color(195, 145, 245, 150));
		g2.drawString("VOID", 24, 92);
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
		if (ability == null || !abilityManager.isEnabled(ability)) return;
		switch (ability) {
			case "blackhole" -> {
				if (!chargingBlackHole && !blackHoleChargeCanceled && abilityManager.canLaunchBlackHole()) {
					chargingBlackHole = true;
					blackHoleChargeFrames = 0;
					blackHoleChargeSlot = slot;
					blackHoleLaunchAngle = ship.getAngle();
				}
			}
			case "teleport" -> {
				if (abilityManager.getLevel("teleport") > 0) {
					teleportAnchor.beginTeleport(ship, teleportObstacles(), WIDTH, HEIGHT,
							abilityManager.getLevel("teleport"));
				}
			}
			case "drone" -> abilityManager.deployDrone(ship);
			case "nova" -> abilityManager.activateNova(ship.getVoidEnergy());
			default -> { }
		}
	}

	private double blackHoleChargeDistance() {
		return Math.min(820, 80 + blackHoleChargeFrames * 5.0);
	}

	private void releaseBlackHoleCharge() {
		if (!chargingBlackHole) return;
		chargingBlackHole = false;
		blackHoles.add(BlackHole.fired(ship.getX(), ship.getY(),
				Math.min(110, 34 + absorbedBlackHoles * 8), blackHoleLaunchAngle,
				blackHoleChargeDistance()));
		abilityManager.startBlackHoleCooldown();
		blackHoleChargeSlot = -1;
	}

	private void drawBlackHoleCharge(Graphics2D g2) {
		if (!chargingBlackHole) return;
		Graphics2D g = (Graphics2D) g2.create();
		try {
			g.setColor(new Color(180, 105, 250));
			g.setStroke(new BasicStroke(2.5f));
			int shipX = (int) ship.getX(), shipY = (int) ship.getY();
			g.drawArc(shipX - 92, shipY - 92, 184, 184, 70,
					(int) Math.min(290, blackHoleChargeFrames * 2));
			double distance = blackHoleChargeDistance();
			int markerX = (int) (((ship.getX() + Math.sin(blackHoleLaunchAngle) * distance) % WIDTH + WIDTH) % WIDTH);
			int markerY = (int) (((ship.getY() - Math.cos(blackHoleLaunchAngle) * distance) % HEIGHT + HEIGHT) % HEIGHT);
			g.drawOval(markerX - 13, markerY - 13, 26, 26);
			g.drawLine(markerX - 20, markerY, markerX - 8, markerY);
			g.drawLine(markerX + 8, markerY, markerX + 20, markerY);
			g.drawLine(markerX, markerY - 20, markerX, markerY - 8);
			g.drawLine(markerX, markerY + 8, markerX, markerY + 20);
		} finally { g.dispose(); }
	}

	private void activateVoidAbility() {
		if (voidCooldownTimer > 0) return;
		toggleVoidWorld();
		if (!ship.getVoidEnergy().isActive()) {
			voidCooldownTimer = voidMaxCooldown;
			abilityManager.startVoidCooldown();
		}
	}

	private void handleShoot() {
		if (ship.getVoidEnergy().isActive() || ship.getVoidEnergy().getEnergy() > 0) {
			int firstLaser = voidLasers.size();
			weaponSystem.fireVoidLasers(voidLasers, ship.getX(), ship.getY(),
					ship.getAngle(), 1.0, WIDTH, HEIGHT);
			for (int i = firstLaser; i < voidLasers.size(); i++)
				clipLaserToBlackHole(voidLasers.get(i));
			if (voidLasers.size() > firstLaser) audio.playLaser();
			if (ship.getVoidEnergy().isActive()) ship.getVoidEnergy().absorbForAction(2.0);
			else ship.getVoidEnergy().dissipateForAction(5.0);
		} else {
			int firstBullet = bullets.size();
			weaponSystem.fire(bullets, ship.getX(), ship.getY(), ship.getAngle(), 1.0);
			if (bullets.size() > firstBullet) audio.playFire();
		}
	}

	private void setHyperMode(boolean active) {
		ship.setHyper(active);
		audio.setHyperActive(active);
	}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (gameState) {
            case "menu" -> {
				if (showingStory) {
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) exitStory();
					else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						storyTerminal.advanceLine();
						if (storyTerminal.isExitRequested()) exitStory();
					}
					repaint();
					return;
				}
				if (showingInstructions) {
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) showingInstructions = false;
					return;
				}
				if (showingSettings) {
					settingsPanel.handleKeyPress(e.getKeyCode());
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						showingSettings = false;
						repaint();
					}
					return;
				}
				
				int key = e.getKeyCode();
				switch (key) {
					case KeyEvent.VK_ENTER -> {
						audio.playClick();
						startNewGame();
					}
					case KeyEvent.VK_S -> {
						if (noSavedGame) break;
						audio.playClick();
						resumeGame();
                	}
				}
            }
			case "playing" -> {
				int key = e.getKeyCode();
				if (showingDeveloperTerminal) {
					if (key == KeyEvent.VK_ESCAPE) {
						showingDeveloperTerminal = false;
						gameClock.reset(System.nanoTime());
					} else developerTerminal.keyPressed(key);
					repaint();
					return;
				}
				if (key == KeyEvent.VK_T && controlConfig.isDeveloperTerminalEnabled()) {
					showingDeveloperTerminal = true;
					chargingBlackHole = false;
					blackHoleChargeSlot = -1;
					suppressTerminalOpenTyped = true;
					thrusting = rotatingLeft = rotatingRight = false;
					setHyperMode(false);
					gameClock.reset(System.nanoTime());
					repaint();
					return;
				}
				if (handleConstructionShortcut(e)) return;
				if (key == KeyEvent.VK_C) {
					toggleConstruct();
					return;
				}
				if (showingConstruct) {
					applyConstructAction(constructPanel.handleKey(key, constructModel(), controlConfig));
					repaint();
					return;
				}
				if (key == KeyEvent.VK_TAB && chargingBlackHole) {
					chargingBlackHole = false;
					blackHoleChargeCanceled = true;
					repaint();
					return;
				}
				if (key == controlConfig.getKey("thrust")) thrusting = true;
				else if (key == controlConfig.getKey("left")) rotatingLeft = true;
				else if (key == controlConfig.getKey("right")) rotatingRight = true;
				else if (key == controlConfig.getKey("hyper")) setHyperMode(true);
				else if (key == controlConfig.getKey("shoot")) {
					if (!shooting) {
						shooting = true;
						handleShoot();
						shootIntervalRemaining = SHOOT_INTERVAL_FRAMES;
					}
				}
				else if (key == controlConfig.getKey("void")) activateVoidAbility();
				else if (key == controlConfig.getKey("ability1")) activateAbility(0);
				else if (key == controlConfig.getKey("ability2")) activateAbility(1);
				else if (key == controlConfig.getKey("ability3")) activateAbility(2);
				else if (key == controlConfig.getKey("ability4")) activateAbility(3);
				else if (key == KeyEvent.VK_ESCAPE) {
					changeScene("paused");
                    gameClock.reset(System.nanoTime());
                    noSavedGame = false;
                    saveGame();
				}
			}
            case "paused" -> {
				int key = e.getKeyCode();
                switch (key) {
                    case KeyEvent.VK_ENTER -> {
                        changeScene("playing");
                        gameClock.reset(System.nanoTime());
                    }
					case KeyEvent.VK_ESCAPE -> {
						changeScene("menu");
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
			if (blackHoleChargeSlot >= 0
					&& key == controlConfig.getKey("ability" + (blackHoleChargeSlot + 1))) {
				if (chargingBlackHole && !blackHoleChargeCanceled) releaseBlackHoleCharge();
				blackHoleChargeCanceled = false;
				blackHoleChargeSlot = -1;
			}
	        if (key == controlConfig.getKey("thrust")) thrusting = false;
	        else if (key == controlConfig.getKey("left")) rotatingLeft = false;
	        else if (key == controlConfig.getKey("right")) rotatingRight = false;
	        else if (key == controlConfig.getKey("hyper")) setHyperMode(false);
			else if (key == controlConfig.getKey("shoot")) shooting = false;
    	}
    }

	@Override
    public void keyTyped(KeyEvent e) {
		if (gameState.equals("playing") && showingDeveloperTerminal) {
			if (suppressTerminalOpenTyped) {
				suppressTerminalOpenTyped = false;
				return;
			}
			developerTerminal.type(e.getKeyChar());
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

}
