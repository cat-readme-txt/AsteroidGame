
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

public final class FontLoader {
	public static Font loadAquirePlain(float size) {
		try {
            // Use InputStream for JAR compatibility
            InputStream fontStream = FontLoader.class.getResourceAsStream(
                "/assets/fonts/aquire-font/Aquire-BW0ox.otf");
            
            if (fontStream == null) {
                System.err.println("Font not found, using fallback");
                return new Font("Arial", Font.PLAIN, (int) size);
            }
            
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(baseFont);
            fontStream.close();
            
            return baseFont.deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Arial", Font.PLAIN, (int) size);
        }
	}
	public static Font loadAquireBold(float size) {
		try {
            InputStream fontStream = FontLoader.class.getResourceAsStream(
                "/assets/fonts/aquire-font/AquireBold-8Ma60.otf");
            if (fontStream == null) {
                return new Font("Arial", Font.BOLD, (int) size);
            }
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
            fontStream.close();
            return baseFont.deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Arial", Font.BOLD, (int) size);
        }
	}
	public static Font loadAquireLight(float size) {
		try {
			InputStream fontStream = FontLoader.class.getResourceAsStream(
                "/assets/fonts/aquire-font/AquireLight-YzE0o.otf");
			if (fontStream == null) {
				return new Font("Arial", Font.PLAIN, (int) size);
			}
			Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
			fontStream.close();
			return baseFont.deriveFont(size);
		} catch (Exception e) {
			e.printStackTrace();
			return new Font("Arial", Font.PLAIN, (int) size); // fallback
		}
	}
	public static Font loadCartesian(float size) {
		try {
			InputStream fontStream = FontLoader.class.getResourceAsStream(
                "/assets/fonts/cartesian-font/Cartesian-0W5Oo.otf");
			if (fontStream == null) {
				return new Font("Arial", Font.PLAIN, (int) size);
			}
			Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(baseFont);
			fontStream.close();
			return baseFont.deriveFont(size);
		} catch (Exception e) {
			e.printStackTrace();
			return new Font("Arial", Font.PLAIN, (int) size); // fallback
		}
	}
	public static Font loadEightgonPlain(float size) {
		try {
			InputStream fontStream = FontLoader.class.getResourceAsStream(
                "/assets/fonts/eightgon-font/Eightgon-OGn6p.ttf");
			if (fontStream == null) {
				return new Font("Arial", Font.PLAIN, (int) size);
			}
			Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(baseFont);
			fontStream.close();
			return baseFont.deriveFont(size);
		} catch (Exception e) {
			e.printStackTrace();
			return new Font("Arial", Font.PLAIN, (int) size); // fallback
		}
	}
	public static Font loadEightgonItalic(float size) {
		try {
			InputStream fontStream = FontLoader.class.getResourceAsStream(
				"/assets/fonts/eightgon-font/EightgonItalic-Zpw6z.ttf");
			if (fontStream == null) {
				return new Font("Arial", Font.PLAIN, (int) size);
			}
			Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(baseFont);
			fontStream.close();
			return baseFont.deriveFont(size);
		} catch (Exception e) {
			e.printStackTrace();
			return new Font("Arial", Font.PLAIN, (int) size); // fallback
		}
	}
	public static Font loadTrenchThin(float size) {
		try {
			InputStream fontStream = FontLoader.class.getResourceAsStream(
                "/assets/fonts/trench-font/TrenchThin-16R0.otf");
			if (fontStream == null) {
				return new Font("Arial", Font.PLAIN, (int) size);
			}
			Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(baseFont);
			fontStream.close();

			return baseFont.deriveFont(size);
		} catch (Exception e) {
			e.printStackTrace();
			return new Font("Arial", Font.PLAIN, (int) size); // fallback
		}
	}
}