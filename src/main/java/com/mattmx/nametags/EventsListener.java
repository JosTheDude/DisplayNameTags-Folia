package com.mattmx.nametags;

import com.mattmx.nametags.entity.NameTagEntity;
import com.mattmx.nametags.entity.trait.SneakTrait;
import com.mattmx.nametags.hook.VoiceChatHook;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.UUID;

public class EventsListener implements Listener {

    private final @NotNull NameTags plugin;

    public EventsListener(@NotNull NameTags plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        FoliaScheduler.getEntityScheduler().run(event.getPlayer(), plugin, (task) -> {
            plugin.getEntityManager()
                .getOrCreateNameTagEntity(event.getPlayer())
                .updateVisibility();
        }, () -> {});
        scheduleResync(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
        scheduleResync(event.getPlayer());
    }

    private void scheduleResync(@NotNull Player player) {
        for (long delay : new long[]{10L, 40L}) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> resync(player), delay);
        }
    }

    private void resync(@NotNull Player player) {
        if (!player.isOnline()) return;
        NameTagEntity own = plugin.getEntityManager().getNameTagEntity(player);
        if (own != null) {
            for (Player viewer : player.getTrackedBy()) reattach(own, viewer);
        }
        for (Player other : player.getWorld().getPlayers()) {
            if (other == player || !other.getTrackedBy().contains(player)) continue;
            NameTagEntity tag = plugin.getEntityManager().getNameTagEntity(other);
            if (tag != null) reattach(tag, player);
        }
    }

    private void reattach(@NotNull NameTagEntity tag, @NotNull Player viewer) {
        if (!viewer.isOnline() || !tag.canBeSeenBy(viewer) || PacketEvents.getAPI().getPlayerManager().getUser(viewer) == null) return;
        tag.updateLocation();
        tag.getPassenger().removeViewer(viewer.getUniqueId());
        tag.getPassenger().addViewer(viewer.getUniqueId());
        tag.sendPassengerPacket(viewer);
    }

//    @EventHandler
//    public void onEntityRemove(@NotNull EntityRemoveFromWorldEvent event) {
//        plugin.getEntityManager().removeLastSentPassengersCache(event.getEntity().getEntityId());
//
//        NameTagEntity entity = plugin.getEntityManager()
//            .removeEntity(event.getEntity());
//
//        if (entity != null) {
//            entity.destroy();
//        }
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        plugin.getEntityManager().removeLastSentPassengersCache(event.getPlayer().getEntityId());
        VoiceChatHook.forget(event.getPlayer().getUniqueId());
        // TODO(matt): might not be sending de-spawn packet to viewers all the time?

        // Remove as a viewer from all entities
        for (final NameTagEntity entity : plugin.getEntityManager().getAllEntities()) {
            entity.getPassenger().removeViewer(event.getPlayer().getUniqueId());
        }

        NameTagEntity entity = plugin.getEntityManager().removeEntity(event.getPlayer());

        if (entity != null) {
            entity.destroy();
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(@NotNull PlayerChangedWorldEvent event) {
        NameTagEntity nameTagEntity = plugin.getEntityManager().getNameTagEntity(event.getPlayer());

        scheduleResync(event.getPlayer());

        if (nameTagEntity == null) return;

        nameTagEntity.updateLocation();

        if (plugin.getConfig().getBoolean("show-self", false)) {
            nameTagEntity.getPassenger().removeViewer(nameTagEntity.getBukkitEntity().getUniqueId());
            nameTagEntity.getPassenger().addViewer(nameTagEntity.getBukkitEntity().getUniqueId());
            nameTagEntity.sendPassengerPacket(event.getPlayer());
        }
    }


    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        NameTagEntity nameTagEntity = plugin.getEntityManager()
            .getNameTagEntity(event.getPlayer());

        if (nameTagEntity == null) return;

        if (plugin.getConfig().getBoolean("show-self", false)) {
            // Hides/removes tag on death/respawn screen
            nameTagEntity.getPassenger().removeViewer(nameTagEntity.getBukkitEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        scheduleResync(event.getPlayer());
        NameTagEntity nameTagEntity = plugin.getEntityManager()
            .getNameTagEntity(event.getPlayer());

        if (nameTagEntity == null) return;

        if (plugin.getConfig().getBoolean("show-self", false)) {

            String respawnWorld = event.getRespawnLocation().getWorld().getName();
            String playerWorld = event.getPlayer().getWorld().getName();
            // Ignoring since same action is handled at EventListener#onPlayerChangeWorld if player was killed in another world.
            if (!playerWorld.equalsIgnoreCase(respawnWorld)) return;

            FoliaScheduler.getEntityScheduler().run(event.getPlayer(), plugin, (task) -> {
                // Update entity location.
                nameTagEntity.updateLocation();
                // Add player back as viewer
                nameTagEntity.getPassenger().addViewer(nameTagEntity.getBukkitEntity().getUniqueId());
                // Send passenger packet
                nameTagEntity.sendPassengerPacket(event.getPlayer());
            }, () -> {});
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSneak(@NotNull PlayerToggleSneakEvent event) {
        if (!plugin.getConfig().getBoolean("sneak.enabled")) {
            return;
        }

        if (event.getPlayer().isInsideVehicle()) return;

        NameTagEntity nameTagEntity = plugin.getEntityManager()
            .getNameTagEntity(event.getPlayer());

        if (nameTagEntity == null) return;

        nameTagEntity.getTraits()
            .getOrAddTrait(SneakTrait.class, SneakTrait::new)
            .updateSneak(event.isSneaking());
    }
}
