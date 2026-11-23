package com.mattmx.nametags.hook;

import com.mattmx.nametags.NameTags;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceChatHook {
    public static final String PLACEHOLDER = "%nametags_voice%";
    private static volatile Icons icons = new Icons(600, "", "", "", "");
    private static volatile @Nullable Tracker tracker;

    public static void inject(@NotNull NameTags plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("voicechat")) return;

        tracker = Tracker.register(plugin);
    }

    public static void load(@Nullable ConfigurationSection section) {
        if (section == null) {
            icons = new Icons(600, "", "", "", "");
            return;
        }

        icons = new Icons(
            Math.max(50L, section.getLong("speaking-window-ms", 600L)),
            section.getString("icons.speaking", ""),
            section.getString("icons.idle", ""),
            section.getString("icons.disabled", ""),
            section.getString("icons.disconnected", "")
        );
    }

    public static @NotNull String apply(@NotNull UUID player, @NotNull String line) {
        if (!line.contains(PLACEHOLDER)) return line;

        Icons current = icons;
        Tracker active = tracker;
        return line.replace(PLACEHOLDER, active == null ? current.disconnected() : active.icon(player, current));
    }

    public static void forget(@NotNull UUID player) {
        Tracker active = tracker;
        if (active != null) active.forget(player);
    }

    private record Icons(long window, String speaking, String idle, String disabled, String disconnected) {
    }

    private static final class Tracker implements VoicechatPlugin {
        private final Map<UUID, Long> lastPacket = new ConcurrentHashMap<>();
        private volatile @Nullable VoicechatServerApi api;

        private static @Nullable Tracker register(NameTags plugin) {
            BukkitVoicechatService service = Bukkit.getServicesManager().load(BukkitVoicechatService.class);
            if (service == null) {
                plugin.getLogger().warning("Simple Voice Chat is enabled, but its API service is missing.");
                return null;
            }

            Tracker created = new Tracker();
            service.registerPlugin(created);
            plugin.getLogger().info("Registering Simple Voice Chat status for " + PLACEHOLDER + ".");
            return created;
        }

        @Override
        public String getPluginId() {
            return "nametags";
        }

        @Override
        public void registerEvents(EventRegistration registration) {
            registration.registerEvent(VoicechatServerStartedEvent.class, event -> api = event.getVoicechat());
            registration.registerEvent(MicrophonePacketEvent.class, event -> {
                VoicechatConnection sender = event.getSenderConnection();
                if (sender != null) lastPacket.put(sender.getPlayer().getUuid(), System.currentTimeMillis());
            });
        }

        private void forget(UUID player) {
            lastPacket.remove(player);
        }

        private String icon(UUID player, Icons icons) {
            VoicechatServerApi current = api;
            VoicechatConnection connection = current == null ? null : current.getConnectionOf(player);
            if (connection == null || !connection.isInstalled() || !connection.isConnected()) return icons.disconnected();
            if (connection.isDisabled()) return icons.disabled();

            Long last = lastPacket.get(player);
            return last != null && System.currentTimeMillis() - last <= icons.window() ? icons.speaking() : icons.idle();
        }
    }
}
