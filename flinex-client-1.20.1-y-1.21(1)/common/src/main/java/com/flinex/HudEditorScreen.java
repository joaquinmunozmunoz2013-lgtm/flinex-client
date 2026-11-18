package com.flinex;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class HudEditorScreen extends BaseScreen {
    private HudModule drag;
    private int dx, dy;

    public HudEditorScreen() { super(Text.literal("Editor HUD")); }

    @Override protected void init() { HudModule.preview = true; }

    @Override public void removed() { HudModule.preview = false; ClientConfig.save(); }

    @Override
    protected void draw(DrawContext ctx, int mx, int my, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int acc = Draw.accent();
        for (Module m : Modules.ALL) {
            if (!(m instanceof HudModule h) || !h.enabled) continue;
            h.render(ctx, mc, acc);
            int bw = Math.max(h.w, 8), bh = Math.max(h.h, 8);
            int col = h == drag ? 0xFFFFFFFF : Draw.alpha(acc, 0.8f);
            ctx.fill(h.x - 1, h.y - 1, h.x + bw + 1, h.y, col);
            ctx.fill(h.x - 1, h.y + bh, h.x + bw + 1, h.y + bh + 1, col);
            ctx.fill(h.x - 1, h.y, h.x, h.y + bh, col);
            ctx.fill(h.x + bw, h.y, h.x + bw + 1, h.y + bh, col);
        }
        ctx.drawCenteredTextWithShadow(textRenderer, "Arrastra los módulos. Esc para guardar y salir.",
                width / 2, 10, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
                if (!(Modules.ALL.get(i) instanceof HudModule h) || !h.enabled) continue;
                if (mx >= h.x && mx < h.x + Math.max(h.w, 8) && my >= h.y && my < h.y + Math.max(h.h, 8)) {
                    drag = h; dx = (int) mx - h.x; dy = (int) my - h.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (drag != null) {
            drag.x = MathHelper.clamp((int) mx - dx, 0, Math.max(0, width - drag.w));
            drag.y = MathHelper.clamp((int) my - dy, 0, Math.max(0, height - drag.h));
            return true;
        }
        return super.mouseDragged(mx, my, button, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drag != null) { drag = null; ClientConfig.save(); return true; }
        return super.mouseReleased(mx, my, button);
    }
}
