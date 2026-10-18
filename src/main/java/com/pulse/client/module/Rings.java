package com.pulse.client.module;

import com.pulse.client.ui.Fx;
import com.pulse.client.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Расходящиеся светящиеся кольца под ногами при прыжке. */
public final class Rings {
    private Rings() {}

    private static final int LIFE = 22;
    private static final int SEGS = 44;
    private static final List<Ring> LIST = new ArrayList<>();

    private static final class Ring {
        final double x, y, z;
        int age;
        Ring(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    }

    public static void spawn(double x, double y, double z) {
        LIST.add(new Ring(x, y, z));
        if (LIST.size() > 6) LIST.remove(0);
    }

    public static void tick() {
        Iterator<Ring> it = LIST.iterator();
        while (it.hasNext()) if (++it.next().age >= LIFE) it.remove();
    }

    public static void render(DrawContext ctx) {
        if (LIST.isEmpty()) return;
        float partial = Comets.partial();
        float u = Fx.u();
        for (Ring r : LIST) {
            float t = (r.age + partial) / LIFE;
            if (t >= 1f) continue;
            float e = 1f - (1f - t) * (1f - t) * (1f - t);
            drawRing(ctx, r, 0.3 + 1.6 * e, u * 1.5f, 1f - t);
            drawRing(ctx, r, 0.2 + 1.1 * e, u * 0.9f, (1f - t) * 0.55f);
        }
    }

    private static void drawRing(DrawContext ctx, Ring r, double radius, float w, float alpha) {
        float[] prev = null;
        for (int i = 0; i <= SEGS; i++) {
            double a = i * Math.PI * 2 / SEGS;
            float[] p = Cam.projectPx(r.x + Math.cos(a) * radius, r.y + 0.03, r.z + Math.sin(a) * radius);
            if (prev != null && p != null) {
                int edge = Theme.accent(i * 7) & 0xFFFFFF;
                int core = Theme.lerp(edge, 0xFFFFFF, 0.75f);
                Fx.glowLine(ctx, prev[0], prev[1], p[0], p[1], w, edge, core, alpha);
            }
            prev = p;
        }
    }
}
