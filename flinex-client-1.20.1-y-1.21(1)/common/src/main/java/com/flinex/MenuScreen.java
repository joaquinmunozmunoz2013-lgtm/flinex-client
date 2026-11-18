package com.flinex;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.BooleanSupplier;

public class MenuScreen extends BaseScreen {
    private static final String[] TABS = {"HUD", "Utilidad", "Rendimiento", "Ajustes"};
    private static final int PW = 380, PH = 250;

    private record Card(String label, boolean toggle, BooleanSupplier state, Runnable click) {}

    private int tab = 0;
    private float open = 0f, ease = 0f;
    private long lastNs = 0;
    private final Map<String, Float> anims = new HashMap<>();

    public MenuScreen() { super(Text.literal("Flinex Client")); }

    private int ox() { return (width - PW) / 2; }
    private int oy() { return (height - PH) / 2 + (int) ((1 - ease) * 14); }
    private int cardX(int i) { return ox() + 104 + (i % 2) * 140; }
    private int cardY(int i) { return oy() + 40 + (i / 2) * 28; }

    private List<Card> cards() {
        List<Card> l = new ArrayList<>();
        if (tab < 3) {
            for (Module m : Modules.ALL)
                if (m.category.equals(TABS[tab])) l.add(new Card(m.name, true, () -> m.enabled, m::toggle));
        } else {
            l.add(new Card("Arcoíris", true, () -> ClientConfig.rainbow, () -> {
                ClientConfig.rainbow = !ClientConfig.rainbow;
                ClientConfig.save();
            }));
            l.add(new Card("Cambiar color", false, () -> false, this::nextColor));
            l.add(new Card("Editar HUD", false, () -> false,
                    () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen())));
        }
        return l;
    }

    private void nextColor() {
        int idx = 0;
        for (int i = 0; i < ClientConfig.PRESETS.length; i++) if (ClientConfig.PRESETS[i] == ClientConfig.accent) idx = i;
        ClientConfig.accent = ClientConfig.PRESETS[(idx + 1) % ClientConfig.PRESETS.length];
        ClientConfig.rainbow = false;
        ClientConfig.save();
    }

    @Override
    protected void draw(DrawContext ctx, int mx, int my, float delta) {
        long n = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min((n - lastNs) / 1e9f, 0.05f);
        lastNs = n;
        open = Math.min(1f, open + dt * 5f);
        ease = 1 - (1 - open) * (1 - open);

        int ox = ox(), oy = oy(), acc = Draw.accent();
        Draw.round(ctx, ox, oy, PW, PH, 6, Draw.alpha(0xF0101018, ease));
        ctx.drawTextWithShadow(textRenderer, "Flinex Client", ox + 14, oy + 12, acc);
        ctx.fill(ox + 8, oy + 28, ox + PW - 8, oy + 29, Draw.alpha(0x30FFFFFF, ease));

        for (int i = 0; i < TABS.length; i++) {
            int ty = oy + 36 + i * 24;
            boolean sel = i == tab;
            boolean hov = mx >= ox + 8 && mx < ox + 92 && my >= ty && my < ty + 20;
            if (sel || hov) Draw.round(ctx, ox + 8, ty, 84, 20, 4, Draw.alpha(sel ? Draw.alpha(acc, 0.28f) : 0x20FFFFFF, ease));
            if (sel) ctx.fill(ox + 8, ty + 4, ox + 10, ty + 16, acc);
            ctx.drawTextWithShadow(textRenderer, TABS[i], ox + 16, ty + 6, sel ? 0xFFFFFFFF : 0xFFB0B0BC);
        }

        List<Card> cs = cards();
        for (int i = 0; i < cs.size(); i++) {
            Card c = cs.get(i);
            int cx = cardX(i), cy = cardY(i);
            boolean hov = mx >= cx && mx < cx + 130 && my >= cy && my < cy + 22;
            Draw.round(ctx, cx, cy, 130, 22, 4, Draw.alpha(hov ? 0xFF2A2A38 : 0xFF1E1E2A, ease));
            ctx.drawTextWithShadow(textRenderer, c.label(), cx + 8, cy + 7, 0xFFFFFFFF);
            if (c.toggle()) {
                String k = tab + c.label();
                float tg = c.state().getAsBoolean() ? 1f : 0f;
                float a = anims.getOrDefault(k, tg);
                a += Math.signum(tg - a) * Math.min(Math.abs(tg - a), dt * 8f);
                anims.put(k, a);
                int tx = cx + 130 - 34;
                Draw.round(ctx, tx, cy + 6, 26, 10, 5, Draw.lerpColor(0xFF3A3A48, acc, a));
                Draw.round(ctx, tx + 2 + (int) (a * 16), cy + 8, 6, 6, 3, 0xFFFFFFFF);
            } else {
                ctx.drawTextWithShadow(textRenderer, ">", cx + 130 - 14, cy + 7, acc);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int ox = ox(), oy = oy();
            for (int i = 0; i < TABS.length; i++) {
                int ty = oy + 36 + i * 24;
                if (mx >= ox + 8 && mx < ox + 92 && my >= ty && my < ty + 20) { tab = i; return true; }
            }
            List<Card> cs = cards();
            for (int i = 0; i < cs.size(); i++) {
                int cx = cardX(i), cy = cardY(i);
                if (mx >= cx && mx < cx + 130 && my >= cy && my < cy + 22) { cs.get(i).click().run(); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
