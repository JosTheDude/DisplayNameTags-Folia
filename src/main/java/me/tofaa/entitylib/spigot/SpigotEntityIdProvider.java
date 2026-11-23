package me.tofaa.entitylib.spigot;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.tofaa.entitylib.EntityIdProvider;
import me.tofaa.entitylib.Platform;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class SpigotEntityIdProvider implements EntityIdProvider {

    private static final AtomicInteger FALLBACK = new AtomicInteger(Integer.MAX_VALUE);

    private final Supplier<Integer> entityIdSupplier;

    public SpigotEntityIdProvider(Platform<JavaPlugin> platform) {
        this.entityIdSupplier = detect(platform);
    }

    @Override
    public int provide(UUID entityId, EntityType entityType) {
        return entityIdSupplier.get();
    }

    private static Supplier<Integer> detect(Platform<JavaPlugin> platform) {
        try {
            Object unsafe = Bukkit.getUnsafe();
            Method method = unsafe.getClass().getMethod("nextEntityId");
            method.invoke(unsafe);
            return () -> {
                try {
                    return (Integer) method.invoke(unsafe);
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Could not allocate an entity id", error);
                }
            };
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        try {
            Class<?> entity = Class.forName("net.minecraft.world.entity.Entity");
            for (Field field : entity.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == AtomicInteger.class) {
                    field.setAccessible(true);
                    AtomicInteger counter = (AtomicInteger) field.get(null);
                    if (counter != null) return counter::incrementAndGet;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        platform.getLogger().warning("No server entity id counter found; using a private descending id range for name tags.");
        return FALLBACK::getAndDecrement;
    }
}
