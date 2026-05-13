package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AntiHunger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-sprint")
        .description("Still allow sprinting even with anti-hunger active.")
        .defaultValue(true)
        .build()
    );

    public AntiHunger() {
        super(BenjoeAddon.CATEGORY, "anti-hunger",
            "Reduces hunger drain by preventing unnecessary food level updates.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        // Prevent the client from sending sprint packets that drain hunger faster
        if (!sprint.get()) {
            mc.player.setSprinting(false);
        }
    }
}
