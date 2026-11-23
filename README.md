<div>

<h1 align="center">🏷️ Display Name Tags</h1>

</div>

Replace your players' boring old name tags with customizable ones based on 
text displays! (Thanks to [EntityLib](https://github.com/Tofaa2/EntityLib)!)

## About this fork

This fork keeps NameTags running on Minecraft 26.2 (Paper/Purpur) for the OvergrowthSMP network.

All credit for the plugin goes to the original authors:

- **[mattmx](https://github.com/mattmx)**, who created Display Name Tags
- **[JosTheDude](https://github.com/JosTheDude)**, whose [DisplayNameTags-Folia](https://github.com/JosTheDude/DisplayNameTags-Folia) this fork is based on

Changes in this fork:

- **26.2 support:** a fallback entity ID provider for servers where EntityLib cannot find the server's entity ID counter.
- **Name tags after teleport, world change, respawn and rejoin:** tags are re-sent to nearby players shortly after these events, so names no longer go missing.
- **Relog fix:** a stale tag left from a previous session is replaced instead of reused, and leaving the server no longer throws "No cached NameTag" errors.
- **Less placeholder work:** when a group tag replaces the default text, the default text is no longer parsed on every refresh.
- **Simple Voice Chat status:** `%nametags_voice%` shows whether a player is speaking, idle, has voice chat disabled or is not connected. Icons are set in the `voice-chat` section of `config.yml`. It does not need PlaceholderAPI.

Licensed under GPL-3.0, the same as the original.


<p align="center">
    <img width="650px" src=nametags.gif />
</p>


## Configuration

Currently, you can customize default name tags and create grouped name tags.

Install the plugin and access the `plugins/NameTags/config.yml` for more information.

## API

Designed primarily for developers, the NameTags api gives you lightweight yet
powerful control over how the plugin operates.

You can override default behaviours using the `setDefaultProvider` method, and
the [NameTagEntityCreateEvent](./src/main/java/com/mattmx/nametags/event/NameTagEntityCreateEvent.java)
to hook into a tag's creation. You can add your own features using the 
[Trait](./src/main/java/com/mattmx/nametags/entity/trait/Trait.java) api.

```java

public void onEnable() {
    NameTags nameTags = NameTags.getInstance();
    
    // Override the default "base" settings of a tag.
    nameTags.getEntityManager()
        .setDefaultProvider((entity, meta) -> {
            meta.setText(Component.text(entity.getName()));
            /* ... */
        });
}

```

Here is an example where we can add an Item Display above the player's name tag
by using the `Trait` system.

```java

class MyCustomTrait extends Trait {
    // TODO create example by putting an ItemStack above a name tag.
    
    @Override
    public void onDisable() {
        // Clean up stuff
    }
}

class MyCustomListener implements Listener {
    
    @EventHandler
    public void onTagCreate(@NotNull NameTagEntityCreateEvent event) {
        if (!event.getBukkitEntity().getName().equals("MattMX")) return;
        
        event.getTraits().getOrAddTrait(MyCustomTrait.class, MyCustomTrait::new);
    }
    
}

```

<details>
    <summary>Kotlin example</summary>

Here is a brief example of Kotlin usage, and shows that you can use the nametags on entities other than just Players!

In this example, a dropped item will display a timer of 4 seconds before it is removed from the world, with a timer above it!

```kt
@EventHandler
fun onItemSpawn(event: ItemSpawnEvent) = event.apply {
    entity.isPersistent = false

    // Armour and tools should take longer to despawn
    val ticksTillRemove = 80 // 4 seconds

    val nameTagEntity = NameTags.getInstance()
        .entityManager
        .getOrCreateNameTagEntity(entity)

    nameTagEntity.modify { meta ->
        meta.isShadow = true
        meta.viewRange = 90f
        meta.backgroundColor = NameTags.TRANSPARENT
        meta.translation = Vector3f(0f, 0.45f, 0f)
        meta.billboardConstraints = AbstractDisplayMeta.BillboardConstraints.VERTICAL
        meta.textOpacity = (-180).toByte()
    }

    var counter = ticksTillRemove / 20L
    val update = runAsyncRepeat(20) {
        counter--
        nameTagEntity.modify { meta ->
            meta.text = Component.text(counter.toString()).color(NamedTextColor.RED)
        }
    }

    runSyncLater(ticksTillRemove) {
        update?.cancel()

        NameTags.getInstance()
            .entityManager
            .removeEntity(entity)
            ?.destroy()

        if (entity.isValid) {
            entity.remove()
        }
    }
}
```
    
</details>

## Roadmap

- `/feat/customization`
    Extension plugin to give players ability to customize their own
    name tags by using a command and customizable GUI interface.
