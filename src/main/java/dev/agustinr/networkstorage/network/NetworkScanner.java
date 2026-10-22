package dev.agustinr.networkstorage.network;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.SectionReader;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recorre la red por adyacencia (6 vecinos) a partir de un bloque.
 * Tiene que llamarse desde el hilo del mundo.
 */
public final class NetworkScanner {

    /** Tope de bloques por recorrido, para no colgar el servidor con redes gigantes. */
    public static final int MAX_BLOCKS = 4096;

    private static final int[][] NEIGHBORS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private final SectionReader reader;

    public NetworkScanner(@Nonnull World world) {
        this.reader = new SectionReader(world.getChunkStore());
    }

    /** Id del bloque en esa posición, o null si es aire o el chunk no está cargado. */
    @Nullable
    public String blockIdAt(int x, int y, int z) {
        if (!reader.canHoldY(y)) {
            return null;
        }
        int blockId = reader.getBlock(x, y, z);
        if (blockId == BlockType.EMPTY_ID) {
            return null;
        }
        return NetworkBlocks.baseId(BlockType.getAssetMap().getAsset(blockId));
    }

    public static List<Vector3i> neighbors(Vector3i pos) {
        List<Vector3i> result = new ArrayList<>(NEIGHBORS.length);
        for (int[] d : NEIGHBORS) {
            result.add(new Vector3i(pos.x + d[0], pos.y + d[1], pos.z + d[2]));
        }
        return result;
    }

    /**
     * Recorre la red que contiene a start. Si start no es un bloque del mod, devuelve null.
     *
     * @param ignored posición que se trata como vacía (por ejemplo, un bloque que se está rompiendo)
     */
    @Nullable
    public StorageNetwork scan(@Nonnull Vector3i start, @Nullable Vector3i ignored) {
        if (start.equals(ignored) || !NetworkBlocks.isNetworkBlock(blockIdAt(start.x, start.y, start.z))) {
            return null;
        }
        List<Vector3i> terminals = new ArrayList<>();
        List<Vector3i> units = new ArrayList<>();
        List<Vector3i> pipes = new ArrayList<>();
        Set<Vector3i> visited = new HashSet<>();
        ArrayDeque<Vector3i> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        boolean truncated = false;

        while (!queue.isEmpty()) {
            Vector3i pos = queue.poll();
            String id = blockIdAt(pos.x, pos.y, pos.z);
            if (NetworkBlocks.isTerminal(id)) {
                terminals.add(pos);
            } else if (NetworkBlocks.isUnit(id)) {
                units.add(pos);
            } else if (NetworkBlocks.isPipe(id)) {
                pipes.add(pos);
            }

            for (Vector3i next : neighbors(pos)) {
                if (next.equals(ignored) || visited.contains(next)) {
                    continue;
                }
                if (!NetworkBlocks.isNetworkBlock(blockIdAt(next.x, next.y, next.z))) {
                    continue;
                }
                if (visited.size() >= MAX_BLOCKS) {
                    truncated = true;
                    continue;
                }
                visited.add(next);
                queue.add(next);
            }
        }
        return new StorageNetwork(terminals, units, pipes, truncated);
    }

    /** Redes distintas que tocan alguna de las posiciones dadas (sin repetir). */
    public List<StorageNetwork> scanAll(@Nonnull List<Vector3i> starts, @Nullable Vector3i ignored) {
        List<StorageNetwork> networks = new ArrayList<>();
        for (Vector3i start : starts) {
            boolean alreadyFound = networks.stream().anyMatch(n -> n.contains(start));
            if (alreadyFound) {
                continue;
            }
            StorageNetwork network = scan(start, ignored);
            if (network != null) {
                networks.add(network);
            }
        }
        return networks;
    }
}
