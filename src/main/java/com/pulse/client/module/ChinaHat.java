package com.pulse.client.module;

import com.pulse.client.ui.Fx;
import com.pulse.client.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

/** «Китайская шляпа»: полупрозрачный градиентный конус над головой игрока (вид от 3-го лица). */
public final class ChinaHat {
    private ChinaHat() {}

    private static final int RINGS = 16;
    private static final int SEGS = 40;

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        if (mc.options.getPerspective().isFirstPerson()) return;

        Vec3d p = mc.player.getLerpedPos(Comets.partial());
        double base = p.y + mc.player.getHeight() + 0.02;
        double radius = 0.62, height = 0.4;
        float u = Fx.u();

        // толщина колец подбирается так, чтобы конус выглядел сплошным
        float[] b = Cam.projectPx(p.x, base, p.z);
        float[] a = Cam.projectPx(p.x, base + height, p.z);
        if (b == null || a == null) return;
        float spacing = Math.abs(b[1] - a[1]) / RINGS;
        float fillW = Math.max(u * 1.2f, spacing * 1.6f);

        for (int r = 0; r < RINGS; r++) {
            double t = r / (double) RINGS;
            double rad = radius * (1.0 - t);
            double y = base + height * t;
            float[] prev = null;
            for (int i = 0; i <= SEGS; i++) {
                double ang = i * Math.PI * 2 / SEGS;
                float[] q = Cam.projectPx(p.x + Math.cos(ang) * rad, y, p.z + Math.sin(ang) * rad);
                if (prev != null && q != null) {
                    int col = Theme.lerp(Theme.accent(i * 9) & 0xFFFFFF, 0xFFFFFF, (float) (t * 0.45));
                    Fx.line(ctx, prev[0], prev[1], q[0], q[1], fillW, Theme.withAlpha(col, 70));
                }
                prev = q;
            }
        }

        // яркий ободок по нижнему краю
        float[] prev = null;
        for (int i = 0; i <= SEGS; i++) {
            double ang = i * Math.PI * 2 / SEGS;
            float[] q = Cam.projectPx(p.x + Math.cos(ang) * radius, base, p.z + Math.sin(ang) * radius);
            if (prev != null && q != null) {
                int edge = Theme.accent(i * 9) & 0xFFFFFF;
                int core = Theme.lerp(edge, 0xFFFFFF, 0.7f);
                Fx.glowLine(ctx, prev[0], prev[1], q[0], q[1], u * 1.4f, edge, core, 1f);
            }
            prev = q;
        }
    }
}
