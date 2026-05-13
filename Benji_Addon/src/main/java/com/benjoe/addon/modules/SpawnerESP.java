package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;

public class SpawnerESP extends Module {
    // We store the mob type string alongside pos so we can color per-type
    private final SettingGroup sgGeneral  = settings.getDefaultGroup();
    private final SettingGroup sgColors   = settings.createGroup("Colors");

    private final Setting<Integer> scanChunks = sgGeneral.add(new IntSetting.Builder()
        .name("scan-chunks")
        .description("Chunks to scan. Full Y column (-64 to 320) is always included.")
        .defaultValue(5).min(1).sliderMax(12)
        .build()
    );
    private final Setting<Boolean> skeletonOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("skeleton-only")
        .description("Only show skeleton spawners.")
        .defaultValue(false).build()
    );
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Both).build()
    );

    // Generic spawner colors
    private final Setting<SettingColor> genericLine = sgColors.add(new ColorSetting.Builder()
        .name("generic-line").defaultValue(new SettingColor(255, 160, 0, 255)).build()
    );
    private final Setting<SettingColor> genericFill = sgColors.add(new ColorSetting.Builder()
        .name("generic-fill").defaultValue(new SettingColor(255, 160, 0, 30)).build()
    );

    // Skeleton spawner colors
    private final Setting<SettingColor> skeletonLine = sgColors.add(new ColorSetting.Builder()
        .name("skeleton-line").defaultValue(new SettingColor(255, 255, 255, 255)).build()
    );
    private final Setting<SettingColor> skeletonFill = sgColors.add(new ColorSetting.Builder()
        .name("skeleton-fill").defaultValue(new SettingColor(255, 255, 255, 25)).build()
    );

    // Zombie spawner colors
    private final Setting<SettingColor> zombieLine = sgColors.add(new ColorSetting.Builder()
        .name("zombie-line").defaultValue(new SettingColor(80, 200, 80, 255)).build()
    );
    private final Setting<SettingColor> zombieFill = sgColors.add(new ColorSetting.Builder()
        .name("zombie-fill").defaultValue(new SettingColor(80, 200, 80, 25)).build()
    );

    // Spider spawner colors
    private final Setting<SettingColor> spiderLine = sgColors.add(new ColorSetting.Builder()
        .name("spider-line").defaultValue(new SettingColor(160, 0, 0, 255)).build()
    );
    private final Setting<SettingColor> spiderFill = sgColors.add(new ColorSetting.Builder()
        .name("spider-fill").defaultValue(new SettingColor(160, 0, 0, 25)).build()
    );

    // Blaze spawner colors (nether)
    private final Setting<SettingColor> blazeLine = sgColors.add(new ColorSetting.Builder()
        .name("blaze-line").defaultValue(new SettingColor(255, 100, 0, 255)).build()
    );
    private final Setting<SettingColor> blazeFill = sgColors.add(new ColorSetting.Builder()
        .name("blaze-fill").defaultValue(new SettingColor(255, 100, 0, 25)).build()
    );

    // Maps pos -> lowercase mob type string
    private final Map<BlockPos, String> spawners = new HashMap<>();
    private int ticker = 0;

    public SpawnerESP() {
        super(BenjoeAddon.CATEGORY, "spawner-esp",
            "Highlights mob spawners through walls from bedrock to sky. Per-mob-type colors.");
    }

    @Override public void onActivate()  { scan(); }
    @Override public void onDeactivate(){ spawners.clear(); }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++ticker < 80) return;
        ticker = 0;
        scan();
    }

    private void scan() {
        if (mc.world == null || mc.player == null) return;
        spawners.clear();
        ChunkPos center = mc.player.getChunkPos();
        int r = scanChunks.get();

        for (int cx = center.x - r; cx <= center.x + r; cx++) {
            for (int cz = center.z - r; cz <= center.z + r; cz++) {
                if (!mc.world.isChunkLoaded(cx, cz)) continue;
                for (BlockEntity be : mc.world.getChunk(cx, cz).getBlockEntities().values()) {
                    if (!(be instanceof MobSpawnerBlockEntity sp)) continue;
                    String id = sp.getLogic()
                        .getSpawnEntry()
                        .getEntityNbtForClient(mc.world)
                        .map(nbt -> nbt.getString("id").toLowerCase())
                        .orElse("unknown");
                    if (skeletonOnly.get() && !id.contains("skeleton")) continue;
                    spawners.put(be.getPos().toImmutable(), id);
                }
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        for (var entry : spawners.entrySet()) {
            BlockPos p = entry.getKey();
            String mob = entry.getValue();
            SettingColor line, fill;

            if (mob.contains("skeleton"))     { line = skeletonLine.get(); fill = skeletonFill.get(); }
            else if (mob.contains("zombie"))  { line = zombieLine.get();   fill = zombieFill.get();   }
            else if (mob.contains("spider"))  { line = spiderLine.get();   fill = spiderFill.get();   }
            else if (mob.contains("blaze"))   { line = blazeLine.get();    fill = blazeFill.get();    }
            else                              { line = genericLine.get();   fill = genericFill.get();  }

            event.renderer.box(
                p.getX(), p.getY(), p.getZ(),
                p.getX() + 1, p.getY() + 1, p.getZ() + 1,
                fill, line, shapeMode.get(), 0
            );
        }
    }
}
