package com.pulse.client.ui;

import net.minecraft.client.gui.DrawContext;

/** Мелкие помощники рисования: скруглённые прямоугольники и т.п. */
public final class Gfx {
    private Gfx() {}

    public static void round(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        for (int i = 0; i < r; i++) {
            int inset = r - (int) Math.round(Math.sqrt(r * r - (double) (r - i) * (r - i)));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
    }

    public static void switchToggle(DrawContext ctx, int x, int y, float anim, int accent) {
        int off = 0xFF34344A;
        int track = Theme.lerp(off, accent, anim) | 0xFF000000;
        round(ctx, x, y, 22, 11, 5, track);
        int kx = x + 1 + (int) (11 * anim);
        round(ctx, kx, y + 1, 9, 9, 4, 0xFFFFFFFF);
    }
}
