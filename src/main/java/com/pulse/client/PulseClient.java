package com.pulse.client;

import com.pulse.client.module.ModuleManager;
import com.pulse.client.ui.ClickGui;
import com.pulse.client.ui.HudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class PulseClient implements ClientModInitializer {
    public static final String MOD_ID = "pulsevisuals";
    private static KeyBinding openGui;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        // Клавиша открытия меню (Right Shift)
        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.wasPressed()) mc.setScreen(new ClickGui());
            ModuleManager.tick(mc);
        });

        // HUD
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of(MOD_ID, "hud"),
                HudRenderer::render);

        // Эффекты при ударе
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && ModuleManager.HIT_EFFECTS.enabled) {
                Vec3d p = entity.getEntityPos().add(0, entity.getHeight() / 2.0, 0);
                for (int i = 0; i < 18; i++) {
                    double dx = (world.random.nextDouble() - 0.5) * 0.6;
                    double dy = (world.random.nextDouble() - 0.2) * 0.5;
                    double dz = (world.random.nextDouble() - 0.5) * 0.6;
                    world.addParticleClient(ParticleTypes.CRIT, p.x, p.y, p.z, dx, dy, dz);
                }
            }
            return ActionResult.PASS;
        });
    }
}
