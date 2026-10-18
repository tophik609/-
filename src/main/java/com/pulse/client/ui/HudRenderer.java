package com.pulse.client.ui;

import com.pulse.client.module.Module;
import com.pulse.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HudRenderer {
    private HudRenderer() {}

    private static float shownHp = -1;
    private static LivingEntity shownTarget;

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;

        if (ModuleManager.WATERMARK.enabled) drawWatermark(ctx, tr, mc);
        if (ModuleManager.INFO.enabled) drawInfo(ctx, tr, mc);
        if (ModuleManager.TARGET_HUD.enabled) drawTarget(ctx, tr, mc);
        if (ModuleManager.TARGET_TAG.enabled) drawTargetTag(ctx, tr, mc);
        drawModuleList(ctx, tr);
        Widgets.render(ctx, tr, mc);
        if (ModuleManager.HIT_EFFECTS.enabled) com.pulse.client.module.Comets.render(ctx, mc);
    }

    /** Свой прицел: точка в центре и четыре засечки. */
    public static void drawCrosshair(DrawContext ctx) {
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        int c = Theme.accent(0);
        int dark = 0x90000000;
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, dark);
        ctx.fill(cx, cy, cx + 1, cy + 1, c);
        int[][] arms = {{-6, 0, -3, 1}, {4, 0, 7, 1}, {0, -6, 1, -3}, {0, 4, 1, 7}};
        for (int[] a : arms) {
            ctx.fill(cx + a[0] - 1, cy + a[1] - 1, cx + a[2] + 1, cy + a[3] + 1, dark);
            ctx.fill(cx + a[0], cy + a[1], cx + a[2], cy + a[3], c);
        }
    }

    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) return e.getLatency();
        }
        return 0;
    }

    /** Плашка сверху по центру: pulsevisuals | ping | FPS */
    private static void drawWatermark(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        String a = "pulsevisuals";
        String b = ping(mc) + " ms";
        String c = mc.getCurrentFps() + " FPS";
        int w = 10 + 8 + tr.getWidth(a) + 8 + tr.getWidth(b) + 8 + tr.getWidth(c) + 6;
        int x = ctx.getScaledWindowWidth() / 2 - w / 2, y = 4, h = 14;
        Gfx.round(ctx, x, y, w, h, 5, Theme.BG);
        // градиентная линия снизу плашки
        for (int i = 4; i < w - 4; i++) ctx.fill(x + i, y + h - 1, x + i + 1, y + h, Theme.accent(i * 5));
        // акцентная точка
        Gfx.round(ctx, x + 5, y + 4, 6, 6, 3, Theme.accent(0));
        int cx = x + 16;
        ctx.drawText(tr, a, cx, y + 3, Theme.TEXT, false);
        cx += tr.getWidth(a) + 8;
        ctx.drawText(tr, b, cx, y + 3, Theme.TEXT_DIM, false);
        cx += tr.getWidth(b) + 8;
        ctx.drawText(tr, c, cx, y + 3, Theme.TEXT_DIM, false);
    }

    private static void drawInfo(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        String line = "XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
        int w = tr.getWidth(line) + 10;
        int x = 4, y = ctx.getScaledWindowHeight() - 16;
        Gfx.round(ctx, x, y, w, 12, 4, Theme.BG);
        ctx.drawText(tr, line, x + 5, y + 2, Theme.TEXT, false);
    }

    private static LivingEntity pickTarget(MinecraftClient mc) {
        LivingEntity t = ModuleManager.lastTarget;
        if (t != null && t.isAlive() && System.currentTimeMillis() - ModuleManager.lastTargetTime < 4000) {
            return t;
        }
        if (mc.targetedEntity instanceof PlayerEntity p) return p;
        return null;
    }

    /** Карточка цели: ник, HP и плавная полоска здоровья. */
    private static void drawTarget(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        LivingEntity t = pickTarget(mc);
        if (t == null) { shownTarget = null; shownHp = -1; return; }
        if (t != shownTarget) { shownTarget = t; shownHp = t.getHealth(); }
        shownHp += (t.getHealth() - shownHp) * 0.15f;

        float max = Math.max(1f, t.getMaxHealth());
        float frac = Math.max(0f, Math.min(1f, shownHp / max));
        String name = t.getName().getString();
        String hp = String.format("HP / %.1f", t.getHealth());

        int w = 118, h = 34;
        int x = ctx.getScaledWindowWidth() / 2 + 28;
        int y = ctx.getScaledWindowHeight() / 2 + 28;

        Gfx.round(ctx, x, y, w, h, 6, Theme.BG_DARK);
        // «аватар» — квадрат с первой буквой ника
        Gfx.round(ctx, x + 5, y + 5, 20, 20, 4, Theme.withAlpha(Theme.accent(0), 0x55));
        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        ctx.drawText(tr, letter, x + 15 - tr.getWidth(letter) / 2, y + 11, Theme.TEXT, false);

        ctx.drawText(tr, name, x + 30, y + 6, Theme.TEXT, false);
        ctx.drawText(tr, hp, x + 30, y + 16, Theme.TEXT_DIM, false);

        // полоска HP
        int bx = x + 5, by = y + h - 7, bw = w - 10;
        Gfx.round(ctx, bx, by, bw, 3, 1, 0xFF2A2A3C);
        int fill = Math.max(2, (int) (bw * frac));
        for (int i = 0; i < fill; i++) {
            ctx.fill(bx + i, by, bx + i + 1, by + 3, Theme.accent(i * 3));
        }
    }

    // ---- метка над головой цели (проекция мира на экран без миксинов) ----
    private static float tagX = -1, tagY = -1;

    private static void drawTargetTag(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        LivingEntity t = pickTarget(mc);
        if (t == null || t == mc.player) { tagX = -1; return; }

        net.minecraft.util.math.Vec3d eye = mc.player.getEyePos();
        net.minecraft.util.math.Vec3d head = t.getEntityPos().add(0, t.getHeight() + 0.45, 0);
        double dx = head.x - eye.x, dy = head.y - eye.y, dz = head.z - eye.z;

        double yaw = Math.toRadians(mc.player.getYaw());
        double pitch = Math.toRadians(mc.player.getPitch());
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

        double z = dx * fx + dy * fy + dz * fz;
        if (z < 0.2) { tagX = -1; return; }
        double x = dx * rx + dy * ry + dz * rz;
        double y = dx * ux + dy * uy + dz * uz;

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        double fov = Math.toRadians(mc.options.getFov().getValue());
        double f = 1.0 / Math.tan(fov / 2.0);
        double aspect = (double) sw / sh;
        float px = (float) ((x / z) * f / aspect * 0.5 + 0.5) * sw;
        float py = (float) (0.5 - (y / z) * f * 0.5) * sh;

        // сглаживание, чтобы метка не дёргалась на 20 тиках
        if (tagX < 0) { tagX = px; tagY = py; }
        tagX += (px - tagX) * 0.5f;
        tagY += (py - tagY) * 0.5f;

        float hp = t.getHealth(), max = Math.max(1f, t.getMaxHealth());
        int hearts = Math.min(10, (int) Math.ceil(max / 2f));
        String heart = "\u2665";
        int hw = tr.getWidth(heart);
        int rowW = hearts * hw;
        int cx = (int) tagX, cy = (int) tagY;

        // сердца
        int hx = cx - rowW / 2;
        int hy = cy - 28;
        for (int i = 0; i < hearts; i++) {
            float need = hp / max * hearts * 2f - i * 2f; // сколько "половинок" в этом сердце
            int col = need >= 2f ? 0xFFFF4D6D : (need >= 1f ? 0xFFB0364C : 0xFF3A3A48);
            ctx.drawText(tr, heart, hx + i * hw, hy, col, true);
        }

        // ник
        String name = t.getName().getString();
        ctx.drawText(tr, name, cx - tr.getWidth(name) / 2, hy + 11, Theme.TEXT, true);

        // N Здоровье
        String hpText = (int) Math.ceil(hp) + " \u0417\u0434\u043e\u0440\u043e\u0432\u044c\u0435";
        ctx.drawText(tr, hpText, cx - tr.getWidth(hpText) / 2, hy + 22, Theme.accent(0), true);
    }

    private static void drawModuleList(DrawContext ctx, TextRenderer tr) {
        List<Module> visible = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            if (m.anim > 0f && m != ModuleManager.MODULE_LIST) visible.add(m);
        }
        if (!ModuleManager.MODULE_LIST.enabled) return;
        visible.sort(Comparator.comparingInt((Module m) -> tr.getWidth(m.name)).reversed());

        int sw = ctx.getScaledWindowWidth();
        int y = 4, idx = 0;
        for (Module m : visible) {
            int tw = tr.getWidth(m.name);
            int boxW = tw + 8;
            int x = sw - (int) (boxW * m.anim) - 2;
            int alpha = (int) (255 * m.anim);
            int color = Theme.withAlpha(Theme.accent(idx * 40), alpha);

            ctx.fill(x, y, sw, y + 11, Theme.withAlpha(Theme.BG, (int) (0xC0 * m.anim)));
            ctx.fill(sw - 2, y, sw, y + 11, color);
            ctx.drawText(tr, m.name, x + 4, y + 2, color, false);

            y += (int) (11 * m.anim);
            idx++;
        }
    }
}
