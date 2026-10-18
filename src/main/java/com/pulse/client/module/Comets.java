package com.pulse.client.module;

import com.pulse.client.ui.Fx;
import com.pulse.client.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Светящиеся штрихи при ударе: дуги с хвостом, белая сердцевина, цветное свечение. */
public final class Comets {
    private Comets() {}

    private static final int TRAIL = 6;
    private static final int COUNT = 12;
    private static final Random RNG = new Random();
    private static final List<Comet> LIST = new ArrayList<>();
    private static long lastTickMs = System.currentTimeMillis();

    private static final class Comet {
        double x, y, z, vx, vy, vz, spin;
        int age, life, n;
        float size;
        final double[] hx = new double[TRAIL], hy = new double[TRAIL], hz = new double[TRAIL];
    }

    /** Доля текущего тика (0..1) для плавной интерполяции между тиками. */
    public static float partial() {
        return Math.min(1f, (System.currentTimeMillis() - lastTickMs) / 50f);
    }

    public static void burst(Entity target) {
        Vec3d p = target.getEntityPos().add(0, target.getHeight() * 0.55, 0);
        for (int i = 0; i < COUNT; i++) {
            Comet c = new Comet();
            c.x = p.x + (RNG.nextDouble() - 0.5) * 0.3;
            c.y = p.y + (RNG.nextDouble() - 0.5) * 0.5;
            c.z = p.z + (RNG.nextDouble() - 0.5) * 0.3;
            double a = RNG.nextDouble() * Math.PI * 2;
            double sp = 0.20 + RNG.nextDouble() * 0.28;
            c.vx = Math.cos(a) * sp;
            c.vz = Math.sin(a) * sp;
            c.vy = (RNG.nextDouble() - 0.25) * 0.26;
            c.spin = (RNG.nextBoolean() ? 1 : -1) * (0.10 + RNG.nextDouble() * 0.16);
            c.life = 11 + RNG.nextInt(7);
            c.size = RNG.nextFloat() < 0.25f ? 3.4f : 2.2f;
            LIST.add(c);
        }
        if (LIST.size() > 80) LIST.subList(0, LIST.size() - 80).clear();
    }

    public static void tick() {
        lastTickMs = System.currentTimeMillis();
        Iterator<Comet> it = LIST.iterator();
        while (it.hasNext()) {
            Comet c = it.next();
            System.arraycopy(c.hx, 0, c.hx, 1, TRAIL - 1);
            System.arraycopy(c.hy, 0, c.hy, 1, TRAIL - 1);
            System.arraycopy(c.hz, 0, c.hz, 1, TRAIL - 1);
            c.hx[0] = c.x;
            c.hy[0] = c.y;
            c.hz[0] = c.z;
            c.n = Math.min(TRAIL, c.n + 1);

            double cos = Math.cos(c.spin), sin = Math.sin(c.spin);
            double nvx = c.vx * cos - c.vz * sin;
            double nvz = c.vx * sin + c.vz * cos;
            c.vx = nvx * 0.94;
            c.vz = nvz * 0.94;
            c.vy = c.vy * 0.94 - 0.006;
            c.x += c.vx;
            c.y += c.vy;
            c.z += c.vz;
            if (++c.age >= c.life) it.remove();
        }
    }

    /** Вызывать внутри Fx.begin/end (координаты в физических пикселях). */
    public static void render(DrawContext ctx) {
        if (LIST.isEmpty()) return;
        float partial = partial();
        float u = Fx.u();

        int accent = Theme.accent(0) & 0xFFFFFF;
        int core = Theme.lerp(accent, 0xFFFFFF, 0.8f);

        for (Comet c : LIST) {
            float life = 1f - (c.age + partial) / c.life;
            if (life <= 0f || c.n == 0) continue;

            double hx = c.hx[0] + (c.x - c.hx[0]) * partial;
            double hy = c.hy[0] + (c.y - c.hy[0]) * partial;
            double hz = c.hz[0] + (c.z - c.hz[0]) * partial;
            float[] last = Cam.projectPx(hx, hy, hz);
            if (last == null) continue;

            for (int i = 0; i < c.n; i++) {
                float[] cur = Cam.projectPx(c.hx[i], c.hy[i], c.hz[i]);
                if (cur == null) break;
                float k = 1f - i / (float) c.n;
                float w = Math.max(1f, c.size * u * (0.3f + 0.7f * k));
                Fx.glowLine(ctx, last[0], last[1], cur[0], cur[1], w, accent, core,
                        life * (0.3f + 0.7f * k));
                last = cur;
            }
        }
    }

    /** Проекция в координаты GUI (для текстов и меток). */
    public static float[] project(MinecraftClient mc, double wx, double wy, double wz, int sw, int sh) {
        return Cam.project(wx, wy, wz, sw, sh);
    }
}
