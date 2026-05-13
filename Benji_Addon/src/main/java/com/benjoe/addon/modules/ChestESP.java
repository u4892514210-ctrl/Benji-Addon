package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;

public class ChestESP extends Module {
    // Chest type enum for color grouping
    public enum ChestType { CHEST, TRAPPED, ENDER, BARREL, SHULKER }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors  = settings.createGroup("Colors");

    private final Setting<Integer> scanChunks = sgGeneral.add(new IntSetting.Builder()
        .name("scan-chunks")
        .description("Chunks around you to scan. Scans full Y -64 to 320 automatically.")
        .defaultValue(4).min(1).sliderMax(10)
        .build()
    );
    private final Setting<Boolean> normalChest   = sgGeneral.add(new BoolSetting.Builder().name("normal-chests").defaultValue(true).build());
    private final Setting<Boolean> trappedChest  = sgGeneral.add(new BoolSetting.Builder().name("trapped-chests").defaultValue(true).build());
    private final Setting<Boolean> enderChest    = sgGeneral.add(new BoolSetting.Builder().name("ender-chests").defaultValue(true).build());
    private final Setting<Boolean> barrel        = sgGeneral.add(new BoolSetting.Builder().name("barrels").defaultValue(true).build());
    private final Setting<Boolean> shulker       = sgGeneral.add(new BoolSetting.Builder().name("shulker-boxes").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode   = sgGeneral.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());

    // Per-type colors
    private final Setting<SettingColor> chestLine    = sgColors.add(new ColorSetting.Builder().name("chest-line").defaultValue(new SettingColor(255, 160, 0, 255)).build());
    private final Setting<SettingColor> chestFill    = sgColors.add(new ColorSetting.Builder().name("chest-fill").defaultValue(new SettingColor(255, 160, 0, 35)).build());
    private final Setting<SettingColor> trappedLine  = sgColors.add(new ColorSetting.Builder().name("trapped-line").defaultValue(new SettingColor(255, 60, 60, 255)).build());
    private final Setting<SettingColor> trappedFill  = sgColors.add(new ColorSetting.Builder().name("trapped-fill").defaultValue(new SettingColor(255, 60, 60, 35)).build());
    private final Setting<SettingColor> enderLine    = sgColors.add(new ColorSetting.Builder().name("ender-line").defaultValue(new SettingColor(100, 0, 255, 255)).build());
    private final Setting<SettingColor> enderFill    = sgColors.add(new ColorSetting.Builder().name("ender-fill").defaultValue(new SettingColor(100, 0, 255, 35)).build());
    private final Setting<SettingColor> barrelLine   = sgColors.add(new ColorSetting.Builder().name("barrel-line").defaultValue(new SettingColor(140, 90, 30, 255)).build());
    private final Setting<SettingColor> barrelFill   = sgColors.add(new ColorSetting.Builder().name("barrel-fill").defaultValue(new SettingColor(140, 90, 30, 35)).build());
    private final Setting<SettingColor> shulkerLine  = sgColors.add(new ColorSetting.Builder().name("shulker-line").defaultValue(new SettingColor(180, 0, 200, 255)).build());
    private final Setting<SettingColor> shulkerFill  = sgColors.add(new ColorSetting.Builder().name("shulker-fill").defaultValue(new SettingColor(180, 0, 200, 35)).build());

    // pos -> type
    private final Map<BlockPos, ChestType> found = new HashMap<>();
    private int ticker = 0;

    public ChestESP() {
        super(BenjoeAddon.CATEGORY, "chest-esp",
            "Highlights chests, barrels, shulkers and ender chests from bedrock to sky.");
    }

    @Override public void onActivate()  { scan(); }
    @Override public void onDeactivate(){ found.clear(); }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++ticker < 60) return; // re-scan every 3 seconds
        ticker = 0;
        scan();
    }

    private void scan() {
        if (mc.world == null || mc.player == null) return;
        found.clear();
        ChunkPos center = mc.player.getChunkPos();
        int r = scanChunks.get();

        for (int cx = center.x - r; cx <= center.x + r; cx++) {
            for (int cz = center.z - r; cz <= center.z + r; cz++) {
                if (!mc.world.isChunkLoaded(cx, cz)) continue;
                // getBlockEntities() covers the FULL chunk column, -64 to 320
                for (BlockEntity be : mc.world.getChunk(cx, cz).getBlockEntities().values()) {
                    ChestType type = classify(be);
                    if (type != null) found.put(be.getPos().toImmutable(), type);
                }
            }
        }
    }

    private ChestType classify(BlockEntity be) {
        if (normalChest.get()  && be instanceof ChestBlockEntity c
                && !(be instanceof TrappedChestBlockEntity)) return ChestType.CHEST;
        if (trappedChest.get() && be instanceof TrappedChestBlockEntity) return ChestType.TRAPPED;
        if (enderChest.get()   && be instanceof EnderChestBlockEntity)   return ChestType.ENDER;
        if (barrel.get()       && be instanceof BarrelBlockEntity)        return ChestType.BARREL;
        if (shulker.get()      && be instanceof ShulkerBoxBlockEntity)    return ChestType.SHULKER;
        return null;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        for (var entry : found.entrySet()) {
            BlockPos p = entry.getKey();
            SettingColor line, fill;
            switch (entry.getValue()) {
                case TRAPPED -> { line = trappedLine.get(); fill = trappedFill.get(); }
                case ENDER   -> { line = enderLine.get();   fill = enderFill.get();   }
                case BARREL  -> { line = barrelLine.get();  fill = barrelFill.get();  }
                case SHULKER -> { line = shulkerLine.get(); fill = shulkerFill.get(); }
                default      -> { line = chestLine.get();   fill = chestFill.get();   }
            }
            event.renderer.box(p.getX(), p.getY(), p.getZ(),
                p.getX() + 1, p.getY() + 1, p.getZ() + 1,
                fill, line, shapeMode.get(), 0);
        }
    }
}
