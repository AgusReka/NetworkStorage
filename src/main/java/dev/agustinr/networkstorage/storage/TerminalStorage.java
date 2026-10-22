package dev.agustinr.networkstorage.storage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.SectionReader;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.agustinr.networkstorage.network.BlockCells;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Acceso al inventario real guardado en el bloque de la terminal. */
public final class TerminalStorage {

    private TerminalStorage() {}

    /**
     * Inventario de la terminal que ocupa pos (cualquiera de sus dos celdas), o null si ahí no hay una
     * terminal con inventario. El inventario vive en la celda de origen.
     */
    @Nullable
    public static SimpleItemContainer get(@Nonnull World world, @Nonnull Vector3i pos) {
        Vector3i origin = BlockCells.origin(new SectionReader(world.getChunkStore()), pos);
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, origin.x, origin.y, origin.z);
        if (blockRef == null) {
            return null;
        }
        ItemContainerBlock container = world.getChunkStore().getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());
        return container != null ? container.getItemContainer() : null;
    }

    public static boolean isEmpty(@Nonnull World world, @Nonnull Vector3i pos) {
        SimpleItemContainer storage = get(world, pos);
        return storage == null || storage.isEmpty();
    }
}
