package dev.agustinr.networkstorage.storage;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.SectionReader;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.agustinr.networkstorage.network.BlockCells;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Abre la ventana de la terminal ({@link TerminalPage}). */
public class OpenTerminalInteraction extends SimpleBlockInteraction {

    public static final String TYPE = "NetworkStorage_OpenTerminal";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<OpenTerminalInteraction> CODEC = BuilderCodec.builder(
                    OpenTerminalInteraction.class, OpenTerminalInteraction::new, SimpleBlockInteraction.CODEC)
            .documentation("Opens the storage network window of a Storage Terminal.")
            .build();

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand, @Nonnull Vector3i targetPos,
                                     @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            LOGGER.atWarning().log("[NetworkStorage] La entidad que usó la terminal no es un jugador");
            return;
        }

        // La terminal ocupa dos celdas: el inventario y las sesiones abiertas usan la de origen.
        ChunkStore chunkStore = world.getChunkStore();
        Vector3i pos = BlockCells.origin(new SectionReader(chunkStore), targetPos);
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            fail(playerRef, "no se encontró el chunk de la terminal");
            return;
        }
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z);
        Store<ChunkStore> chunkComponents = chunkStore.getStore();
        ItemContainerBlock containerBlock = blockRef != null
                ? chunkComponents.getComponent(blockRef, ItemContainerBlock.getComponentType())
                : null;
        if (containerBlock == null) {
            // Pasa con terminales puestas antes de que el bloque tuviera inventario.
            fail(playerRef, "esta terminal no tiene inventario. Si la pusiste con una versión vieja del mod, rompela y ponela de nuevo");
            return;
        }
        TerminalPage page = new TerminalPage(playerRef, world, pos);
        player.getPageManager().openCustomPage(ref, store, page);
        TerminalSessions.open(page);
    }

    private static void fail(@Nonnull PlayerRef player, @Nonnull String reason) {
        LOGGER.atWarning().log("[NetworkStorage] No se abrió la terminal: " + reason);
        player.sendMessage(Message.raw("[Network Storage] No se pudo abrir la terminal: " + reason + "."));
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context,
                                             @Nullable ItemStack itemInHand, @Nonnull World world,
                                             @Nonnull Vector3i targetBlock) {
    }
}
