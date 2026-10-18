package com.pulse.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;

/** Реальная позиция и поворот камеры (работает и в виде от 3-го лица) + проекция мира на экран. */
public final class Cam {
    private Cam() {}

    public static Vec3d pos = Vec3d.ZERO;
    public static float yaw, pitch;
    /** Масштаб GUI (физических пикселей на один пиксель интерфейса) */
    public static float scale = 1f;

    private static Method posMethod;
    private static boolean searched;

    public static void update(MinecraftClient mc) {
        scale = (float) mc.getWindow().getScaleFactor();
        Camera cam = mc.gameRenderer.getCamera();
        yaw = cam.getYaw();
        pitch = cam.getPitch();

        Vec3d eye = mc.player != null ? mc.player.getEyePos() : Vec3d.ZERO;
        Vec3d p = null;
        if (!searched) {
            searched = true;
            for (Method m : cam.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == Vec3d.class) {
                    posMethod = m;
                    break;
                }
            }
        }
        if (posMethod != null) {
            try {
                p = (Vec3d) posMethod.invoke(cam);
            } catch (Exception ignored) {
            }
        }
        // страховка: камера не может быть далеко от головы игрока
        if (p == null || p.squaredDistanceTo(eye) > 64) p = eye;
        pos = p;
    }

    /** Координаты в пикселях GUI или null, если точка позади камеры. */
    public static float[] project(double wx, double wy, double wz, int sw, int sh) {
        double dx = wx - pos.x, dy = wy - pos.y, dz = wz - pos.z;
        double ya = Math.toRadians(yaw), pi = Math.toRadians(pitch);
        double fx = -Math.sin(ya) * Math.cos(pi), fy = -Math.sin(pi), fz = Math.cos(ya) * Math.cos(pi);
        double rx = -Math.cos(ya), ry = 0, rz = -Math.sin(ya);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        double z = dx * fx + dy * fy + dz * fz;
        if (z < 0.1) return null;
        double x = dx * rx + dy * ry + dz * rz;
        double y = dx * ux + dy * uy + dz * uz;
        double fov = Math.toRadians(MinecraftClient.getInstance().options.getFov().getValue());
        double f = 1.0 / Math.tan(fov / 2.0);
        double aspect = (double) sw / sh;
        return new float[]{
                (float) ((x / z) * f / aspect * 0.5 + 0.5) * sw,
                (float) (0.5 - (y / z) * f * 0.5) * sh
        };
    }

    /** То же, но в физических пикселях окна (для отрисовки без «квадратиков»). */
    public static float[] projectPx(double wx, double wy, double wz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        float[] p = project(wx, wy, wz, sw, sh);
        if (p == null) return null;
        p[0] *= scale;
        p[1] *= scale;
        if (Math.abs(p[0]) > 6000 || Math.abs(p[1]) > 6000) return null;
        return p;
    }
}
