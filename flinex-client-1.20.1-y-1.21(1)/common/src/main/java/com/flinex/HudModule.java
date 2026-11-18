package com.flinex;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public abstract class HudModule extends Module {
    /** true mientras el editor de HUD está abierto (muestra datos de ejemplo). */
    public static boolean preview = false;
    public int w = 60, h = 18;

    protected static final int BG = 0x90101018;

    public HudModule(String name, boolean on, int x, int y) {
        super(name, "HUD", on);
        this.x = x;
        this.y = y;
    }

    public abstract void render(DrawContext ctx, MinecraftClient mc, int accent);

    // ---------- Panel de texto (FPS, CPS, Ping, Coordenadas, Combo, Reach, Totems) ----------
    public static class TextHud extends HudModule {
        private final Supplier<List<String>> lines;
        private List<String> cache = List.of();
        private int t = 0;

        public TextHud(String name, boolean on, int x, int y, Supplier<List<String>> lines) {
            super(name, on, x, y);
            this.lines = lines;
        }

        @Override
        public void tick(MinecraftClient mc) {
            // se actualiza cada 3 ticks, no en cada frame: menos trabajo por frame
            if (mc.player != null && t++ % 3 == 0) cache = lines.get();
        }

        @Override
        public void render(DrawContext ctx, MinecraftClient mc, int accent) {
            int tw = 0;
            for (String s : cache) tw = Math.max(tw, mc.textRenderer.getWidth(s));
            w = tw + 14;
            h = Math.max(1, cache.size()) * 11 + 6;
            Draw.round(ctx, x, y, w, h, 3, BG);
            ctx.fill(x + 1, y + 3, x + 3, y + h - 3, accent);
            for (int i = 0; i < cache.size(); i++)
                ctx.drawTextWithShadow(mc.textRenderer, cache.get(i), x + 7, y + 4 + i * 11, 0xFFFFFFFF);
        }
    }

    // ---------- Keystrokes ----------
    public static class KeysHud extends HudModule {
        public KeysHud(String name, boolean on, int x, int y) { super(name, on, x, y); }

        @Override
        public void render(DrawContext ctx, MinecraftClient mc, int accent) {
            w = 58; h = 58;
            var o = mc.options;
            key(ctx, mc, x + 20, y, 18, 18, "W", o.forwardKey.isPressed(), accent);
            key(ctx, mc, x, y + 20, 18, 18, "A", o.leftKey.isPressed(), accent);
            key(ctx, mc, x + 20, y + 20, 18, 18, "S", o.backKey.isPressed(), accent);
            key(ctx, mc, x + 40, y + 20, 18, 18, "D", o.rightKey.isPressed(), accent);
            key(ctx, mc, x, y + 40, 28, 18, "LMB", o.attackKey.isPressed(), accent);
            key(ctx, mc, x + 30, y + 40, 28, 18, "RMB", o.useKey.isPressed(), accent);
        }

        private void key(DrawContext ctx, MinecraftClient mc, int kx, int ky, int kw, int kh,
                         String label, boolean down, int accent) {
            Draw.round(ctx, kx, ky, kw, kh, 3, down ? Draw.alpha(accent, 0.85f) : BG);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, label, kx + kw / 2, ky + kh / 2 - 4, 0xFFFFFFFF);
        }
    }

    // ---------- Armadura ----------
    public static class ArmorHud extends HudModule {
        private static final EquipmentSlot[] SLOTS = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        public ArmorHud(String name, boolean on, int x, int y) { super(name, on, x, y); }

        @Override
        public void render(DrawContext ctx, MinecraftClient mc, int accent) {
            w = 54; h = 4 * 18 + 4;
            Draw.round(ctx, x, y, w, h, 3, BG);
            if (mc.player == null) return;
            for (int i = 0; i < 4; i++) {
                ItemStack st = mc.player.getEquippedStack(SLOTS[i]);
                if (st.isEmpty()) continue;
                int iy = y + 2 + i * 18;
                ctx.drawItem(st, x + 3, iy);
                if (st.isDamageable()) {
                    int left = st.getMaxDamage() - st.getDamage();
                    float pct = left / (float) st.getMaxDamage();
                    int col = Draw.lerpColor(0xFFFF4040, 0xFF50FF70, pct);
                    ctx.drawTextWithShadow(mc.textRenderer, String.valueOf(left), x + 22, iy + 5, col);
                }
            }
        }
    }

    // ---------- Target HUD ----------
    public static class TargetHud extends HudModule {
        private float shown = 20f;

        public TargetHud(String name, boolean on, int x, int y) { super(name, on, x, y); }

        @Override
        public void render(DrawContext ctx, MinecraftClient mc, int accent) {
            w = 120; h = 38;
            LivingEntity t = FlinexClient.target;
            boolean live = t != null && t.isAlive() && System.currentTimeMillis() - FlinexClient.targetTime < 3000;
            if (!live && !preview) return;

            String name = live ? t.getName().getString() : "Jugador";
            float hp = live ? t.getHealth() : 14f;
            float max = live && t.getMaxHealth() > 0 ? t.getMaxHealth() : 20f;
            double dist = live && mc.player != null ? mc.player.distanceTo(t) : 3.1;
            shown += (hp - shown) * 0.12f;

            Draw.round(ctx, x, y, w, h, 4, BG);
            ctx.fill(x + 1, y + 4, x + 3, y + h - 4, accent);
            ctx.drawTextWithShadow(mc.textRenderer, name, x + 8, y + 5, 0xFFFFFFFF);

            int bx = x + 8, by = y + 17, bw = w - 16;
            Draw.round(ctx, bx, by, bw, 5, 2, 0x80000000);
            float ratio = MathHelper.clamp(shown / max, 0f, 1f);
            int fill = Math.max(2, (int) (bw * ratio));
            Draw.round(ctx, bx, by, fill, 5, 2, Draw.lerpColor(0xFFFF4040, 0xFF50FF70, ratio));

            ctx.drawTextWithShadow(mc.textRenderer,
                    String.format(Locale.ROOT, "%.1f HP  -  %.1f m", hp, dist), x + 8, y + 26, 0xFFC8C8D0);
        }
    }
}
