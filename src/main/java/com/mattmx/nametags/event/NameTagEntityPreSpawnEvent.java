package com.mattmx.nametags.event;

import com.github.retrooper.packetevents.protocol.player.User;
import com.mattmx.nametags.entity.NameTagEntity;
import me.tofaa.entitylib.ve.ViewerRule;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called before attempting to spawn the Entity in the world.
 * <p>
 * <b>You should not call</b> {@link com.mattmx.nametags.entity.NameTagEntity#sendPassengerPacket(Player)} or {@link NameTagEntity#destroy()}!
 * </p>
 */
public class NameTagEntityPreSpawnEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final @NotNull NameTagEntity nameTag;

    public NameTagEntityPreSpawnEvent(@NotNull NameTagEntity nameTag) {
        this.nameTag = nameTag;
    }

    public @NotNull NameTagEntity getNameTag() {
        return nameTag;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return getHandlerList();
    }
}