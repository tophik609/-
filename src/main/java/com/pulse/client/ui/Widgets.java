package com.pulse.client.ui;

import com.pulse.client.module.Comets;
import com.pulse.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.List;

/** Дополнительные HUD-виджеты: Keystrokes+CPS, Effects, Armor, Combo, цифры урона. */
public final class Widgets {
    private Widgets() {}

    private static final Random RNG = new Random();

    // ---- CPS ----
    private static final List<Long> CLICKS = new ArrayList<>();
    private static boolean wasAttack;

    // ---- Combo ----
    private static int combo;
    private static long lastHit;

    // ---- Damage numbers ----
    private static final class Dmg {
        double x, y, z;
        String text;
        int age;
    }
    private static final List<Dmg> DMGS = new ArrayList<>();
    private static LivingEntity watched;
    private static float watchedHp;

    // ---- Keystrokes ----
    private static final float[] KEY_ANIM = new float[6];

    public static void onHit() {
        long now = System.currentTimeMillis();
        if (now - lastHit > 3000) combo = 0;
        combo++;
        lastHit = now;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) return;

        boolean atk = mc.options.attackKey.isPressed();
        long now = System.currentTimeMillis();
        if (atk && !wasAttack) CLICKS.add(now);
        wasAttack = atk;
        CLICKS.removeIf(t -> now - t > 1000);

