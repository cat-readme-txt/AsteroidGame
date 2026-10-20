# VoidNavigator

VoidNavigator is a desktop asteroid survival game. Fly, fight, collect matter-antimatter fuel (M-AM), enter the Void, and spend Incident Essence on ship constructions. The full rules and numeric stats are in [GAME_MECHANICS.md](GAME_MECHANICS.md).

## Download and play

Extract the entire ZIP before launching. Keep the files in the extracted folder together.

| Download | How to start | Java installation |
| --- | --- | --- |
| `VoidNavigator-Windows-x64-0.zip` | Open the extracted `VoidNavigator` folder and double-click `VoidNavigator.exe`. | None; this build includes a Java runtime. |
| `VoidNavigator-macOS-arm64-0.zip` | Open the extracted `VoidNavigator.app`. | None; this build includes a Java runtime. |
| `VoidNavigator-Java17-portable-0.zip` | On Windows, double-click `Play-VoidNavigator.bat` inside the extracted folder. On macOS or Linux, run `java -jar AsteroidGame.jar` there. | Java 17 or newer is required. |

The Windows app ZIP is produced on Windows by the **Windows download** GitHub Actions workflow. After a successful workflow run, open its page under **Actions**, scroll to **Artifacts**, download `VoidNavigator-Windows-x64-0`, then extract the downloaded ZIP and the game ZIP inside it. GitHub requires sign-in to download workflow artifacts; a maintainer can also share the generated game ZIP directly. The portable ZIP can be made on macOS or Linux with `./scripts/package-portable.sh`. A maintainer can make the self-contained Windows ZIP locally with `./scripts/package-windows.ps1` in PowerShell on Windows with JDK 17 or newer.

The macOS app is built for Apple Silicon. The Windows app is built for x64 Windows. The portable JAR uses Java desktop APIs and is the fallback for other desktop systems with Java 17 or newer. Windows native play has not yet been verified on a Windows machine in this checkout.

## First flight

Choose **New Game** from the main menu. The ship starts with 500 M-AM. Destroy asteroids for more fuel and score, defeat cosmic entities and Void creatures for Incident Essence, and press **C** to spend Essence. The in-game **How to play** page and **Settings → Key bindings** show controls and allow key changes.

| Default key | Action |
| --- | --- |
| Up / Left / Right arrows | Thrust / turn left / turn right |
| Space | Hyper mode while held |
| A | Fire; hold for repeated shots |
| V | Toggle the Void |
| Q / W / E / R | Use ability slots 1–4 |
| C | Open or close construction |
| Ctrl+A | Open the armory |
| Ctrl + an ability slot key | Upgrade its equipped ability; an empty slot opens ability construction |
| Tab | Cancel a charged Black Hole launch |
| Esc | Pause; Esc again from pause returns to the menu |
| F11 | Toggle full screen |

Controls changed in Settings replace the defaults shown here. Weapon construction costs 5 Incident Essence per level; ability and utility construction costs 10 per level. The construction screen shows the relevant shortcuts.

## Saves and troubleshooting

Pausing saves the active run. **Continue** restores it; **New Game** starts a fresh run. Highest score and survival time are retained. Saves are stored outside the game folder: `%APPDATA%\VoidNavigator\save.dat` on Windows, `~/Library/Application Support/VoidNavigator/save.dat` on macOS, and `${XDG_DATA_HOME:-~/.local/share}/VoidNavigator/save.dat` on Linux.

If the portable Windows launcher says Java is missing, install Java 17 or newer and reopen the `.bat` file. If the game opens without sound, check the system output device; gameplay can continue without audio. The self-contained Windows build does not need the portable launcher or a separate Java installation.

## Build from source

On macOS or Linux with JDK 17 or newer, run `./scripts/build-desktop.sh` and then `java -jar build/desktop/AsteroidGame.jar`. On Windows with JDK 17 or newer, run `./scripts/package-windows.ps1` in PowerShell to build the complete Windows download. The Java source and game assets are under `src/desktop/`.
