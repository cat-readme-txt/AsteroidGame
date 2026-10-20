# VoidNavigator mechanics and stats

These are the current gameplay values for the desktop build. The simulation targets 60 ticks per second; times below convert frames at that rate. Distances are screen pixels. Controls can be rebound in **Settings → Key bindings**.

## Objective, levels, and resources

Survive successive timed levels. Each level has a 30-second active period and a 3-second level transition. The level starts with `min(2 + floor((level − 1) / 3), 5)` asteroids. During active play, each tick has an additional asteroid spawn chance of `min(level, 12) / 400`, subject to a 24-asteroid cap. Asteroids spawn beyond an edge and head inward at roughly 1–2.5 pixels per tick. Starting asteroids on levels 4–9 receive a `level × 0.5` speed multiplier. The ship, bullets, and black holes wrap at screen edges.

| Resource | Starting value | Maximum and gain | Use or consequence |
| --- | ---: | --- | --- |
| M-AM fuel | 500 | 1,000; asteroid kills give 20/10/5 for large/medium/small | Thrust spends 1 per tick, or 4 in hyper mode. Fuel at zero stops sustained thrust. |
| Score | 0 | Large/medium/small asteroid kills give 20/10/5 points | Highest score is retained. |
| Incident Essence | 0 | Defeating a weight 1/2/3 cosmic entity or Void creature gives a random 1–2 / 1–3 / 1–5 Essence | Pay 5 per weapon level or 10 per ability or utility level. |
| Void energy | 0 | 100 | At 100, the run ends unless a charged Shield clears the energy. |

Ship bullets, drone bullets, Swarm blasts, Nova, Shield, and Void lasers fired outside the Void all use the same asteroid reward path. Swarm blasts also award M-AM and score when they catch fragments that split during the same tick. Asteroids consumed by a black hole are removed without a fuel or score reward. A maxed Energy Modulator can instead absorb a weakened natural black hole and grant infinite M-AM for that run.

The regular and Void enemy populations are measured separately by **weight**, not by a fixed count. Each starts spawning at level 3 with a target total weight of `min(level − 2, 10)`; each population can add up to two enemies every 60 ticks while below that target. Each enemy weighs 1, 2, or 3 according to its size.

## Flight and combat

| Stat | Value |
| --- | --- |
| Thrust acceleration | 0.05 per tick; hyper multiplies thrust by 4 |
| Rotation | 1° per tick; hyper multiplies turning by 8.5 |
| Speed clamp | Each velocity axis is limited to ±2.5 normally or ±8 in hyper mode |
| Void energy thrust bonus | If any Void energy remains, thrust gains a further 20% acceleration |
| Shot repeat | Holding Fire shoots once every 8 ticks after the first shot |
| Ordinary bullet | 10 pixels/tick, 180-tick life (3 seconds), 25 damage to enemies |
| Bullet radius | 3 pixels normally; 5, 7, or 9 with Bullet Enlargement levels 1–3 |
| Void laser | Screen-diagonal reach, 10-tick visual life, 50 damage to enemies |

While Void is active, the ship can pass through asteroids, but Void creatures can kill it. Outside the Void, cosmic entities and asteroids can kill on contact. Firing while Void is active or while stored Void energy remains uses Void lasers; otherwise it uses bullets. Outside the Void, a laser shot consumes 5 stored energy. In the Void, it adds 2 energy before Energy Modulator effects. Lasers shrink a black hole by 6 size units per hit. Nova and Shield destroy asteroids without splitting them; bullets and lasers outside the Void split large and medium asteroids into two of the next smaller size.

Void energy rises by 0.1 per tick while in the Void, with action-based gain able to accelerate up to 3×. It falls by 0.05 per tick outside the Void. Thrusting or turning uses 0.1 energy per action outside the Void; firing a laser there uses 5. Toggling out of the Void starts a 3-second cooldown. Energy reaching 100 is fatal unless Shield is charged.

## Construction and shortcuts

