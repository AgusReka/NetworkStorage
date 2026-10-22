package dev.agustinr.networkstorage.network;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.agustinr.networkstorage.storage.TerminalSessions;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** Lógica común a poner y romper bloques: recalcula las redes afectadas y lo deja en la consola. */
public final class NetworkEvents {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private NetworkEvents() {}

    /** Cuántas terminales y units tendría la red que se forma al poner un bloque. */
    public record PlacementTotals(int terminals, int units) {}

    /**
     * Totales de la red que quedaría si se pone un bloque (puede unir varias redes).
     * Se llama antes de que el bloque exista, así que se miran los vecinos de todas sus celdas.
     *
     * @param cells celdas que va a ocupar el bloque (dos para la terminal)
     */
    public static PlacementTotals totalsAfterPlacing(@Nonnull World world, @Nonnull List<Vector3i> cells, @Nonnull String placedId) {
        NetworkScanner scanner = new NetworkScanner(world);
        int terminals = NetworkBlocks.isTerminal(placedId) ? 1 : 0;
        int units = NetworkBlocks.isUnit(placedId) ? 1 : 0;
        for (StorageNetwork network : scanner.scanAll(NetworkScanner.neighbors(cells), cells)) {
            terminals += network.terminals().size();
            units += network.units().size();
        }
        return new PlacementTotals(terminals, units);
    }

    /** Recalcula la red después de poner un bloque. */
    public static void afterPlace(@Nonnull World world, @Nonnull Vector3i pos) {
        StorageNetwork network = new NetworkScanner(world).scan(pos);
        if (network == null) {
            LOGGER.atWarning().log("[NetworkStorage] Se puso un bloque en " + StorageNetwork.format(pos) + " pero no se encontró al recalcular");
            return;
        }
        report(world, "Bloque puesto en " + StorageNetwork.format(pos), List.of(network));
    }

    /**
     * Recalcula las redes vecinas después de romper un bloque (puede haber partido la red en varias).
     *
     * @param cells celdas que ocupaba el bloque, tomadas antes de romperlo
     */
    public static void afterBreak(@Nonnull World world, @Nonnull List<Vector3i> cells) {
        List<StorageNetwork> networks = new NetworkScanner(world).scanAll(NetworkScanner.neighbors(cells), cells);
        report(world, "Bloque roto en " + StorageNetwork.format(cells.getFirst()), networks);
        // Si lo que se rompió era una terminal, sus ventanas abiertas muestran que ya no existe.
        for (Vector3i cell : cells) {
            TerminalSessions.refreshAll(world, cell);
        }
    }

    private static void report(@Nonnull World world, String header, List<StorageNetwork> networks) {
        List<String> lines = new ArrayList<>();
        if (networks.isEmpty()) {
            lines.add("no quedó ninguna red conectada");
        } else {
            for (int i = 0; i < networks.size(); i++) {
                lines.add("red " + (i + 1) + ": " + networks.get(i).describe());
            }
        }
        LOGGER.atInfo().log("[NetworkStorage] [" + world.getName() + "] " + header + " -> " + String.join(" | ", lines));
        // La capacidad cambió: las terminales abiertas de estas redes tienen que mostrarla actualizada.
        for (StorageNetwork network : networks) {
            for (Vector3i terminal : network.terminals()) {
                TerminalSessions.refreshAll(world, terminal);
            }
        }
    }
}
