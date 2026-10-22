package dev.agustinr.networkstorage.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.EnvironmentBreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.agustinr.networkstorage.network.NetworkBlocks;
import dev.agustinr.networkstorage.network.NetworkEvents;
import org.joml.Vector3i;

import javax.annotation.Nonnull;

/**
 * Bloques rotos sin jugador (por ejemplo, explosiones o fuego). El juego dispara este evento a nivel
 * de mundo, no de entidad, así que tiene que ser un WorldEventSystem.
 */
public class NetworkEnvironmentBreakSystem extends WorldEventSystem<EntityStore, EnvironmentBreakBlockEvent> {

    public NetworkEnvironmentBreakSystem() {
        super(EnvironmentBreakBlockEvent.class);
    }

    @Override
    public void handle(@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull EnvironmentBreakBlockEvent event) {
        if (!NetworkBlocks.isNetworkBlock(NetworkBlocks.baseId(event.getBlockType()))) {
            return;
        }
        World world = store.getExternalData().getWorld();
        Vector3i pos = new Vector3i(event.getTargetBlock());
        world.execute(() -> NetworkEvents.afterBreak(world, pos));
    }
}
