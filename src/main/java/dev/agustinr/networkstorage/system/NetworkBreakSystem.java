package dev.agustinr.networkstorage.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.agustinr.networkstorage.network.NetworkBlocks;
import dev.agustinr.networkstorage.network.NetworkEvents;
import dev.agustinr.networkstorage.network.NetworkScanner;
import dev.agustinr.networkstorage.storage.TerminalStorage;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.List;

public class NetworkBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public NetworkBreakSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
        String blockId = NetworkBlocks.baseId(event.getBlockType());
        if (event.isCancelled() || !NetworkBlocks.isNetworkBlock(blockId)) {
            return;
        }
        World world = store.getExternalData().getWorld();
        Vector3i pos = new Vector3i(event.getTargetBlock());
        PlayerRef player = store.getComponent(chunk.getReferenceTo(index), PlayerRef.getComponentType());

        // Romper una terminal con ítems los tiraría todos al suelo de golpe: hay que vaciarla antes.
        if (NetworkBlocks.isTerminal(blockId) && !TerminalStorage.isEmpty(world, pos)) {
            event.setCancelled(true);
            if (player != null) {
                player.sendMessage(Message.raw("[Network Storage] Vaciá la terminal antes de romperla."));
            }
            return;
        }

        // El bloque se saca después del evento; recalculamos en el próximo tick. Las celdas se toman
        // ahora, mientras el bloque existe (la terminal ocupa dos).
        List<Vector3i> cells = new NetworkScanner(world).cellsAt(pos);
        world.execute(() -> {
            if (!event.isCancelled()) {
                NetworkEvents.afterBreak(world, cells);
            }
        });
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
