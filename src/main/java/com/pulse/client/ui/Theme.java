package com.pulse.client.ui;

public final class Theme {
    private Theme() {}

    public static final String[] PRESET_NAMES = {"Purple", "Pink & Red", "Ocean"};
    private static final int[][] PRESETS = {
            {0x8B5CF6, 0x6366F1},
            {0xFF4D8D, 0xFF3B3B},
            {0x38BDF8, 0x6366F1}
    };
    public static int preset = 0;

    /** Плавно переливающийся акцентный цвет пресета (ARGB). */
    public static int accent(int offset) {
        double t = (System.currentTimeMillis() + offset * 9L) / 900.0;
        float k = (float) (0.5 + 0.5 * Math.sin(t));
        return lerp(PRESETS[preset][0], PRESETS[preset][1], k) | 0xFF000000;
    }

    public static int presetColor(int p, int which) {
        return PRESETS[p][which] | 0xFF000000;
    }

    public static int lerp(int a, int b, float k) {
        int r = (int) (((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = (int) (((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    public static final int BG = 0xC0101018;
    public static final int BG_DARK = 0xE60C0C14;
    public static final int CARD = 0xFF171722;
    public static final int CARD_HOVER = 0xFF1E1E2C;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF9A9AB0;
}
