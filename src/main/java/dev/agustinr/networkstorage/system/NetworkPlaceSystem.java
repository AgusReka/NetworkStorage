package dev.agustinr.networkstorage.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.agustinr.networkstorage.network.NetworkBlocks;
import dev.agustinr.networkstorage.network.NetworkEvents;
import org.joml.Vector3i;

import javax.annotation.Nonnull;

public class NetworkPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public NetworkPlaceSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull PlaceBlockEvent event) {
        ItemStack item = event.getItemInHand();
        if (event.isCancelled() || item == null || item.isEmpty()) {
            return;
        }
        String placedId = item.getItemId();
        if (!NetworkBlocks.isNetworkBlock(placedId)) {
            return;
        }

        World world = store.getExternalData().getWorld();
        Vector3i pos = new Vector3i(event.getTargetBlock());
        PlayerRef player = store.getComponent(chunk.getReferenceTo(index), PlayerRef.getComponentType());

        // Una red tiene exactamente una terminal: no dejamos poner un bloque que una dos.
        if (NetworkEvents.terminalsAfterPlacing(world, pos, placedId) > 1) {
            event.setCancelled(true);
            if (player != null) {
                player.sendMessage(Message.raw("[Network Storage] No se puede: ese bloque conectaría dos terminales en la misma red."));
            }
            return;
        }

        // El bloque todavía no está puesto; recalculamos en el próximo tick.
        world.execute(() -> {
            if (!event.isCancelled()) {
                NetworkEvents.afterPlace(world, pos);
            }
        });
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
