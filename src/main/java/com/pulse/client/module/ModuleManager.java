package com.pulse.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    public static Module WATERMARK, INFO, MODULE_LIST, FULLBRIGHT, TRAIL, HIT_EFFECTS;

    public static void init() {
        WATERMARK = add(new Module("Watermark", "Название клиента сверху слева") {}, true);
        INFO = add(new Module("Info", "FPS, пинг и координаты") {}, true);
        MODULE_LIST = add(new Module("ModuleList", "Анимированный список модулей") {}, true);

        FULLBRIGHT = add(new Module("Fullbright", "Полная яркость (ночное зрение)") {
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

        TRAIL = add(new Module("Trail", "Частицы-след за игроком") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null) return;
                Vec3d v = mc.player.getVelocity();
                if (v.horizontalLengthSquared() < 0.0004) return;
                Vec3d p = mc.player.getEntityPos();
                mc.world.addParticleClient(ParticleTypes.END_ROD,
                        p.x, p.y + 0.1, p.z, 0, 0.01, 0);
            }
        }, false);

        HIT_EFFECTS = add(new Module("HitEffects", "Частицы при ударе по сущности") {}, true);
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
