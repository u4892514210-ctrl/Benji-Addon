package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;

public class BenjiESP extends Module {
    private final SettingGroup sgGeneral       = settings.getDefaultGroup();
    private final SettingGroup sgSurface       = settings.createGroup("Surface Player Colors");
    private final SettingGroup sgUnderground   = settings.createGroup("Underground Player Colors");
    private final SettingGroup sgPillar        = settings.createGroup("Pillar Settings");

    // --- General ---
    private final Setting<Integer> chunkRange = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-range")
        .description("Chunks around you to scan for players.")
        .defaultValue(8).min(1).sliderMax(20)
        .build()
    );
    private final Setting<Boolean> showSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("show-self").defaultValue(false)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Both)
        .build()
    );

    // --- Surface colors ---
    private final Setting<SettingColor> surfacePillarLine = sgSurface.add(new ColorSetting.Builder()
        .name("pillar-line").defaultValue(new SettingColor(255, 140, 0, 255)).build()
    );
    private final Setting<SettingColor> surfacePillarFill = sgSurface.add(new ColorSetting.Builder()
        .name("pillar-fill").defaultValue(new SettingColor(255, 140, 0, 20)).build()
    );
    private final Setting<SettingColor> surfaceBoxLine = sgSurface.add(new ColorSetting.Builder()
        .name("box-line").defaultValue(new SettingColor(255, 200, 0, 255)).build()
    );
    private final Setting<SettingColor> surfaceBoxFill = sgSurface.add(new ColorSetting.Builder()
        .name("box-fill").defaultValue(new SettingColor(255, 200, 0, 35)).build()
    );

    // --- Underground colors ---
    private final Setting<SettingColor> ugPillarLine = sgUnderground.add(new ColorSetting.Builder()
        .name("pillar-line").defaultValue(new SettingColor(180, 0, 255, 255)).build()
    );
    private final Setting<SettingColor> ugPillarFill = sgUnderground.add(new ColorSetting.Builder()
        .name("pillar-fill").defaultValue(new SettingColor(180, 0, 255, 20)).build()
    );
    private final Setting<SettingColor> ugBoxLine = sgUnderground.add(new ColorSetting.Builder()
        .name("box-line").defaultValue(new SettingColor(220, 0, 255, 255)).build()
    );
    private final Setting<SettingColor> ugBoxFill = sgUnderground.add(new ColorSetting.Builder()
        .name("box-fill").defaultValue(new SettingColor(220, 0, 255, 35)).build()
    );

    // --- Pillar settings ---
    private final Setting<Integer> pillarBottom = sgPillar.add(new IntSetting.Builder()
        .name("pillar-bottom")
        .description("Bottom Y of the pillar. -64 = bedrock.")
        .defaultValue(-64).sliderMin(-64).sliderMax(0)
        .build()
    );
    private final Setting<Integer> pillarTop = sgPillar.add(new IntSetting.Builder()
        .name("pillar-top")
        .description("Top Y of the pillar. 320 = build limit.")
        .defaultValue(320).sliderMin(64).sliderMax(320)
        .build()
    );
    private final Setting<Boolean> drawPlayerBox = sgPillar.add(new BoolSetting.Builder()
        .name("draw-player-box")
        .description("Draw a tight box around the player's actual body.")
        .defaultValue(true)
        .build()
    );

    public BenjiESP() {
        super(BenjoeAddon.CATEGORY, "benji-esp",
            "Full-height pillar ESP for all nearby players. Purple = underground, orange = surface.");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        ChunkPos center = mc.player.getChunkPos();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && !showSelf.get()) continue;
            if (player.isSpectator()) continue;

            ChunkPos pc = player.getChunkPos();
            if (Math.abs(pc.x - center.x) > chunkRange.get()) continue;
            if (Math.abs(pc.z - center.z) > chunkRange.get()) continue;

            boolean ug = player.getY() < 0;

            SettingColor pillarLine = ug ? ugPillarLine.get() : surfacePillarLine.get();
            SettingColor pillarFill = ug ? ugPillarFill.get() : surfacePillarFill.get();
            SettingColor boxLine    = ug ? ugBoxLine.get()    : surfaceBoxLine.get();
            SettingColor boxFill    = ug ? ugBoxFill.get()    : surfaceBoxFill.get();

            int px = player.getBlockPos().getX();
            int pz = player.getBlockPos().getZ();

            // Full-height pillar
            event.renderer.box(
                px, pillarBottom.get(), pz,
                px + 1, pillarTop.get(), pz + 1,
                pillarFill, pillarLine, shapeMode.get(), 0
            );

            // Tight player body box
            if (drawPlayerBox.get()) {
                event.renderer.box(
                    player.getX() - 0.35, player.getY(), player.getZ() - 0.35,
                    player.getX() + 0.35, player.getY() + player.getHeight(), player.getZ() + 0.35,
                    boxFill, boxLine, shapeMode.get(), 0
                );
            }
        }
    }
}
