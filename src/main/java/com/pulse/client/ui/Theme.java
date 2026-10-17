package com.pulse.client.ui;

import java.awt.Color;

public final class Theme {
    private Theme() {}

    /** Плавно переливающийся акцентный цвет (ARGB). offset сдвигает фазу для градиента. */
    public static int accent(int offset) {
        float hue = ((System.currentTimeMillis() + offset * 9L) % 6000L) / 6000f;
        // фиолетово-голубая гамма, как в "пульсе": 0.5..0.85
        float h = 0.5f + 0.35f * (0.5f + 0.5f * (float) Math.sin(hue * Math.PI * 2));
        return Color.HSBtoRGB(h, 0.65f, 1f) | 0xFF000000;
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    public static final int BG = 0xB0101018;
    public static final int BG_DARK = 0xD00A0A10;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFFAAAAAA;
}
