import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;

public final class AudioRegressionTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        for (GameAudio.Track track : GameAudio.Track.values()) {
            URL resource = GameAudio.class.getResource(track.resource());
            check(resource != null, track + " music is bundled");
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
                AudioFormat format = stream.getFormat();
                check(format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED,
                        track + " is decodable PCM");
                check(format.getChannels() == 1 && format.getSampleSizeInBits() == 16
                                && format.getSampleRate() == 32000,
                        track + " matches the streaming format");
                check(stream.getFrameLength() > 32_000, track + " contains playable audio");
            }
        }
        for (String effect : new String[]{"ui-select.wav", "mouse-click.wav"}) {
            URL resource = GameAudio.class.getResource("/assets/audio/" + effect);
            check(resource != null, effect + " is bundled");
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
                check(stream.getFrameLength() > 0, effect + " is decodable");
            }
        }
        GameAudio audio = new GameAudio();
        check(audio.selected() == GameAudio.Track.MENU, "menu music is the initial track");
        audio.select(GameAudio.Track.VOID);
        check(audio.selected() == GameAudio.Track.VOID, "Void travel selects Void music");
        audio.restart(GameAudio.Track.STORY);
        check(audio.selected() == GameAudio.Track.STORY, "story entry selects story music");
        audio.forget(GameAudio.Track.STORY);
        audio.close();
        System.out.println("AudioRegressionTest: " + checks + " checks passed");
    }
}
