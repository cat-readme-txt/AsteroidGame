
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

public final class FontLoader {
    private static Font aquirePlainBase;
    private static Font aquireBoldBase;
    private static Font aquireLightBase;
    private static Font cartesianBase;
    private static Font eightgonPlainBase;
    private static Font eightgonItalicBase;
    private static Font trenchThinBase;

    private static Font loadBaseFont(Font cached, String path, int styleFallback) {
        if (cached != null) {
            return cached;
        }
        try (InputStream fontStream = FontLoader.class.getResourceAsStream(path)) {
            if (fontStream == null) {
                return new Font("Arial", styleFallback, 12);
            }
            return Font.createFont(Font.TRUETYPE_FONT, fontStream);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Arial", styleFallback, 12);
        }
    }

	public static Font loadAquirePlain(float size) {
        aquirePlainBase = loadBaseFont(
            aquirePlainBase,
            "/assets/fonts/aquire-font/Aquire-BW0ox.otf",
            Font.PLAIN
        );
        return aquirePlainBase.deriveFont(size);
	}
	public static Font loadAquireBold(float size) {
        aquireBoldBase = loadBaseFont(
            aquireBoldBase,
            "/assets/fonts/aquire-font/AquireBold-8Ma60.otf",
            Font.BOLD
        );
        return aquireBoldBase.deriveFont(size);
	}
	public static Font loadAquireLight(float size) {
        aquireLightBase = loadBaseFont(
            aquireLightBase,
            "/assets/fonts/aquire-font/AquireLight-YzE0o.otf",
            Font.PLAIN
        );
        return aquireLightBase.deriveFont(size);
	}
	public static Font loadCartesian(float size) {
        cartesianBase = loadBaseFont(
            cartesianBase,
            "/assets/fonts/cartesian-font/Cartesian-0W5Oo.otf",
            Font.PLAIN
        );
        return cartesianBase.deriveFont(size);
	}
	public static Font loadEightgonPlain(float size) {
        eightgonPlainBase = loadBaseFont(
            eightgonPlainBase,
            "/assets/fonts/eightgon-font/Eightgon-OGn6p.ttf",
            Font.PLAIN
        );
        return eightgonPlainBase.deriveFont(size);
	}
	public static Font loadEightgonItalic(float size) {
        eightgonItalicBase = loadBaseFont(
            eightgonItalicBase,
            "/assets/fonts/eightgon-font/EightgonItalic-Zpw6z.ttf",
            Font.PLAIN
        );
        return eightgonItalicBase.deriveFont(size);
	}
	public static Font loadTrenchThin(float size) {
        trenchThinBase = loadBaseFont(
            trenchThinBase,
            "/assets/fonts/trench-font/TrenchThin-16R0.otf",
            Font.PLAIN
        );
        return trenchThinBase.deriveFont(size);
	}
}
