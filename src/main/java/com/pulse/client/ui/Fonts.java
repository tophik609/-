package com.pulse.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Гладкий TTF-шрифт интерфейса вместо пиксельного ванильного. */
public final class Fonts {
    private Fonts() {}

    private static final Style STYLE = Style.EMPTY.withFont(
            new StyleSpriteSource.Font(Identifier.of("pulsevisuals", "ui")));
    private static final Map<String, Text> CACHE = new HashMap<>();

    private static Text text(String s) {
        Text t = CACHE.get(s);
        if (t == null) {
            if (CACHE.size() > 600) CACHE.clear();
            t = Text.literal(s).setStyle(STYLE);
            CACHE.put(s, t);
        }
        return t;
    }

    public static int width(TextRenderer tr, String s) {
        return tr.getWidth(text(s));
    }

    public static void draw(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        ctx.drawText(tr, text(s), x, y, color, shadow);
    }
}
