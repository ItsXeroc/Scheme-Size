package scheme.tools;

public class DisabledTools {

    public static final int FLUSH      = 1;
    public static final int FILL       = 1 << 1;
    public static final int BRUSH      = 1 << 2;
    public static final int RULESETTER = 1 << 3;
    public static final int DESPAWN    = 1 << 4;
    public static final int TELEPORT   = 1 << 5;
    public static final int SPAWN      = 1 << 6;
    public static final int EFFECT     = 1 << 7;
    public static final int ITEM       = 1 << 8;
    public static final int TEAM       = 1 << 9;
    public static final int CORE       = 1 << 10;

    private static int flags;

    public static void set(byte[] data) {
        if (data.length >= 2) flags = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        else flags = 0;
    }

    public static void clear() {
        flags = 0;
    }

    public static boolean disabled(int flag) {
        return (flags & flag) != 0;
    }

    public static byte[] encode(int flags) {
        return new byte[]{ (byte)(flags >> 8), (byte)(flags & 0xFF) };
    }
}
