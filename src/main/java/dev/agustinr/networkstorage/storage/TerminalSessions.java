package dev.agustinr.networkstorage.storage;

import com.hypixel.hytale.server.core.universe.world.World;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Terminales abiertas por cada jugador, para refrescar a todos cuando la red cambia. */
public final class TerminalSessions {

    private static final Map<String, Set<TerminalPage>> OPEN = new ConcurrentHashMap<>();

    private TerminalSessions() {}

    private static String key(@Nonnull World world, @Nonnull Vector3i pos) {
        return world.getName() + "@" + pos.x + "," + pos.y + "," + pos.z;
    }

    public static void open(@Nonnull TerminalPage page) {
        OPEN.computeIfAbsent(key(page.getWorld(), page.getPos()), k -> ConcurrentHashMap.newKeySet()).add(page);
    }

    public static void close(@Nonnull TerminalPage page) {
        String key = key(page.getWorld(), page.getPos());
        OPEN.computeIfPresent(key, (k, pages) -> {
            pages.remove(page);
            return pages.isEmpty() ? null : pages;
        });
    }

    /** Refresca todas las ventanas abiertas de esa terminal. */
    public static void refreshAll(@Nonnull World world, @Nonnull Vector3i pos) {
        Set<TerminalPage> pages = OPEN.get(key(world, pos));
        if (pages != null) {
            for (TerminalPage page : List.copyOf(pages)) {
                page.refresh();
            }
        }
    }
}
