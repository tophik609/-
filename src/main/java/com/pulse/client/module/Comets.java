package com.pulse.client.module;

import com.pulse.client.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Светящиеся штрихи-«кометы» при ударе: летят дугами от цели, у каждой хвост,
 * белая сердцевина и цветной край. Рисуются в экранном пространстве.
 */
public final class Comets {
    private Comets() {}

    private static final int TRAIL = 8;
    private static final Random RNG = new Random();
    private static final List<Comet> LIST = new ArrayList<>();
    private static long lastTickMs = System.currentTimeMillis();

    private static final class Comet {
        double x, y, z, vx, vy, vz;
        double spin;
        int age, life;
        float size;
        final double[][] hist = new double[TRAIL][3];
        int histN = 0;
    }

    public static void burst(Entity target) {
        Vec3d p = target.getEntityPos().add(0, target.getHeight() * 0.55, 0);
        int n = 16;
        for (int i = 0; i < n; i++) {
            Comet c = new Comet();
            c.x = p.x + (RNG.nextDouble() - 0.5) * 0.3;
            c.y = p.y + (RNG.nextDouble() - 0.5) * 0.5;
            c.z = p.z + (RNG.nextDouble() - 0.5) * 0.3;
            double a = RNG.nextDouble() * Math.PI * 2;
            double sp = 0.22 + RNG.nextDouble() * 0.30;
            c.vx = Math.cos(a) * sp;
            c.vz = Math.sin(a) * sp;
            c.vy = (RNG.nextDouble() - 0.25) * 0.28;
            c.spin = (RNG.nextBoolean() ? 1 : -1) * (0.10 + RNG.nextDouble() * 0.16);
            c.life = 12 + RNG.nextInt(8);
            c.size = RNG.nextFloat() < 0.25f ? 4f : 2.6f;
            LIST.add(c);
        }
    }

    public static void tick() {
        lastTickMs = System.currentTimeMillis();
        Iterator<Comet> it = LIST.iterator();
        while (it.hasNext()) {
            Comet c = it.next();
            // записываем историю позиций для хвоста
            System.arraycopy(c.hist, 0, c.hist, 1, TRAIL - 1);
            // чтобы не плодить массивы — копируем значения
            c.hist[0] = new double[]{c.x, c.y, c.z};
            c.histN = Math.min(TRAIL, c.histN + 1);

            // поворот скорости вокруг вертикали = изогнутая дуга
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

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (LIST.isEmpty() || mc.player == null) return;
        float partial = Math.min(1f, (System.currentTimeMillis() - lastTickMs) / 50f);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        int accent = Theme.accent(0) & 0xFFFFFF;
        int edge = accent;
        int core = Theme.lerp(accent, 0xFFFFFF, 0.78f);

        for (Comet c : LIST) {
            float life = 1f - (c.age + partial) / c.life;
            if (life <= 0f) continue;

            // позиция головы, интерполированная между тиками
            double[] prev = c.histN > 0 ? c.hist[0] : new double[]{c.x, c.y, c.z};
            double hx = prev[0] + (c.x - prev[0]) * partial;
            double hy = prev[1] + (c.y - prev[1]) * partial;
            double hz = prev[2] + (c.z - prev[2]) * partial;

            float[] last = project(mc, hx, hy, hz, sw, sh);
            if (last == null) continue;

            int segs = Math.min(c.histN, TRAIL);
            for (int i = 0; i < segs; i++) {
                double[] h = c.hist[i];
                float[] cur = project(mc, h[0], h[1], h[2], sw, sh);
                if (cur == null) break;
                float k = 1f - i / (float) segs;       // 1 у головы, 0 у хвоста
                float width = Math.max(1f, c.size * k);
                int a = (int) (255 * life * (0.25f + 0.75f * k));
                // внешний цветной край и светлая сердцевина
                line(ctx, last[0], last[1], cur[0], cur[1], width + 2f, Theme.withAlpha(edge, a / 2));
                line(ctx, last[0], last[1], cur[0], cur[1], width, Theme.withAlpha(core, a));
                last = cur;
            }
        }
    }

    private static void line(DrawContext ctx, float x0, float y0, float x1, float y1, float w, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        int steps = (int) Math.min(90, Math.max(1, Math.max(Math.abs(dx), Math.abs(dy))));
        int s = Math.max(1, Math.round(w));
        int half = s / 2;
        for (int i = 0; i <= steps; i++) {
            int px = Math.round(x0 + dx * i / steps) - half;
            int py = Math.round(y0 + dy * i / steps) - half;
            ctx.fill(px, py, px + s, py + s, color);
        }
    }

    /** Проекция точки мира в экранные координаты GUI (без миксинов). null — если позади камеры. */
    private static float[] project(MinecraftClient mc, double wx, double wy, double wz, int sw, int sh) {
        Vec3d eye = mc.player.getEyePos();
        double dx = wx - eye.x, dy = wy - eye.y, dz = wz - eye.z;
        double yaw = Math.toRadians(mc.player.getYaw());
        double pitch = Math.toRadians(mc.player.getPitch());
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        double z = dx * fx + dy * fy + dz * fz;
        if (z < 0.15) return null;
        double x = dx * rx + dy * ry + dz * rz;
        double y = dx * ux + dy * uy + dz * uz;
        double fov = Math.toRadians(mc.options.getFov().getValue());
        double f = 1.0 / Math.tan(fov / 2.0);
        double aspect = (double) sw / sh;
        return new float[]{
                (float) ((x / z) * f / aspect * 0.5 + 0.5) * sw,
                (float) (0.5 - (y / z) * f * 0.5) * sh
        };
    }
}
