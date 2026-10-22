package dev.agustinr.networkstorage.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.EnvironmentBreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.agustinr.networkstorage.network.NetworkBlocks;
import dev.agustinr.networkstorage.network.NetworkEvents;
import org.joml.Vector3i;

import javax.annotation.Nonnull;

/** Bloques rotos sin jugador (por ejemplo, explosiones). */
public class NetworkEnvironmentBreakSystem extends EntityEventSystem<EntityStore, EnvironmentBreakBlockEvent> {

    public NetworkEnvironmentBreakSystem() {
        super(EnvironmentBreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull EnvironmentBreakBlockEvent event) {
        if (!NetworkBlocks.isNetworkBlock(NetworkBlocks.baseId(event.getBlockType()))) {
            return;
        }
        World world = store.getExternalData().getWorld();
        Vector3i pos = new Vector3i(event.getTargetBlock());
        world.execute(() -> NetworkEvents.afterBreak(world, pos));
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
