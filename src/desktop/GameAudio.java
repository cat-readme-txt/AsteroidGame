import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

/** Streams one soundtrack at a time and remembers each world's playback position. */
public final class GameAudio implements AutoCloseable {
    public enum Track {
        MENU("menu.wav"), BATTLE("battle.wav"), VOID("void.wav"), STORY("story.wav");

        private final String file;
        Track(String file) { this.file = file; }
        String resource() { return "/assets/audio/" + file; }
    }

    private final Object lock = new Object();
    private final long[] savedFrames = new long[Track.values().length];
    private final long[] resetGeneration = new long[Track.values().length];
    private volatile Track selected = Track.MENU;
    private volatile SourceDataLine activeLine;
    private volatile Clip selectClip;
    private volatile Clip clickClip;
    private volatile double musicVolume = 0.5;
    private volatile double effectsVolume = 0.7;
    private Thread worker;
    private volatile boolean closed;

    public void start() {
        synchronized (lock) {
            if (worker != null && worker.isAlive()) return;
            closed = false;
            worker = new Thread(this::run, "VoidNavigator-audio");
            worker.setDaemon(true);
            worker.start();
        }
    }

    public void select(Track track) {
        synchronized (lock) {
            selected = track;
            lock.notifyAll();
        }
    }

    public void restart(Track track) {
        synchronized (lock) {
            savedFrames[track.ordinal()] = 0;
            resetGeneration[track.ordinal()]++;
            selected = track;
            lock.notifyAll();
        }
    }

    public void forget(Track track) {
        synchronized (lock) {
            savedFrames[track.ordinal()] = 0;
            resetGeneration[track.ordinal()]++;
            lock.notifyAll();
        }
    }

    public Track selected() { return selected; }
    public void setVolumes(int musicPercent, int effectsPercent) {
        musicVolume = Math.max(0, Math.min(100, musicPercent)) / 100.0;
        effectsVolume = Math.max(0, Math.min(100, effectsPercent)) / 100.0;
    }
    public void playSelect() { play(selectClip); }
    public void playClick() { play(clickClip); }

    private void play(Clip clip) {
        if (clip == null) return;
        synchronized (clip) {
            if (effectsVolume <= 0) return;
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.max(gain.getMinimum(),
                        Math.min(gain.getMaximum(), (float) (20 * Math.log10(effectsVolume)))));
            }
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private void run() {
        selectClip = loadClip("/assets/audio/ui-select.wav");
        clickClip = loadClip("/assets/audio/mouse-click.wav");
        while (true) {
            Track track;
            long startingFrame, generation;
            synchronized (lock) {
                if (closed) break;
                while (selected == null && !closed) {
                    try { lock.wait(); }
                    catch (InterruptedException exception) { Thread.currentThread().interrupt(); return; }
                }
                if (closed) break;
                track = selected;
                startingFrame = savedFrames[track.ordinal()];
                generation = resetGeneration[track.ordinal()];
            }
            boolean ended = stream(track, startingFrame, generation);
            if (!ended) {
                synchronized (lock) {
                    while (!closed && selected == track) {
                        try { lock.wait(); }
                        catch (InterruptedException exception) { Thread.currentThread().interrupt(); return; }
                    }
                }
            }
        }
        closeClip(selectClip);
        closeClip(clickClip);
        selectClip = clickClip = null;
    }

    private boolean stream(Track track, long startingFrame, long generation) {
        URL resource = GameAudio.class.getResource(track.resource());
        if (resource == null) return false;
        SourceDataLine line = null;
        boolean reachedEnd = false;
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            int frameSize = stream.getFormat().getFrameSize();
            long remaining = startingFrame * frameSize;
            while (remaining > 0) {
                long skipped = stream.skip(remaining);
                if (skipped <= 0) break;
                remaining -= skipped;
            }
            if (remaining > 0) {
                reachedEnd = true;
                return true;
            }
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(stream.getFormat(), 16_384);
            activeLine = line;
            line.start();
            byte[] buffer = new byte[8_192];
            while (!closed && selected == track) {
                int count = stream.read(buffer, 0, buffer.length);
                if (count < 0) { reachedEnd = true; break; }
                lowerMusicVolume(buffer, count);
                int written = 0;
                while (written < count && !closed && selected == track)
                    written += line.write(buffer, written, count - written);
            }
            if (reachedEnd) line.drain();
        } catch (Exception exception) {
            System.err.println("Could not play " + track + " music: " + exception.getMessage());
        } finally {
            synchronized (lock) {
                if (resetGeneration[track.ordinal()] == generation) {
                    savedFrames[track.ordinal()] = reachedEnd ? 0
                            : startingFrame + (line == null ? 0 : line.getLongFramePosition());
                }
            }
            if (line != null) {
                line.stop();
                line.flush();
                line.close();
            }
            activeLine = null;
        }
        return reachedEnd;
    }

    private void lowerMusicVolume(byte[] samples, int count) {
        double gain = musicVolume * 0.76;
        for (int i = 0; i + 1 < count; i += 2) {
            short value = (short) ((samples[i] & 0xff) | (samples[i + 1] << 8));
            int quieter = (int) (value * gain);
            samples[i] = (byte) quieter;
            samples[i + 1] = (byte) (quieter >> 8);
        }
    }

    private static Clip loadClip(String name) {
        URL resource = GameAudio.class.getResource(name);
        if (resource == null) return null;
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception exception) {
            return null;
        }
    }

    private static void closeClip(Clip clip) { if (clip != null) clip.close(); }

    @Override public void close() {
        synchronized (lock) {
            closed = true;
            lock.notifyAll();
        }
        SourceDataLine line = activeLine;
        if (line != null) {
            line.stop();
            line.flush();
            line.close();
        }
        Thread current = worker;
        if (current != null && current != Thread.currentThread()) {
            try { current.join(250); }
            catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        }
        worker = null;
    }
}
