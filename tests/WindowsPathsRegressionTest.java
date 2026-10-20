import java.nio.file.Path;

public final class WindowsPathsRegressionTest {
    public static void main(String[] args) {
        String previousOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11");
            String appData = System.getenv("APPDATA");
            Path expected = appData != null && !appData.isBlank()
                    ? Path.of(appData, "VoidNavigator")
                    : Path.of(System.getProperty("user.home"), "AppData", "Roaming", "VoidNavigator");
            if (!AppPaths.appDataDir().equals(expected)
                    || !AppPaths.saveFile().toPath().equals(expected.resolve("save.dat"))) {
                throw new AssertionError("Windows saves must use the user's roaming app data");
            }
            System.out.println("WindowsPathsRegressionTest: PASS");
        } finally {
            if (previousOs == null) System.clearProperty("os.name");
            else System.setProperty("os.name", previousOs);
        }
    }
}
