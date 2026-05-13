package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CoordsLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> logInterval = sgGeneral.add(new IntSetting.Builder()
        .name("log-interval").description("Ticks between logs. 20 = 1 second.")
        .defaultValue(200).min(20).sliderMax(1200).build()
    );
    private final Setting<Boolean> logToChat = sgGeneral.add(new BoolSetting.Builder()
        .name("log-to-chat").defaultValue(true).build()
    );
    private final Setting<Boolean> logToFile = sgGeneral.add(new BoolSetting.Builder()
        .name("log-to-file").description("Saves to coords_log.txt in .minecraft folder.")
        .defaultValue(true).build()
    );
    private final Setting<Boolean> includeNether = sgGeneral.add(new BoolSetting.Builder()
        .name("nether-coords").defaultValue(true).build()
    );

    private int tickCounter = 0;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CoordsLogger() {
        super(BenjoeAddon.CATEGORY, "coords-logger", "Logs your coordinates to chat and/or a file.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (++tickCounter < logInterval.get()) return;
        tickCounter = 0;

        BlockPos pos = mc.player.getBlockPos();
        String dim = getDim();
        String entry = String.format("[%s] %s | X: %d  Y: %d  Z: %d",
            LocalDateTime.now().format(FMT), dim, pos.getX(), pos.getY(), pos.getZ());

        String extra = "";
        if (includeNether.get()) {
            boolean inNether = mc.world.getRegistryKey().equals(net.minecraft.world.World.NETHER);
            extra = inNether
                ? String.format("  §7(OW: %d, %d)", pos.getX() * 8, pos.getZ() * 8)
                : String.format("  §7(Nether: %d, %d)", pos.getX() / 8, pos.getZ() / 8);
        }

        if (logToChat.get()) ChatUtils.infoPrefix("Coords", entry + extra);
        if (logToFile.get()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(
                    mc.runDirectory.getAbsolutePath() + "/coords_log.txt", true))) {
                pw.println(entry);
            } catch (IOException e) { BenjoeAddon.LOG.error("Coords log error: {}", e.getMessage()); }
        }
    }

    private String getDim() {
        if (mc.world == null) return "?";
        if (mc.world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) return "Overworld";
        if (mc.world.getRegistryKey().equals(net.minecraft.world.World.NETHER))    return "Nether";
        if (mc.world.getRegistryKey().equals(net.minecraft.world.World.END))       return "The End";
        return "Unknown";
    }
}
