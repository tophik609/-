package com.pulse.client.module;

import com.pulse.client.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final String VISUALS = "Visuals";
    public static final String HUD = "HUD";

    public static final List<Module> MODULES = new ArrayList<>();

    public static Module WATERMARK, INFO, MODULE_LIST, TARGET_HUD, TARGET_TAG,
            FULLBRIGHT, TRAIL, HIT_EFFECTS, JUMP_CIRCLE, CROSSHAIR, HIT_SOUNDS,
            KEYSTROKES, EFFECTS_HUD, ARMOR_HUD, COMBO, DAMAGE_NUMBERS, CHINA_HAT;

    /** Последняя цель удара — для Target HUD */
    public static LivingEntity lastTarget;
    public static long lastTargetTime;

    private static boolean wasOnGround = true;

    public static void init() {
        WATERMARK = add(new Module("Watermark", "Плашка Pulse сверху по центру", HUD) {}, true);
        INFO = add(new Module("Info", "Координаты внизу слева", HUD) {}, true);
        MODULE_LIST = add(new Module("ModuleList", "Список включённых модулей", HUD) {}, true);
        TARGET_HUD = add(new Module("TargetHUD", "Карточка цели с HP", HUD) {}, true);

        KEYSTROKES = add(new Module("Keystrokes", "WASD, мышь и CPS", HUD) {}, true);
        EFFECTS_HUD = add(new Module("Effects", "Активные эффекты с таймером", HUD) {}, true);
        ARMOR_HUD = add(new Module("Armor HUD", "Броня и прочность справа", HUD) {}, true);
        COMBO = add(new Module("Combo", "Счётчик комбо ударов", HUD) {}, true);
        DAMAGE_NUMBERS = add(new Module("Damage Numbers", "Всплывающий урон над целью", VISUALS) {}, true);

        TARGET_TAG = add(new Module("Target Tag", "Сердца и здоровье над головой цели", VISUALS) {}, true);

        FULLBRIGHT = add(new Module("Full Bright", "Полная яркость", VISUALS) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null) {
                    mc.player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
                }
            }
            @Override public void onDisable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }, false);

        TRAIL = add(new Module("Trail", "Частицы-след за игроком", VISUALS) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null) return;
                if (mc.player.getVelocity().horizontalLengthSquared() < 0.0004) return;
                Vec3d p = mc.player.getEntityPos();
                mc.world.addParticleClient(dust(1.0f), p.x, p.y + 0.1, p.z, 0, 0, 0);
            }
        }, false);

        HIT_EFFECTS = add(new Module("Hit Effects", "Светящиеся штрихи при ударе", VISUALS) {}, true);
        CROSSHAIR = add(new Module("Crosshair", "Свой прицел вместо ванильного", VISUALS) {}, false);
        HIT_SOUNDS = add(new Module("Hit Sounds", "Звук при ударе", VISUALS) {}, false);

        JUMP_CIRCLE = add(new Module("Jump Circle", "Кольца под ногами при прыжке", VISUALS) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null) return;
                boolean ground = mc.player.isOnGround();
                if (wasOnGround && !ground && mc.player.getVelocity().y > 0) {
                    Vec3d p = mc.player.getEntityPos();
                    Rings.spawn(p.x, p.y, p.z);
                }
                wasOnGround = ground;
            }
        }, true);

        CHINA_HAT = add(new Module("China Hat", "Светящаяся шляпа над головой (3-е лицо)", VISUALS) {}, true);
    }

    private static DustParticleEffect dust(float scale) {
        return new DustParticleEffect(Theme.accent(0) & 0xFFFFFF, scale);
    }

    /** Вызывается при ударе по сущности. */
    public static void onAttack(Entity target) {
        if (target instanceof LivingEntity le) {
            lastTarget = le;
            lastTargetTime = System.currentTimeMillis();
        }
        if (HIT_SOUNDS.enabled) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
            }
        }
        com.pulse.client.ui.Widgets.onHit();
        if (HIT_EFFECTS.enabled) Comets.burst(target);
    }

    private static Module add(Module m, boolean on) {
        MODULES.add(m);
        if (on) m.enabled = true;
        return m;
    }

    public static void tick(MinecraftClient mc) {
        Comets.tick();
        Rings.tick();
        com.pulse.client.ui.Widgets.tick(mc);
        for (Module m : MODULES) {
            m.tickAnim();
            if (m.enabled) m.onTick(mc);
        }
    }
}
