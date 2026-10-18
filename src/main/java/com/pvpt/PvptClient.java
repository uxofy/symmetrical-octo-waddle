package com.pvpt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public class PvptClient implements ClientModInitializer {
    private static KeyBinding menuKey;
    private static KeyBinding toggleKey;
    private static int sinceAttack = 100; // тиков с последнего удара мода

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("pvpt", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.pvpt.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.pvpt.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, cat));

        ClientTickEvents.END_CLIENT_TICK.register(PvptClient::tick);
    }

    private static void tick(MinecraftClient mc) {
        sinceAttack++;
        while (toggleKey.wasPressed()) {
            Config.enabled = !Config.enabled;
            Config.save();
        }
        while (menuKey.wasPressed()) {
            mc.setScreen(new ConfigScreen(mc.currentScreen));
        }

        if (!Config.enabled) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null || mc.interactionManager == null) return;

        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        Entity target = hit.getEntity();
        if (!(target instanceof PlayerEntity pl) || !pl.isAlive() || !pl.isAttackable() || pl.isSpectator()) return;

        if (hit.getPos().distanceTo(p.getEyePos()) > Config.distance) return;

        ItemStack stack = p.getMainHandStack();
        boolean sword = stack.isIn(ItemTags.SWORDS);
        boolean axe = stack.isIn(ItemTags.AXES);
        if (!sword && !axe) return;

        // свой кулдаун: топор 1.0 с (20 тиков), меч 0.625 с (13 тиков)
        int need = axe ? 20 : 13;
        if (sinceAttack < need) return;

        // условия крита без учёта спринта
        boolean critBase = p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing()
                && !p.isTouchingWater() && !p.hasStatusEffect(StatusEffects.BLINDNESS)
                && !p.hasVehicle();
        boolean crit = critBase && !p.isSprinting();
        boolean normal = !crit && p.isOnGround(); // обычный удар (спринт разрешён)

        boolean ok = switch (Config.mode) {
            case CRITS -> critBase;   // бьёт и на бегу
            case SWEEP -> normal;
            case ALL -> true;
        };
        if (!ok) return;

        // Крит на бегу: сервер не даёт крит в спринте, поэтому перед ударом
        // отправляем ему "остановил спринт" (в ваниле спринт после удара и так сбрасывается).
        var net = mc.getNetworkHandler();
        if (Config.mode == Config.Mode.CRITS && p.isSprinting() && net != null) {
            net.sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }

        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        sinceAttack = 0;
    }
}
