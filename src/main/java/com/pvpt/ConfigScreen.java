package com.pvpt;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.literal("pvpt"));
        this.parent = parent;
    }

    private Text enabledText() {
        return Text.literal("Триггербот: " + (Config.enabled ? "ВКЛ" : "ВЫКЛ"));
    }

    private Text modeText() {
        String m = switch (Config.mode) {
            case ALL -> "Все удары";
            case CRITS -> "Только криты";
            case SWEEP -> "Только обычные / сплеш";
        };
        return Text.literal("Режим: " + m);
    }

    private Text distText() {
        return Text.literal("Дистанция: " + Config.distance + " бл.");
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 2 - 30;

        addDrawableChild(ButtonWidget.builder(enabledText(), b -> {
            Config.enabled = !Config.enabled;
            b.setMessage(enabledText());
        }).dimensions(x, y, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(modeText(), b -> {
            Config.Mode[] v = Config.Mode.values();
            Config.mode = v[(Config.mode.ordinal() + 1) % v.length];
            b.setMessage(modeText());
        }).dimensions(x, y + 26, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(distText(), b -> {
            Config.distance += 0.5;
            if (Config.distance > 3.0) Config.distance = 1.0;
            b.setMessage(distText());
        }).dimensions(x, y + 52, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(x, y + 86, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("pvpt"), width / 2, height / 2 - 55, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        Config.save();
        client.setScreen(parent);
    }
}
