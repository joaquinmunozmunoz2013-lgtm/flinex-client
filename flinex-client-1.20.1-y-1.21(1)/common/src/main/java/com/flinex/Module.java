package com.flinex;

import net.minecraft.client.MinecraftClient;

public class Module {
    public final String name, category;
    public boolean enabled;
    public int x, y;

    public Module(String name, String category, boolean enabledByDefault) {
        this.name = name;
        this.category = category;
        this.enabled = enabledByDefault;
    }

    public void tick(MinecraftClient mc) {}
    public void onEnable(MinecraftClient mc) {}
    public void onDisable(MinecraftClient mc) {}

    public void toggle() {
        MinecraftClient mc = MinecraftClient.getInstance();
        enabled = !enabled;
        if (enabled) onEnable(mc); else onDisable(mc);
        ClientConfig.save();
    }
}
