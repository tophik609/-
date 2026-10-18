package com.pulse.client.ui;

import com.pulse.client.module.Cam;
import net.minecraft.client.gui.DrawContext;

/** Рисование в физических пикселях окна: линии с мягким свечением без крупных «квадратов» GUI. */
public final class Fx {
    private Fx() {}

    public static void begin(DrawContext ctx) {
        float s = Cam.scale;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(1f / s, 1f / s);
    }

    public static void end(DrawContext ctx) {
        ctx.getMatrices().popMatrix();
    }

    /** Единица толщины, пропорциональная масштабу интерфейса. */
    public static float u() {
        return Math.max(1f, Cam.scale * 0.55f);
    }

    public static void line(DrawContext ctx, float x0, float y0, float x1, float y1, float w, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int s = Math.max(1, Math.round(w));
        float step = Math.max(1f, s * 0.9f);
        int n = Math.max(1, Math.min(300, (int) Math.ceil(len / step)));
        float half = s / 2f;
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            int px = Math.round(x0 + dx * t - half);
            int py = Math.round(y0 + dy * t - half);
            ctx.fill(px, py, px + s, py + s, color);
        }
    }

    /** Линия с мягким внешним свечением и светлой сердцевиной. alpha: 0..1 */
    public static void glowLine(DrawContext ctx, float x0, float y0, float x1, float y1,
                                float w, int edgeRgb, int coreRgb, float alpha) {
        int a = (int) (255 * Math.max(0f, Math.min(1f, alpha)));
        if (a < 4) return;
        line(ctx, x0, y0, x1, y1, w * 2.4f, Theme.withAlpha(edgeRgb, a / 9));
        line(ctx, x0, y0, x1, y1, w * 1.6f, Theme.withAlpha(edgeRgb, a / 4));
        line(ctx, x0, y0, x1, y1, w, Theme.withAlpha(coreRgb, a));
    }
}
