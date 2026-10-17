package com.pulse.client.module;

import com.pulse.client.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final String VISUALS = "Visuals";
    public static final String HUD = "HUD";

    public static final List<Module> MODULES = new ArrayList<>();

    public static Module WATERMARK, INFO, MODULE_LIST, TARGET_HUD,
            FULLBRIGHT, TRAIL, HIT_EFFECTS, JUMP_CIRCLE;

    /** Последняя цель удара — для Target HUD */
    public static LivingEntity lastTarget;
    public static long lastTargetTime;

    private static boolean wasOnGround = true;

    public static void init() {
        WATERMARK = add(new Module("Watermark", "Плашка Pulse сверху по центру", HUD) {}, true);
        INFO = add(new Module("Info", "Координаты внизу слева", HUD) {}, true);
        MODULE_LIST = add(new Module("ModuleList", "Список включённых модулей", HUD) {}, true);
        TARGET_HUD = add(new Module("TargetHUD", "Карточка цели с HP", HUD) {}, true);

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

        HIT_EFFECTS = add(new Module("Hit Effects", "Спираль частиц при ударе", VISUALS) {}, true);

        JUMP_CIRCLE = add(new Module("Jump Circle", "Кольцо под ногами при прыжке", VISUALS) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null) return;
                boolean ground = mc.player.isOnGround();
                if (wasOnGround && !ground && mc.player.getVelocity().y > 0) {
                    Vec3d p = mc.player.getEntityPos();
                    for (int i = 0; i < 36; i++) {
                        double a = i * Math.PI * 2 / 36;
                        mc.world.addParticleClient(dust(1.2f),
                                p.x + Math.cos(a) * 0.85, p.y + 0.05, p.z + Math.sin(a) * 0.85,
                                0, 0, 0);
                    }
                }
                wasOnGround = ground;
            }
        }, false);
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
        if (!HIT_EFFECTS.enabled) return;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;

        Vec3d p = target.getEntityPos();
        double h = target.getHeight();
        // спираль вокруг цели
        for (int i = 0; i < 40; i++) {
            double a = i * 0.55;
            double y = p.y + (i / 40.0) * h;
            double r = 0.55;
            world.addParticleClient(dust(1.1f),
                    p.x + Math.cos(a) * r, y, p.z + Math.sin(a) * r, 0, 0, 0);
        }
        for (int i = 0; i < 10; i++) {
            world.addParticleClient(ParticleTypes.CRIT, p.x, p.y + h / 2, p.z,
                    (world.random.nextDouble() - 0.5) * 0.6,
                    (world.random.nextDouble() - 0.2) * 0.5,
                    (world.random.nextDouble() - 0.5) * 0.6);
        }
    }

    private static Module add(Module m, boolean on) {
        MODULES.add(m);
        if (on) m.enabled = true;
        return m;
    }

    public static void tick(MinecraftClient mc) {
        for (Module m : MODULES) {
            m.tickAnim();
            if (m.enabled) m.onTick(mc);
        }
    }
}
