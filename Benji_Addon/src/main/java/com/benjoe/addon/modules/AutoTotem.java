package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> healthThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("health-threshold")
        .description("Only equip totem when health is below this value. 0 = always equip.")
        .defaultValue(0).min(0).sliderMax(20)
        .build()
    );

    public AutoTotem() {
        super(BenjoeAddon.CATEGORY, "auto-totem",
            "Automatically moves a totem of undying into your offhand slot.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;

        // Check if offhand already has a totem
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;

        // Health threshold check
        if (healthThreshold.get() > 0 && mc.player.getHealth() > healthThreshold.get()) return;

        // Find a totem in inventory (slots 9-44)
        int totemSlot = -1;
        for (int i = 9; i < 45; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }
        if (totemSlot == -1) return;

        // Move totem to offhand (slot 45 in the inventory screen)
        mc.interactionManager.clickSlot(
            mc.player.playerScreenHandler.syncId,
            totemSlot, 0, SlotActionType.PICKUP, mc.player
        );
        mc.interactionManager.clickSlot(
            mc.player.playerScreenHandler.syncId,
            45, 0, SlotActionType.PICKUP, mc.player
        );
        // If leftover in cursor, put back
        if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                totemSlot, 0, SlotActionType.PICKUP, mc.player
            );
        }
    }
}
