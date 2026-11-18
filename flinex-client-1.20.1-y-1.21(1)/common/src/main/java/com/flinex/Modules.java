package com.flinex;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;

import java.util.*;

public final class Modules {
    private Modules() {}

    public static final List<Module> ALL = new ArrayList<>();
    public static Module zoom, fullbright, sprint, boost;

    private static <T extends Module> T add(T m) { ALL.add(m); return m; }

    public static void init() {
        // ---- HUD ----
        add(new HudModule.TextHud("FPS", true, 6, 6,
                () -> List.of("FPS: " + FlinexClient.fps())));
        add(new HudModule.TextHud("CPS", true, 6, 27, () -> List.of("CPS: " + FlinexClient.cps())));
        add(new HudModule.TextHud("Ping", false, 6, 48, () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            int ping = 0;
            if (mc.getNetworkHandler() != null && mc.player != null) {
                var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) ping = e.getLatency();
            }
            return List.of("Ping: " + ping + " ms");
        }));
        add(new HudModule.TextHud("Coordenadas", true, 6, 69, () -> {
            var p = MinecraftClient.getInstance().player;
            return p == null ? List.of("XYZ: -")
                    : List.of(String.format(Locale.ROOT, "XYZ: %.0f %.0f %.0f", p.getX(), p.getY(), p.getZ()));
        }));
        add(new HudModule.TextHud("Combo", true, 6, 90, () -> List.of("Combo: " + FlinexClient.combo)));
        add(new HudModule.TextHud("Reach", true, 6, 111,
                () -> List.of(String.format(Locale.ROOT, "Reach: %.2f", FlinexClient.lastReach))));
        add(new HudModule.TextHud("Totems", false, 6, 132, () -> {
            var p = MinecraftClient.getInstance().player;
            int n = 0;
            if (p != null)
                for (int i = 0; i < p.getInventory().size(); i++)
                    if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) n += p.getInventory().getStack(i).getCount();
            return List.of("Totems: " + n);
        }));
        add(new HudModule.KeysHud("Keystrokes", true, 6, 156));
        add(new HudModule.ArmorHud("Armadura", true, 6, 222));
        add(new HudModule.TargetHud("Target HUD", true, 200, 60));

        // ---- Utilidad ----
        zoom = add(new Module("Zoom", "Utilidad", true) {});
        fullbright = add(new Module("Fullbright", "Utilidad", false) {
            @Override public void tick(MinecraftClient mc) {
                if (mc.player != null)
                    mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
            }
            @Override public void onDisable(MinecraftClient mc) {
                if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });
        sprint = add(new Module("Sprint automatico", "Utilidad", false) {
            @Override public void tick(MinecraftClient mc) {
                var p = mc.player;
                if (p != null && mc.options.forwardKey.isPressed() && !p.isSneaking()
                        && !p.horizontalCollision && p.getHungerManager().getFoodLevel() > 6)
                    p.setSprinting(true);
            }
        });

        // ---- Rendimiento ----
        boost = add(new Boost());
    }

    /** Baja ajustes pesados y limita FPS cuando la ventana no está enfocada. Restaura al apagarlo. */
    static class Boost extends Module {
        private ParticlesMode pm; private double ed; private CloudRenderMode cl;
        private int bb, fps; private boolean sh, applied, limited; private GraphicsMode gm;

        Boost() { super("FPS Boost", "Rendimiento", false); }

        @Override public void onEnable(MinecraftClient mc) {
            if (applied) return;
            GameOptions o = mc.options;
            pm = o.getParticles().getValue();
            ed = o.getEntityDistanceScaling().getValue();
            cl = o.getCloudRenderMode().getValue();
            bb = o.getBiomeBlendRadius().getValue();
            sh = o.getEntityShadows().getValue();
            gm = o.getGraphicsMode().getValue();
            o.getParticles().setValue(ParticlesMode.MINIMAL);
            o.getEntityDistanceScaling().setValue(0.5);
            o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            o.getBiomeBlendRadius().setValue(0);
            o.getEntityShadows().setValue(false);
            o.getGraphicsMode().setValue(GraphicsMode.FAST);
            applied = true;
        }

        @Override public void onDisable(MinecraftClient mc) {
            GameOptions o = mc.options;
            if (limited) { o.getMaxFps().setValue(fps); limited = false; }
            if (!applied) return;
            o.getParticles().setValue(pm);
            o.getEntityDistanceScaling().setValue(ed);
            o.getCloudRenderMode().setValue(cl);
            o.getBiomeBlendRadius().setValue(bb);
            o.getEntityShadows().setValue(sh);
            o.getGraphicsMode().setValue(gm);
            applied = false;
        }

        @Override public void tick(MinecraftClient mc) {
            boolean focused = mc.isWindowFocused();
            if (!focused && !limited) { fps = mc.options.getMaxFps().getValue(); mc.options.getMaxFps().setValue(15); limited = true; }
            else if (focused && limited) { mc.options.getMaxFps().setValue(fps); limited = false; }
        }
    }
}
