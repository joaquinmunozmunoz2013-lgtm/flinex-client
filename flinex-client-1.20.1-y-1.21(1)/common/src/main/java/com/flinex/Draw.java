package com.flinex;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public final class Draw {
    private Draw() {}

    public static int accent() {
        if (ClientConfig.rainbow)
            return java.awt.Color.HSBtoRGB(((System.currentTimeMillis() / 15) % 360) / 360f, 0.55f, 1f) | 0xFF000000;
        return ClientConfig.accent;
    }

    /** Rectángulo con esquinas redondeadas (radio pequeño). */
    public static void round(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double d = r - i - 0.5;
            int in = (int) Math.round(r - Math.sqrt(r * r - d * d));
            c.fill(x + in, y + i, x + w - in, y + i + 1, col);
            c.fill(x + in, y + h - i - 1, x + w - in, y + h - i, col);
        }
        c.fill(x, y + r, x + w, y + h - r, col);
    }

    public static int alpha(int col, float a) {
        int al = (int) (((col >>> 24) & 0xFF) * MathHelper.clamp(a, 0f, 1f));
        return (al << 24) | (col & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
