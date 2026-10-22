package dev.agustinr.networkstorage.storage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.agustinr.networkstorage.network.NetworkScanner;
import dev.agustinr.networkstorage.network.StorageNetwork;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ventana de la terminal: a la izquierda los ítems de la red (uno por tipo, con su total y scroll),
 * a la derecha el inventario del jugador.
 * <ul>
 *   <li>Clic izquierdo en un ítem de la red: saca un stack. Clic derecho: saca uno.</li>
 *   <li>Clic izquierdo en un ítem del inventario: lo guarda entero. Clic derecho: guarda uno.</li>
 * </ul>
 */
public class TerminalPage extends InteractiveCustomUIPage<TerminalPage.PageEvent> {

    private static final String LAYOUT = "NetworkStorage/TerminalPage.ui";
    private static final String SLOT = "NetworkStorage/TerminalSlot.ui";

    private final World world;
    private final Vector3i pos;
    private String search = "";
    /** Ítems mostrados en la última actualización, para saber qué se clickeó. */
    private List<NetworkInventory.Entry> shown = List.of();

    public TerminalPage(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull Vector3i pos) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEvent.CODEC);
        this.world = world;
        this.pos = new Vector3i(pos);
    }

    public World getWorld() {
        return world;
    }

    public Vector3i getPos() {
        return pos;
    }

    /** Inventario real de la terminal, o null si ya no existe (por ejemplo, la rompieron). */
    @Nullable
    private SimpleItemContainer storage() {
        return TerminalStorage.get(world, pos);
    }

    private int capacity() {
        StorageNetwork network = new NetworkScanner(world).scan(pos);
        return network != null ? network.capacity() : 0;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        ui.append(LAYOUT);
        ui.set("#SearchInput.Value", search);
        buildContent(ref, store, ui, events);
    }

    /**
     * Lo que cambia entre actualizaciones: las grillas y sus eventos. Las actualizaciones parciales
     * reemplazan todos los eventos, así que el del buscador se vuelve a registrar cada vez.
     */
    private void buildContent(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
                EventData.of("Action", "Search").append("@Search", "#SearchInput.Value"), false);

        SimpleItemContainer storage = storage();
        if (storage == null) {
            ui.set("#CapacityLabel.Text", "La terminal ya no existe.");
            ui.clear("#NetworkGrid");
            shown = List.of();
            return;
        }
        buildNetworkGrid(ui, events, storage);
        buildPlayerGrid(ref, store, ui, events);
    }

    private void buildNetworkGrid(UICommandBuilder ui, UIEventBuilder events, SimpleItemContainer storage) {
        int capacity = capacity();
        int used = NetworkInventory.usedSlots(storage);
        String status = used > capacity ? "  (red sobrecargada: agregá units)" : used == capacity ? "  (red llena)" : "";
        ui.set("#CapacityLabel.Text", "Slots: " + used + " / " + capacity + status);

        String query = normalize(search.trim());
        List<NetworkInventory.Entry> entries = new ArrayList<>();
        for (NetworkInventory.Entry entry : NetworkInventory.summarize(storage)) {
            if (query.isEmpty() || matches(entry.sample(), query)) {
                entries.add(entry);
            }
        }
        shown = entries;

        ui.clear("#NetworkGrid");
        for (int i = 0; i < entries.size(); i++) {
            NetworkInventory.Entry entry = entries.get(i);
            String selector = "#NetworkGrid[" + i + "]";
            ui.append("#NetworkGrid", SLOT);
            ui.set(selector + " #Item.ItemId", entry.sample().getItemId());
            ui.set(selector + " #Quantity.Text", formatQuantity(entry.total()));
            events.addEventBinding(CustomUIEventBindingType.Activating, selector + " #Button",
                    EventData.of("Action", "Take").append("Index", String.valueOf(i)).append("Amount", "Stack"), false);
            events.addEventBinding(CustomUIEventBindingType.RightClicking, selector + " #Button",
                    EventData.of("Action", "Take").append("Index", String.valueOf(i)).append("Amount", "One"), false);
        }
        ui.set("#EmptyLabel.Visible", entries.isEmpty());
    }

    private void buildPlayerGrid(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        ItemContainer inventory = InventoryComponent.getCombined(store, ref, InventoryComponent.HOTBAR_FIRST);
        ui.clear("#PlayerGrid");
        int shownSlots = 0;
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (ItemStack.isEmpty(stack)) {
                continue;
            }
            String selector = "#PlayerGrid[" + shownSlots + "]";
            ui.append("#PlayerGrid", SLOT);
            ui.set(selector + " #Item.ItemId", stack.getItemId());
            ui.set(selector + " #Quantity.Text", stack.getQuantity() > 1 ? String.valueOf(stack.getQuantity()) : "");
            events.addEventBinding(CustomUIEventBindingType.Activating, selector + " #Button",
                    EventData.of("Action", "Deposit").append("Slot", String.valueOf(slot)).append("Amount", "Stack"), false);
            events.addEventBinding(CustomUIEventBindingType.RightClicking, selector + " #Button",
                    EventData.of("Action", "Deposit").append("Slot", String.valueOf(slot)).append("Amount", "One"), false);
            shownSlots++;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEvent event) {
        if (event.action == null) {
            return;
        }
        switch (event.action) {
            case "Search" -> {
                search = event.search != null ? event.search : "";
                refresh(ref, store);
            }
            case "Take" -> take(ref, store, event);
            case "Deposit" -> deposit(ref, store, event);
            default -> {
            }
        }
    }

    private void take(Ref<EntityStore> ref, Store<EntityStore> store, PageEvent event) {
        SimpleItemContainer storage = storage();
        int index = parse(event.index);
        if (storage == null || index < 0 || index >= shown.size()) {
            refresh(ref, store);
            return;
        }
        ItemStack sample = shown.get(index).sample();
        int amount = "One".equals(event.amount) ? 1 : NetworkInventory.maxStack(sample);

        ItemStack taken = NetworkInventory.take(storage, sample, amount);
        if (taken != null) {
            ItemContainer inventory = InventoryComponent.getCombined(store, ref, InventoryComponent.HOTBAR_FIRST);
            ItemStack remainder = inventory.addItemStack(taken).getRemainder();
            if (!ItemStack.isEmpty(remainder)) {
                // No entró en el inventario del jugador: vuelve a la red aunque esté llena, para no perderlo.
                NetworkInventory.insert(storage, remainder, Integer.MAX_VALUE);
                playerRef.sendMessage(Message.raw("[Network Storage] Tu inventario está lleno."));
            }
        }
        TerminalSessions.refreshAll(world, pos);
    }

    private void deposit(Ref<EntityStore> ref, Store<EntityStore> store, PageEvent event) {
        SimpleItemContainer storage = storage();
        int slot = parse(event.slot);
        ItemContainer inventory = InventoryComponent.getCombined(store, ref, InventoryComponent.HOTBAR_FIRST);
        if (storage == null || slot < 0 || slot >= inventory.getCapacity()) {
            refresh(ref, store);
            return;
        }
        ItemStack stack = inventory.getItemStack((short) slot);
        if (ItemStack.isEmpty(stack)) {
            refresh(ref, store);
            return;
        }
        int amount = "One".equals(event.amount) ? 1 : stack.getQuantity();

        int notStored = NetworkInventory.insert(storage, stack.withQuantity(amount), capacity());
        int stored = amount - notStored;
        if (stored > 0) {
            inventory.setItemStackForSlot((short) slot, stack.withQuantity(stack.getQuantity() - stored));
        }
        if (notStored > 0) {
            playerRef.sendMessage(Message.raw("[Network Storage] La red está llena: agregá Storage Units."));
        }
        TerminalSessions.refreshAll(world, pos);
    }

    /**
     * Actualiza las grillas con el estado actual. Es una actualización parcial: la ventana no se
     * vuelve a crear, así que el scroll y el texto del buscador se mantienen.
     */
    public void refresh(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        buildContent(ref, store, ui, events);
        sendUpdate(ui, events, false);
    }

    /** El ítem coincide con la búsqueda por su nombre en el idioma del jugador o por su id. */
    private boolean matches(ItemStack stack, String query) {
        if (normalize(stack.getItemId()).contains(query)) {
            return true;
        }
        String name = I18nModule.get().getMessage(playerRef.getLanguage(), stack.getItem().getTranslationKey());
        return name != null && normalize(name).contains(query);
    }

    /** Minúsculas y sin tildes, para que "pocion" encuentre "Poción". */
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Refresca la página desde afuera (por ejemplo, porque otro jugador cambió la red). */
    public void refresh() {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            refresh(ref, ref.getStore());
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        TerminalSessions.close(this);
    }

    private static int parse(@Nullable String value) {
        try {
            return value != null ? Integer.parseInt(value) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String formatQuantity(long quantity) {
        if (quantity < 1_000) {
            return quantity > 1 ? String.valueOf(quantity) : "";
        }
        if (quantity < 1_000_000) {
            return String.format(Locale.ROOT, "%.1fk", quantity / 1_000.0).replace(".0k", "k");
        }
        return String.format(Locale.ROOT, "%.1fM", quantity / 1_000_000.0).replace(".0M", "M");
    }

    /** Datos que manda el cliente al interactuar con la página. */
    public static class PageEvent {
        public static final BuilderCodec<PageEvent> CODEC = BuilderCodec.builder(PageEvent.class, PageEvent::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, v) -> e.action = v, e -> e.action)
                .add()
                .append(new KeyedCodec<>("Index", Codec.STRING), (e, v) -> e.index = v, e -> e.index)
                .add()
                .append(new KeyedCodec<>("Slot", Codec.STRING), (e, v) -> e.slot = v, e -> e.slot)
                .add()
                .append(new KeyedCodec<>("Amount", Codec.STRING), (e, v) -> e.amount = v, e -> e.amount)
                .add()
                .append(new KeyedCodec<>("@Search", Codec.STRING), (e, v) -> e.search = v, e -> e.search)
                .add()
                .build();

        String action;
        String index;
        String slot;
        String amount;
        String search;
    }
}