Press **C**, then choose **1 Utilities**, **2 Weapons**, **3 Abilities**, or **4 Return to Humanity**. The Return page is a placeholder. Each weapon, ability, and utility has three construction levels. New abilities and utilities must be assigned to an empty slot; ability slots and utility slots are separate groups of four. The utility systems run passively once constructed.

**Ctrl+A** opens the armory even without Essence. **Ctrl + Q/W/E/R** upgrades the equipped ability in the corresponding slot for 10 Essence. If that slot is empty, it opens the ability construction page regardless of Essence. An unaffordable upgrade displays an insufficient Essence message. The construction pages show their shortcuts. Default direct construction keys on the Abilities page are **1 Teleport**, **2 Drone**, **3 Nova**, **4 Black Hole**; these four keys can be rebound.

### Weapons: 5 Essence per level

| Construction | Level 0 | Levels 1 / 2 / 3 |
| --- | --- | --- |
| Front Nozzles | One forward shot | Add a pair at ±15° / ±30° / ±45°; total forward shots 3 / 5 / 7 |
| Rear Nozzles | No rear shot | Add a rear pair at ±15° / ±30° / ±45°; total rear shots 2 / 4 / 6 |
| Piercing | One target per bullet | Up to 3 / 5 / unlimited distinct targets per bullet |
| Bullet Enlargement | Radius 3 | Radius 5 / 7 / 9; color shifts deeper purple |

Nozzle upgrades also apply to Void lasers. Drones fire through the same weapon construction system. The nozzles are shown as a compass arch with narrow triangular direction pointers; the pointers fade toward their tips.

### Abilities: 10 Essence per level

| Ability | Level 1 | Level 2 | Level 3 | Other rules |
| --- | --- | --- | --- | --- |
| Teleport | 10-second cooldown | 5-second cooldown | 2-second cooldown | A marker selects a safe destination. Arrival occurs after 30 ticks, with 15 ticks of protection. Landing stays at least 90 pixels from edges and 32 pixels clear of obstacles when possible. |
| Drone | 1 active drone, fires every 5 seconds | 2 active drones, fires every 3 seconds | 3 active drones, fires every 1 second | One deploy charge regenerates every 12 seconds, up to the level cap. Drones pursue targets within 450 pixels and fire within 165. They avoid black holes and targets inside their danger zones. |
| Nova | 30-second cooldown | 20-second cooldown | 10-second cooldown | Takes 1 second to charge, then expands over 1 second to a 1,000-pixel radius. It destroys asteroids, enemies, and black holes, and clears Void energy. |
| Black Hole | 30-second cooldown | 20-second cooldown | 10-second cooldown | Requires at least 1 / 2 / 3 natural black holes absorbed before levels 1 / 2 / 3 can be built. Hold its slot key to extend launch distance from 80 to 820 pixels (5 pixels per tick, capped at 150 charge ticks); release to launch. Tab cancels. |

The Black Hole ability launches a player-owned hole at 12 pixels per tick until it reaches the chosen distance. Its initial size is `min(110, 34 + 8 × absorbed black holes)`. Its center can merge with natural holes; merged holes become player-owned and inherit the longer remaining lifetime plus half the shorter one.

### Utilities: 10 Essence per level

| Utility | Level 1 | Level 2 | Level 3 |
| --- | --- | --- | --- |
| Energy Modulator | Actions above 50 Void energy drain 1.25× the usual action amount | Above-50 action drain is 1.75× | Above-50 action drain is 2.25×; natural holes at size ≤32 can be absorbed |
| Swarm | 2 bots every 10 seconds | 4 bots every 8 seconds | 6 bots every 5 seconds |
| Metric Divergence | 3 rings; 0.8× speed per ring crossed | 4 rings; 0.5× speed per ring crossed | 5 rings; 0.3× speed per ring crossed |
| Shield | 18-second recharge, 160-pixel burst radius | 12-second recharge, 320-pixel radius | 7-second recharge, full-screen diagonal radius |

Energy Modulator still lets idle Void energy rise naturally past 50 and reach 100. Below 50, player actions increase energy and speed later gain; above 50, those same actions drain energy, using the multipliers in the table. Passive gain above 50 remains 0.1 per tick. It does not remove the danger at 100.

