import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class GameSaveStore {
    private static final int FILE_MAGIC = 0x41534731; // "ASG1"
    private static final int FILE_VERSION = 4;

    private final File saveFile;

    public GameSaveStore(File saveFile) {
        this.saveFile = saveFile;
    }

    public GameSaveState loadState() {
        if (!saveFile.exists() || saveFile.length() == 0) {
            return emptyState();
        }

        try (DataInputStream input = new DataInputStream(new FileInputStream(saveFile))) {
            int marker = input.readInt();
            if (marker == FILE_MAGIC) {
                return readVersionedState(input);
            }
        } catch (EOFException e) {
            return emptyState();
        } catch (IOException e) {
            e.printStackTrace();
            return emptyState();
        }

        return loadLegacyState();
    }

    public void saveActiveSession(int highestScore, int highestDuration, GameSessionSnapshot session) {
        writeState(new GameSaveState(highestScore, highestDuration, session));
    }

    public void clearActiveSession(int highestScore, int highestDuration) {
        writeState(new GameSaveState(highestScore, highestDuration, null));
    }

    private void writeState(GameSaveState state) {
        ensureParentDirectory();

        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(saveFile))) {
            output.writeInt(FILE_MAGIC);
            output.writeInt(FILE_VERSION);
            output.writeInt(state.highestScore());
            output.writeInt(state.highestDuration());
            output.writeBoolean(state.hasActiveSession());

            if (state.hasActiveSession()) {
                writeSession(output, state.activeSession());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private GameSaveState readVersionedState(DataInputStream input) throws IOException {
        int version = input.readInt();
        if (version != 1 && version != 2 && version != 3 && version != FILE_VERSION) {
            throw new IOException("Unsupported save version: " + version);
        }

        int highestScore = input.readInt();
        int highestDuration = input.readInt();
        boolean hasActiveSession = input.readBoolean();

        GameSessionSnapshot session = hasActiveSession ? readSession(input, version == 1, version) : null;
        return new GameSaveState(highestScore, highestDuration, session);
    }

    private GameSaveState loadLegacyState() {
        try (DataInputStream input = new DataInputStream(new FileInputStream(saveFile))) {
            int highestScore = input.readInt();
            int highestDuration = input.readInt();
            GameSessionSnapshot session = readSession(input, true, 0);
            return new GameSaveState(highestScore, highestDuration, session);
        } catch (EOFException e) {
            return emptyState();
        } catch (IOException e) {
            e.printStackTrace();
            return emptyState();
        }
    }

    private GameSessionSnapshot readSession(DataInputStream input, boolean legacyXP, int version) throws IOException {
        GameSessionSnapshot session = new GameSessionSnapshot(
            input.readInt(),
            input.readInt(),
            input.readDouble(),
            input.readInt(),
            input.readInt(),
            input.readBoolean(),
            input.readDouble(),
            input.readDouble(),
            input.readDouble()
        );
        if (legacyXP) {
            input.readInt();
            input.readInt();
            input.readInt();
        }
        if (version < 3) return session;
        int essence = input.readInt();
        int absorbed = input.readInt();
        boolean infinite = input.readBoolean();
        int[] weaponLevels = readLevels(input);
        int[] abilityLevels = readLevels(input);
        if (version == 3) abilityLevels = new int[]{abilityLevels[0], abilityLevels[2],
                abilityLevels[3], abilityLevels[1]};
        String[] abilitySlots = readSlots(input);
        int[] utilityLevels = readLevels(input);
        String[] utilitySlots = readSlots(input);
        return new GameSessionSnapshot(session.level(), session.score(), session.fuel(),
                session.levelStartCountdown(), session.levelDisplayAlpha(), session.levelStarting(),
                session.shipX(), session.shipY(), session.shipAngle(), essence, absorbed, infinite,
                weaponLevels, abilityLevels, abilitySlots, utilityLevels, utilitySlots);
    }

    private void writeSession(DataOutputStream output, GameSessionSnapshot session) throws IOException {
        output.writeInt(session.level());
        output.writeInt(session.score());
        output.writeDouble(session.fuel());
        output.writeInt(session.levelStartCountdown());
        output.writeInt(session.levelDisplayAlpha());
        output.writeBoolean(session.levelStarting());
        output.writeDouble(session.shipX());
        output.writeDouble(session.shipY());
        output.writeDouble(session.shipAngle());
        output.writeInt(session.incidentEssence());
        output.writeInt(session.absorbedBlackHoles());
        output.writeBoolean(session.infiniteFuel());
        writeLevels(output, session.weaponLevels());
        writeLevels(output, session.abilityLevels());
        writeSlots(output, session.abilitySlots());
        writeLevels(output, session.utilityLevels());
        writeSlots(output, session.utilitySlots());
    }

    private static int[] readLevels(DataInputStream input) throws IOException {
        int[] levels = new int[4];
        for (int i = 0; i < 4; i++) levels[i] = input.readUnsignedByte();
        return levels;
    }

    private static void writeLevels(DataOutputStream output, int[] levels) throws IOException {
        for (int i = 0; i < 4; i++) output.writeByte(levels[i]);
    }

    private static String[] readSlots(DataInputStream input) throws IOException {
        String[] slots = new String[4];
        for (int i = 0; i < 4; i++) if (input.readBoolean()) slots[i] = input.readUTF();
        return slots;
    }

    private static void writeSlots(DataOutputStream output, String[] slots) throws IOException {
        for (int i = 0; i < 4; i++) {
            output.writeBoolean(slots[i] != null);
            if (slots[i] != null) output.writeUTF(slots[i]);
        }
    }

    private void ensureParentDirectory() {
        File parent = saveFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.err.println("Unable to create save directory: " + parent.getAbsolutePath());
        }
    }

    private GameSaveState emptyState() {
        return new GameSaveState(0, 0, null);
    }
}