        LivingEntity t = ModuleManager.lastTarget;
        if (t != null && t.isAlive()) {
            if (t != watched) { watched = t; watchedHp = t.getHealth(); }
            float hp = t.getHealth();
            if (hp < watchedHp - 0.01f && ModuleManager.DAMAGE_NUMBERS.enabled) {
                Dmg d = new Dmg();
                Vec3d p = t.getEntityPos();
                d.x = p.x + (RNG.nextDouble() - 0.5) * 0.7;
                d.y = p.y + t.getHeight() * 0.9;
                d.z = p.z + (RNG.nextDouble() - 0.5) * 0.7;
                d.text = String.format(Locale.ROOT, "-%.1f", watchedHp - hp);
                DMGS.add(d);
            }
            watchedHp = hp;
        } else {
            watched = null;
        }
        DMGS.removeIf(d -> ++d.age > 26);
    }

    public static void render(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        if (ModuleManager.KEYSTROKES.enabled) keystrokes(ctx, tr, mc);
        if (ModuleManager.EFFECTS_HUD.enabled) effects(ctx, tr, mc);
        if (ModuleManager.ARMOR_HUD.enabled) armor(ctx, mc);
        if (ModuleManager.COMBO.enabled) combo(ctx, tr);
        if (ModuleManager.DAMAGE_NUMBERS.enabled) damageNumbers(ctx, tr, mc);
    }

    // ------------------------------------------------------------------

    private static void keyCell(DrawContext ctx, TextRenderer tr, int idx, boolean pressed,
                                int x, int y, int w, int h, String label) {
        KEY_ANIM[idx] += ((pressed ? 1f : 0f) - KEY_ANIM[idx]) * 0.3f;
        float k = KEY_ANIM[idx];
        int bg = Theme.lerp(0x14141E, Theme.accent(idx * 30) & 0xFFFFFF, k * 0.6f) | 0xD0000000;
        Gfx.round(ctx, x, y, w, h, 4, bg);
        int tc = Theme.lerp(0x9A9AB0, 0xFFFFFF, k) | 0xFF000000;
        Fonts.draw(ctx, tr, label, x + w / 2 - Fonts.width(tr, label) / 2, y + h / 2 - 4, tc, false);
    }

    private static void keystrokes(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        int box = 18, g = 2;
        int x = 6, y = ctx.getScaledWindowHeight() - 104;
        keyCell(ctx, tr, 0, mc.options.forwardKey.isPressed(), x + box + g, y, box, box, "W");
        keyCell(ctx, tr, 1, mc.options.leftKey.isPressed(), x, y + box + g, box, box, "A");
        keyCell(ctx, tr, 2, mc.options.backKey.isPressed(), x + box + g, y + box + g, box, box, "S");
        keyCell(ctx, tr, 3, mc.options.rightKey.isPressed(), x + 2 * (box + g), y + box + g, box, box, "D");
        int my = y + 2 * (box + g);
        int mw = (3 * box + 2 * g - g) / 2;
        keyCell(ctx, tr, 4, mc.options.attackKey.isPressed(), x, my, mw, box, "LMB");
        keyCell(ctx, tr, 5, mc.options.useKey.isPressed(), x + mw + g, my, mw, box, "RMB");
        String cps = CLICKS.size() + " CPS";
        Fonts.draw(ctx, tr, cps, x, my + box + 3, Theme.TEXT_DIM, true);
    }

    private static String fmtTime(int ticks) {
        int s = ticks / 20;
        return (s / 60) + ":" + String.format(Locale.ROOT, "%02d", s % 60);
    }

    private static void effects(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        int y = 24;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            if (ModuleManager.FULLBRIGHT.enabled && e.getEffectType() == StatusEffects.NIGHT_VISION) continue;
            String name = e.getEffectType().value().getName().getString();
            String line = name + " " + (e.getAmplifier() + 1) + "  " + fmtTime(e.getDuration());
            int w = Fonts.width(tr, line) + 10;
            Gfx.round(ctx, 4, y, w, 12, 4, Theme.BG);
            ctx.fill(4, y + 2, 5, y + 10, Theme.accent(y));
            Fonts.draw(ctx, tr, line, 9, y + 2, Theme.TEXT, false);
            y += 14;
        }
    }

    private static void armor(DrawContext ctx, MinecraftClient mc) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int x = sw - 26, y = sh / 2 - 44;
        Gfx.round(ctx, x - 3, y - 3, 24, 4 * 21 + 3, 5, Theme.BG);
        for (int i = 0; i < slots.length; i++) {
            ItemStack st = mc.player.getEquippedStack(slots[i]);
            int iy = y + i * 21;
            if (st.isEmpty()) continue;
            ctx.drawItem(st, x, iy);
            int max = st.getMaxDamage();
            if (max > 0) {
                float frac = 1f - st.getDamage() / (float) max;
                int col = Theme.lerp(0xFF3B3B, 0x4ADE80, frac) | 0xFF000000;
                ctx.fill(x, iy + 17, x + 16, iy + 18, 0xFF2A2A3C);
                ctx.fill(x, iy + 17, x + Math.max(1, (int) (16 * frac)), iy + 18, col);
            }
        }
    }

    private static void combo(DrawContext ctx, TextRenderer tr) {
        long age = System.currentTimeMillis() - lastHit;
        if (combo < 2 || age > 3000) return;
        String text = "Combo x" + combo;
        int w = Fonts.width(tr, text) + 12;
        int x = ctx.getScaledWindowWidth() / 2 - w / 2;
        int y = ctx.getScaledWindowHeight() / 2 + 46;
        int alpha = age > 2400 ? (int) (255 * (3000 - age) / 600f) : 255;
        Gfx.round(ctx, x, y, w, 14, 5, Theme.withAlpha(0x101018, (int) (alpha * 0.75f)));
        Fonts.draw(ctx, tr, text, x + 6, y + 3, Theme.withAlpha(Theme.accent(0), alpha), false);
    }

    private static void damageNumbers(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (Dmg d : DMGS) {
            float[] p = Comets.project(mc, d.x, d.y + d.age * 0.035, d.z, sw, sh);
            if (p == null) continue;
            int alpha = Math.max(40, (int) (255 * (1f - d.age / 26f)));
            int color = Theme.withAlpha(0xFF5577, alpha);
            Fonts.draw(ctx, tr, d.text, (int) p[0] - Fonts.width(tr, d.text) / 2, (int) p[1], color, true);
        }
    }
}
