package com.pulse.client.ui;

import com.pulse.client.module.Module;
import com.pulse.client.module.ModuleManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGui extends Screen {
    private static final int W = 130, ROW = 16, HEADER = 18;

    public ClickGui() {
        super(Text.literal("Pulse"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int panelX() { return width / 2 - W / 2; }
    private int panelY() { return height / 2 - (HEADER + ModuleManager.MODULES.size() * ROW) / 2; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // рисует фон/блюр
        int x = panelX(), y = panelY();
        int totalH = HEADER + ModuleManager.MODULES.size() * ROW;

        ctx.fill(x, y, x + W, y + totalH, Theme.BG_DARK);
        for (int i = 0; i < W; i++) ctx.fill(x + i, y, x + i + 1, y + 2, Theme.accent(i * 6));
        ctx.drawText(textRenderer, "Pulse", x + 6, y + 6, Theme.accent(0), true);

        int ry = y + HEADER;
        for (Module m : ModuleManager.MODULES) {
            boolean hover = mouseX >= x && mouseX < x + W && mouseY >= ry && mouseY < ry + ROW;
            if (hover) ctx.fill(x, ry, x + W, ry + ROW, 0x30FFFFFF);
            if (m.enabled) ctx.fill(x, ry, x + 2, ry + ROW, Theme.accent(ry));
            ctx.drawText(textRenderer, m.name, x + 8, ry + 4,
                    m.enabled ? Theme.accent(ry) : Theme.TEXT_DIM, true);
            if (hover) {
                ctx.drawText(textRenderer, m.description, x + W + 6, ry + 4, Theme.TEXT_DIM, true);
            }
            ry += ROW;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            int x = panelX(), ry = panelY() + HEADER;
            for (Module m : ModuleManager.MODULES) {
                if (click.x() >= x && click.x() < x + W && click.y() >= ry && click.y() < ry + ROW) {
                    m.toggle();
                    return true;
                }
                ry += ROW;
            }
        }
        return super.mouseClicked(click, doubled);
    }
}
