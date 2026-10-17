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
        drawModuleList(ctx, tr);
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
