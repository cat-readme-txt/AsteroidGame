import java.nio.file.Files;
import java.nio.file.Path;

/** Deployment checks for the responsive CheerpJ browser surface. */
public final class WebResizeRegressionTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        check(DesktopLauncher.isBrowserMode(new String[] {"--browser"}),
                "browser launch argument is recognized");
        check(!DesktopLauncher.isBrowserMode(new String[0]),
                "desktop launch remains the default");
        check(!DesktopLauncher.isBrowserMode(null),
                "missing launch arguments remain desktop mode");

        String html = Files.readString(Path.of("docs", "index.html"));
        check(html.contains("name=\"viewport\""),
                "page declares a device-width viewport");
        check(html.contains("height: 100dvh"),
                "container follows the dynamic mobile viewport height");
        check(html.contains("cheerpjCreateDisplay(-1, -1"),
                "CheerpJ display follows its parent size");
        check(html.contains("appJarPath, \"--browser\""),
                "web launcher requests browser window behavior");

        System.out.println("WebResizeRegressionTest: " + assertions + " assertions passed");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
