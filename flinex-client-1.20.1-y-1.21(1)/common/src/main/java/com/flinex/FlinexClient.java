package com.flinex;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public class FlinexClient implements ClientModInitializer {
    public static KeyBinding menuKey, zoomKey;
    public static int combo = 0;
    public static double lastReach = 0;
    public static LivingEntity target;
    public static long targetTime = 0;
    public static float zoomFactor = 1f;

    private static int lastHurt = 0;
    private static boolean wasAttack = false, started = false;
    private static final Deque<Long> clicks = new ArrayDeque<>();

    public static int cps() { return clicks.size(); }
    public static int fps() { return fps; }
    private static int fps = 0, frames = 0;
    private static long fpsStamp = System.currentTimeMillis();

    @Override
    public void onInitializeClient() {
        Modules.init();
        ClientConfig.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flinex.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.flinex"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flinex.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.flinex"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!started) {
                started = true;
                for (Module m : Modules.ALL) if (m.enabled) m.onEnable(mc);
            }
            while (menuKey.wasPressed()) mc.setScreen(new MenuScreen());

            boolean atk = mc.options.attackKey.isPressed();
            if (atk && !wasAttack) clicks.add(System.currentTimeMillis());
            wasAttack = atk;
            long now = System.currentTimeMillis();
            while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();

            if (mc.player != null) {
                if (mc.player.hurtTime > lastHurt) combo = 0;
                lastHurt = mc.player.hurtTime;
            }

            for (Module m : Modules.ALL) if (m.enabled) m.tick(mc);

            float tgt = (Modules.zoom.enabled && zoomKey.isPressed() && mc.currentScreen == null) ? 0.25f : 1f;
            zoomFactor += (tgt - zoomFactor) * 0.4f;
            if (Math.abs(tgt - zoomFactor) < 0.002f) zoomFactor = tgt;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity le) {
                combo++;
                target = le;
                targetTime = System.currentTimeMillis();
                Vec3d eye = player.getEyePos();
                Box b = le.getBoundingBox();
                Vec3d closest = new Vec3d(
                        MathHelper.clamp(eye.x, b.minX, b.maxX),
                        MathHelper.clamp(eye.y, b.minY, b.maxY),
                        MathHelper.clamp(eye.z, b.minZ, b.maxZ));
                lastReach = eye.distanceTo(closest);
            }
            return ActionResult.PASS;
        });

        // restaura los ajustes de video si el FPS Boost estaba activo al cerrar
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            for (Module m : Modules.ALL) if (m.enabled) m.onDisable(mc);
        });

        HudRenderCallback.EVENT.register((ctx, tick) -> renderHud(ctx));
    }

    private static void renderHud(DrawContext ctx) {
        frames++;
        long nowMs = System.currentTimeMillis();
        if (nowMs - fpsStamp >= 1000) { fps = frames; frames = 0; fpsStamp = nowMs; }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.currentScreen instanceof HudEditorScreen) return;
        int accent = Draw.accent();
        for (Module m : Modules.ALL)
            if (m instanceof HudModule h && h.enabled) h.render(ctx, mc, accent);
    }
}
