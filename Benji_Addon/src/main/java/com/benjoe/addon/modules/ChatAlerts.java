package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

public class ChatAlerts extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> keywords = sgGeneral.add(new StringListSetting.Builder()
        .name("keywords")
        .description("Words that trigger an alert when seen in chat.")
        .defaultValue(List.of("your_username", "coords", "found base", "stash"))
        .build()
    );
    private final Setting<Boolean> alertOnJoin = sgGeneral.add(new BoolSetting.Builder()
        .name("alert-on-join").defaultValue(true).build()
    );
    private final Setting<Boolean> highlight = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-message").defaultValue(true).build()
    );

    public ChatAlerts() {
        super(BenjoeAddon.CATEGORY, "chat-alerts",
            "Plays a pling and highlights chat when keywords are mentioned.");
    }

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        if (mc.player == null) return;
        String raw = event.getMessage().getString().toLowerCase();

        if (alertOnJoin.get() && raw.contains("joined the game")) {
            pling();
            if (highlight.get())
                ChatUtils.infoPrefix("Alert", "§eJoined: §f" + event.getMessage().getString());
            return;
        }

        for (String kw : keywords.get()) {
            if (raw.contains(kw.toLowerCase())) {
                pling();
                if (highlight.get())
                    ChatUtils.infoPrefix("Alert", "§c[" + kw + "] §f" + event.getMessage().getString());
                break;
            }
        }
    }

    private void pling() {
        if (mc.player != null)
            mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 1f);
    }
}