Swarm's first wave appears after 1 second. Bots stay for at most one wave period, seek separate targets within about 460 pixels, and explode on contact. Each blast has an 85-pixel radius and lasts 20 ticks. Like drones, Swarm bots steer away from active black holes and skip targets inside a hole's danger area. The utilities panel shows Swarm and Shield recharge as circulating rings.

Metric Divergence affects only asteroids and hostile creatures/entities moving toward the ship. Its outer ring has a 260-pixel radius; inner ring radii are `260 / sqrt(ring number)`. The speed factors multiply for every crossed ring. Bullets and Void lasers are unaffected. Shield triggers on an asteroid or hostile enemy within its 34-pixel boundary, or on Void energy reaching 100. It destroys asteroids and hostile enemies inside its burst radius, clears energy at the 100 threshold, and then recharges. Its visible burst lasts 22 ticks.

## Enemies and hazards

| Object | Size or HP | Behavior and reward |
| --- | --- | --- |
| Small asteroid | Nominal radius 15 pixels | Breaks without splitting; +5 M-AM and +5 score when destroyed by the player's weapons, drones, Swarm, Nova, or Shield. |
| Medium asteroid | Nominal radius 25 pixels | Splits into two small asteroids when hit by bullets or out-of-Void lasers; +10 M-AM and +10 score for the medium itself. |
| Large asteroid | Nominal radius 40 pixels | Splits into two medium asteroids when hit by bullets or out-of-Void lasers; +20 M-AM and +20 score for the large itself. |
| Cosmic entity | Size 6–210 pixels; 25–250 HP (1–10 ordinary bullet hits) | Chases the ship outside the Void. Small/medium/large sizes 6–68 / 69–139 / 140–210 have weight 1 / 2 / 3 and grant 1–2 / 1–3 / 1–5 Essence. Movement speed is 0.8–4 pixels per tick, decreasing with size. |
| Void creature | Size 6–210 pixels; 25–250 HP (1–10 ordinary bullet hits) | Chases the ship inside the Void. Uses the same size tiers, weights, and Essence rewards as cosmic entities. Pursuit speed is 0.6–3.5 pixels per tick, decreasing with size. |
| Natural black hole | Initial size `60–79 + 2 × min(level, 10)`; growth cap 420 | First appears at level 8 after a 3-second warning. Up to three natural holes can be present before another is added at a level start. Its event horizon radius is half its size. Contact can consume the ship and other objects. |

Natural black holes attract one another from up to 900 pixels and merge when their event horizons meet. A merged hole's mass is based on the sum of its parents' squared sizes. Consuming large/medium/small asteroids grows a hole by 7/5/3 size units; consuming an enemy grows it by 4. A natural hole attracts the ship within `min(350, 2.75 × size)` pixels. Lasers shrink a hole by 6 per hit; a hole below size 18 breaks unless a level 3 Energy Modulator makes it absorbable. To absorb a natural hole, shrink it to size 32 or less and move within its event horizon radius plus 42 pixels. Absorption grants infinite M-AM and counts toward Black Hole ability levels.

Enemy HP is `25 × [1 + round((size − 6) × 9 / 204)]`. Drone movement is capped at 2.5 pixels per tick; Swarm bot movement is capped at 3.8. A free-moving black hole is capped between 0.35 and 1.8 pixels per tick depending on size. Asteroid shapes vary around their nominal radii.

Natural holes begin with a 30-second life pool; fired holes begin with 22 seconds. Their lifetime multiplier is `max(0.4, 0.95 − 0.55 × size / 420)`, so larger holes spend that pool faster and expire sooner. A hole fades out over 18 ticks after its life ends. These are approximate lifetimes because holes can grow, shrink, or merge during play.

## Saving and interface

The left panel shows equipped utilities and recharge rings. The bottom bar shows the four equipped abilities, their keys, and cooldown progress. The construction screen shows current Essence, levels, costs, and shortcuts. A paused run is saved for **Continue**; **New Game** resets the current run's fuel, Essence, ability and utility constructions, and absorption progress. Highest score and survival duration remain saved. Save locations and download instructions are in [README.md](README.md).
