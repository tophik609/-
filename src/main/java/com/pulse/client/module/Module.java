package com.pulse.client.module;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    public final String name;
    public final String description;
    public boolean enabled;
    /** 0..1 — плавная анимация для списка модулей */
    public float anim;

    protected Module(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient mc) {}

    public final void tickAnim() {
        float target = enabled ? 1f : 0f;
        anim += (target - anim) * 0.22f;
        if (Math.abs(target - anim) < 0.01f) anim = target;
    }
}
