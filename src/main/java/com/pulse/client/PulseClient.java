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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
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

        // Свой прицел (если модуль включён — рисуем свой, иначе ванильный)
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (ctx, tc) -> {
            if (ModuleManager.CROSSHAIR.enabled) HudRenderer.drawCrosshair(ctx);
            else original.render(ctx, tc);
        });

        // Эффекты при ударе
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) ModuleManager.onAttack(entity);
            return ActionResult.PASS;
        });
    }
}
