package dev.agustinr.networkstorage.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.agustinr.networkstorage.network.NetworkBlocks;
import dev.agustinr.networkstorage.storage.TerminalStorage;

import javax.annotation.Nonnull;

/**
 * Una terminal con ítems no recibe daño sin jugador (explosiones): si se rompiera, todo su contenido
 * caería al suelo. Es la misma regla que para los jugadores, que tienen que vaciarla antes de romperla.
 * El daño hecho por jugadores llega como evento de entidad, así que este sistema no lo ve.
 */
public class TerminalExplosionGuardSystem extends WorldEventSystem<EntityStore, DamageBlockEvent> {

    public TerminalExplosionGuardSystem() {
        super(DamageBlockEvent.class);
    }

    @Override
    public void handle(@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull DamageBlockEvent event) {
        if (event.isCancelled() || !NetworkBlocks.isTerminal(NetworkBlocks.baseId(event.getBlockType()))) {
            return;
        }
        World world = store.getExternalData().getWorld();
        if (!TerminalStorage.isEmpty(world, event.getTargetBlock())) {
            event.setCancelled(true);
        }
    }
}
