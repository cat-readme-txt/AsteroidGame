import java.io.File;
import java.nio.file.Path;

public final class AppPaths {
    private static final String APP_DIR_NAME = "VoidNavigator";

    private AppPaths() {
    }

    public static File saveFile() {
        return appDataDir().resolve("save.dat").toFile();
    }

    public static Path appDataDir() {
        String userHome = System.getProperty("user.home", ".");
        String osName = System.getProperty("os.name", "").toLowerCase();

        if (osName.contains("mac")) {
            return Path.of(userHome, "Library", "Application Support", APP_DIR_NAME);
        }

        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, APP_DIR_NAME);
            }
            return Path.of(userHome, "AppData", "Roaming", APP_DIR_NAME);
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Path.of(xdgDataHome, APP_DIR_NAME);
        }

        return Path.of(userHome, ".local", "share", APP_DIR_NAME);
    }
}
