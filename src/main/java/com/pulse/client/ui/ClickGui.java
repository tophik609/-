package com.pulse.client.ui;

import com.pulse.client.module.Module;
import com.pulse.client.module.ModuleManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Тёмное меню в стиле Pulse: переключатели в 2 колонки, вкладки снизу, пресеты цвета. */
public class ClickGui extends Screen {
    private static final int PW = 300, PH = 244;
    private static final int COL_W = 134, ROW_H = 24, GAP = 6, PAD = 12;
    private static final String[] TABS = {ModuleManager.VISUALS, ModuleManager.HUD, "Presets"};

    private static int tab = 0;

    private final long openedAt = System.currentTimeMillis();

    public ClickGui() {
        super(Text.literal("PulseVisuals"));
    }

    private float openK() {
        float k = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
        return 1f - (1f - k) * (1f - k);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int px() { return width / 2 - PW / 2; }
    private int py() { return height / 2 - PH / 2 + (int) ((1f - openK()) * 16); }

    private List<Module> tabModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) if (m.category.equals(TABS[tab])) list.add(m);
        return list;
    }

    // ---- геометрия вкладок внизу ----
    private int tabX(int i) { return px() + PW / 2 - (TABS.length * 62) / 2 + i * 62; }
    private int tabY() { return py() + PH - 24; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        com.pulse.client.module.Cam.update(net.minecraft.client.MinecraftClient.getInstance());
        int x = px(), y = py();
        int accent = Theme.accent(0);

        Gfx.round(ctx, x, y, PW, PH, 8, Theme.BG_DARK);

        // заголовок
        String title = "PulseVisuals";
        int tw = Fonts.width(textRenderer, title);
        int tx = x + PW / 2 - (tw + 14) / 2;
        Gfx.round(ctx, tx, y + 11, 8, 8, 2, accent);
        Fonts.draw(ctx, textRenderer, title, tx + 14, y + 11, Theme.TEXT, false);
        ctx.fill(x + PAD, y + 28, x + PW - PAD, y + 29, 0x22FFFFFF);

        if (TABS[tab].equals("Presets")) renderPresets(ctx, mx, my, x, y);
        else renderModules(ctx, mx, my, x, y, accent);

        // вкладки
        for (int i = 0; i < TABS.length; i++) {
            int bx = tabX(i), by = tabY();
            boolean sel = i == tab;
            boolean hov = mx >= bx && mx < bx + 58 && my >= by && my < by + 16;
            Gfx.round(ctx, bx, by, 58, 16, 6,
                    sel ? Theme.withAlpha(accent, 0x55) : (hov ? Theme.CARD_HOVER : Theme.CARD));
            int cw = Fonts.width(textRenderer, TABS[i]);
            Fonts.draw(ctx, textRenderer, TABS[i], bx + 29 - cw / 2, by + 4,
                    sel ? Theme.TEXT : Theme.TEXT_DIM, false);
        }
    }

    private void renderModules(DrawContext ctx, int mx, int my, int x, int y, int accent) {
        List<Module> list = tabModules();
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int cx = x + PAD + (i % 2) * (COL_W + GAP);
            int cy = y + 36 + (i / 2) * (ROW_H + GAP);
            boolean hov = mx >= cx && mx < cx + COL_W && my >= cy && my < cy + ROW_H;
            Gfx.round(ctx, cx, cy, COL_W, ROW_H, 6, hov ? Theme.CARD_HOVER : Theme.CARD);
            Fonts.draw(ctx, textRenderer, m.name, cx + 8, cy + 8, Theme.TEXT, false);
            Gfx.switchToggle(ctx, cx + COL_W - 30, cy + 7, m.anim, accent);
            if (hov) {
                Fonts.draw(ctx, textRenderer, m.description, x + PAD, y + PH - 40, Theme.TEXT_DIM, false);
            }
        }
    }

    private void renderPresets(DrawContext ctx, int mx, int my, int x, int y) {
        Fonts.draw(ctx, textRenderer, "Цветовые пресеты", x + PAD, y + 36, Theme.TEXT_DIM, false);
        for (int i = 0; i < Theme.PRESET_NAMES.length; i++) {
            int cy = y + 50 + i * (ROW_H + GAP);
            int cx = x + PAD, cw = PW - PAD * 2;
            boolean sel = Theme.preset == i;
            boolean hov = mx >= cx && mx < cx + cw && my >= cy && my < cy + ROW_H;
            Gfx.round(ctx, cx, cy, cw, ROW_H, 6, hov ? Theme.CARD_HOVER : Theme.CARD);
            if (sel) ctx.fill(cx, cy + 4, cx + 2, cy + ROW_H - 4, Theme.presetColor(i, 0));
            Fonts.draw(ctx, textRenderer, Theme.PRESET_NAMES[i], cx + 10, cy + 8, Theme.TEXT, false);
            // градиентная полоска-превью справа
            int gx = cx + cw - 70, gw = 60;
            for (int k = 0; k < gw; k++) {
                int col = Theme.lerp(Theme.presetColor(i, 0), Theme.presetColor(i, 1), k / (float) gw);
                ctx.fill(gx + k, cy + 9, gx + k + 1, cy + 15, col | 0xFF000000);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double mx = click.x(), my = click.y();
            int x = px(), y = py();

            for (int i = 0; i < TABS.length; i++) {
                int bx = tabX(i), by = tabY();
                if (mx >= bx && mx < bx + 58 && my >= by && my < by + 16) { tab = i; return true; }
            }

            if (TABS[tab].equals("Presets")) {
                for (int i = 0; i < Theme.PRESET_NAMES.length; i++) {
                    int cy = y + 50 + i * (ROW_H + GAP);
                    int cx = x + PAD, cw = PW - PAD * 2;
                    if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ROW_H) {
                        Theme.preset = i;
                        return true;
                    }
                }
            } else {
                List<Module> list = tabModules();
                for (int i = 0; i < list.size(); i++) {
                    int cx = x + PAD + (i % 2) * (COL_W + GAP);
                    int cy = y + 36 + (i / 2) * (ROW_H + GAP);
                    if (mx >= cx && mx < cx + COL_W && my >= cy && my < cy + ROW_H) {
                        list.get(i).toggle();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }
}
