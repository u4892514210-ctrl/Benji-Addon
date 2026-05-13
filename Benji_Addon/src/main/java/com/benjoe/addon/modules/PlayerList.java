package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Comparator;
import java.util.List;

public class PlayerList extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> x = sgGeneral.add(new DoubleSetting.Builder()
        .name("x").description("X position on screen.").defaultValue(10).sliderMax(1920).build()
    );
    private final Setting<Double> y = sgGeneral.add(new DoubleSetting.Builder()
        .name("y").description("Y position on screen.").defaultValue(100).sliderMax(1080).build()
    );
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale").defaultValue(1.0).min(0.5).sliderMax(3.0).build()
    );
    private final Setting<Integer> maxPlayers = sgGeneral.add(new IntSetting.Builder()
        .name("max-players").description("Max players to list on screen.").defaultValue(10).min(1).sliderMax(30).build()
    );
    private final Setting<Boolean> sortByDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("sort-by-distance").defaultValue(true).build()
    );
    private final Setting<Boolean> showHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("show-health").defaultValue(true).build()
    );

    public PlayerList() {
        super(BenjoeAddon.CATEGORY, "player-list",
            "Shows a HUD list of all nearby players with distance and health.");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        List<PlayerEntity> players = mc.world.getPlayers().stream()
            .filter(p -> p != mc.player && !p.isSpectator())
            .sorted(sortByDistance.get()
                ? Comparator.comparingDouble(p -> mc.player.distanceTo(p))
                : Comparator.comparing(p -> p.getGameProfile().getName()))
            .limit(maxPlayers.get())
            .toList();

        if (players.isEmpty()) return;

        TextRenderer tr = TextRenderer.get();
        tr.begin(scale.get(), false, true);

        double drawY = y.get();
        tr.render("§7── Nearby Players ──", x.get(), drawY, Color.WHITE, true);
        drawY += 11;

        for (PlayerEntity p : players) {
            double dist = mc.player.distanceTo(p);
            String name = p.getGameProfile().getName();

            // Color name based on distance: green < 50, yellow < 150, red otherwise
            String nameColor = dist < 50 ? "§a" : dist < 150 ? "§e" : "§c";
            String line;

            if (showHealth.get()) {
                int hp = (int) p.getHealth();
                String hpColor = hp > 15 ? "§a" : hp > 8 ? "§e" : "§c";
                line = nameColor + name + " §7" + (int)dist + "m  " + hpColor + "❤" + hp;
            } else {
                line = nameColor + name + " §7" + (int)dist + "m";
            }

            tr.render(line, x.get(), drawY, Color.WHITE, true);
            drawY += 11;
        }

        tr.end();
    }
}
