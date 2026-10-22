package dev.agustinr.networkstorage.network;

import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.hypixel.hytale.server.core.universe.world.accessor.SectionReader;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Bloques que ocupan varias celdas (la terminal mide 2 de ancho). El juego pone en las celdas extra
 * bloques "relleno" (filler) con el mismo id que el original y el desplazamiento hasta él.
 */
public final class BlockCells {

    private BlockCells() {}

    /** Celda de origen del bloque que ocupa pos (pos misma si no es un relleno). */
    @Nonnull
    public static Vector3i origin(@Nonnull SectionReader reader, @Nonnull Vector3i pos) {
        int filler = reader.getFiller(pos.x, pos.y, pos.z);
        if (filler == 0) {
            return new Vector3i(pos);
        }
        return new Vector3i(pos).sub(FillerBlockUtil.unpackX(filler), FillerBlockUtil.unpackY(filler),
                FillerBlockUtil.unpackZ(filler));
    }

    /** Todas las celdas que ocupa un bloque con ese id y rotación puesto en origin. */
    @Nonnull
    public static List<Vector3i> cells(int blockId, int rotation, @Nonnull Vector3i origin) {
        BlockBoundingBoxes.RotatedVariantBoxes footprint = FillerBlockUtil.multiCellFootprint(blockId, rotation);
        if (footprint == null) {
            return List.of(new Vector3i(origin));
        }
        List<Vector3i> cells = new ArrayList<>();
        FillerBlockUtil.forEachFillerBlock(footprint,
                (dx, dy, dz) -> cells.add(new Vector3i(origin.x + dx, origin.y + dy, origin.z + dz)));
        return cells;
    }

    /** Todas las celdas del bloque que ocupa pos, leyendo el mundo. */
    @Nonnull
    public static List<Vector3i> cellsAt(@Nonnull SectionReader reader, @Nonnull Vector3i pos) {
        Vector3i origin = origin(reader, pos);
        int blockId = reader.getBlock(origin.x, origin.y, origin.z);
        return cells(blockId, reader.getRotationIndex(origin.x, origin.y, origin.z), origin);
    }
}
