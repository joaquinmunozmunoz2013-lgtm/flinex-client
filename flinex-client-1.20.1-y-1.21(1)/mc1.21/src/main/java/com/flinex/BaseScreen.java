package com.flinex;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** 1.21: Screen.render ya dibuja el fondo (no llamarlo dos veces). */
public abstract class BaseScreen extends Screen {
    protected BaseScreen(Text title) { super(title); }

    protected abstract void draw(DrawContext ctx, int mx, int my, float delta);

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        draw(ctx, mx, my, delta);
    }
}
