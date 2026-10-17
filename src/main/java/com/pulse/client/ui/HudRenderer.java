package com.pulse.client.ui;

import com.pulse.client.module.Module;
import com.pulse.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HudRenderer {
    private HudRenderer() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;

        if (ModuleManager.WATERMARK.enabled) drawWatermark(ctx, tr);
        if (ModuleManager.INFO.enabled) drawInfo(ctx, tr, mc);
        if (ModuleManager.MODULE_LIST.enabled || anyListVisible()) drawModuleList(ctx, tr);
    }

    private static boolean anyListVisible() {
        return ModuleManager.MODULES.stream().anyMatch(m -> m.anim > 0f);
    }

    private static void drawWatermark(DrawContext ctx, TextRenderer tr) {
        String text = "Pulse";
        int w = tr.getWidth(text) + 12;
        int x = 4, y = 4, h = 14;
        ctx.fill(x, y, x + w, y + h, Theme.BG);
        // градиентная линия сверху
        for (int i = 0; i < w; i++) {
            ctx.fill(x + i, y, x + i + 1, y + 1, Theme.accent(i * 6));
        }
        ctx.drawText(tr, text, x + 6, y + 3, Theme.accent(0), true);
    }

    private static void drawInfo(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String line = "FPS " + mc.getCurrentFps()
                + "  |  Ping " + ping + "ms"
                + "  |  XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
        int w = tr.getWidth(line) + 10;
        int x = 4, y = ctx.getScaledWindowHeight() - 16;
        ctx.fill(x, y, x + w, y + 12, Theme.BG);
        ctx.fill(x, y, x + 1, y + 12, Theme.accent(0));
        ctx.drawText(tr, line, x + 5, y + 2, Theme.TEXT, true);
    }

    private static void drawModuleList(DrawContext ctx, TextRenderer tr) {
        List<Module> visible = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            if (m.anim > 0f && m != ModuleManager.MODULE_LIST) visible.add(m);
        }
        visible.sort(Comparator.comparingInt((Module m) -> tr.getWidth(m.name)).reversed());

        int sw = ctx.getScaledWindowWidth();
        int y = 4;
        int idx = 0;
        for (Module m : visible) {
            int tw = tr.getWidth(m.name);
            int boxW = tw + 8;
            // выезд справа с плавной анимацией
            int x = sw - (int) (boxW * m.anim) - 2;
            int alpha = (int) (255 * m.anim);
            int color = Theme.withAlpha(Theme.accent(idx * 40), alpha);

            ctx.fill(x, y, sw, y + 11, Theme.withAlpha(Theme.BG, (int) (0xB0 * m.anim)));
            ctx.fill(sw - 2, y, sw, y + 11, color);
            ctx.drawText(tr, m.name, x + 4, y + 2, color, true);

            y += (int) (11 * m.anim);
            idx++;
        }
    }
}
