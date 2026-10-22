package dev.agustinr.networkstorage.storage;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Operaciones sobre el inventario real de la terminal.
 * <p>
 * Los slots ocupados cuentan contra la capacidad de la red (18 por unit). El inventario real es más
 * grande que eso, así que acá se decide qué entra: se completan stacks existentes siempre, y se ocupa
 * un slot nuevo solo si quedan slots libres en la red. Sacar está siempre permitido.
 */
public final class NetworkInventory {

    private NetworkInventory() {}

    /** Un tipo de ítem guardado en la red, con su total sumando todos sus stacks. */
    public record Entry(@Nonnull ItemStack sample, long total) {}

    /** Ítems de la red agrupados por tipo (ítems apilables entre sí), de mayor a menor cantidad. */
    public static List<Entry> summarize(@Nonnull ItemContainer storage) {
        List<ItemStack> samples = new ArrayList<>();
        List<Long> totals = new ArrayList<>();
        for (short slot = 0; slot < storage.getCapacity(); slot++) {
            ItemStack stack = storage.getItemStack(slot);
            if (ItemStack.isEmpty(stack)) {
                continue;
            }
            int index = indexOfStackable(samples, stack);
            if (index < 0) {
                samples.add(stack);
                totals.add((long) stack.getQuantity());
            } else {
                totals.set(index, totals.get(index) + stack.getQuantity());
            }
        }
        List<Entry> entries = new ArrayList<>(samples.size());
        for (int i = 0; i < samples.size(); i++) {
            entries.add(new Entry(samples.get(i), totals.get(i)));
        }
        entries.sort(Comparator.comparingLong(Entry::total).reversed()
                .thenComparing(e -> e.sample().getItemId()));
        return entries;
    }

    public static int usedSlots(@Nonnull ItemContainer storage) {
        int used = 0;
        for (short slot = 0; slot < storage.getCapacity(); slot++) {
            if (!ItemStack.isEmpty(storage.getItemStack(slot))) {
                used++;
            }
        }
        return used;
    }

    public static int maxStack(@Nonnull ItemStack stack) {
        Item item = stack.getItem();
        return item != null ? Math.max(1, item.getMaxStack()) : 1;
    }

    /**
     * Saca hasta amount ítems apilables con sample. Devuelve lo que sacó (null si no había).
     * Empieza por los últimos slots para que los primeros queden ordenados.
     */
    @Nullable
    public static ItemStack take(@Nonnull ItemContainer storage, @Nonnull ItemStack sample, int amount) {
        int taken = 0;
        for (short slot = (short) (storage.getCapacity() - 1); slot >= 0 && taken < amount; slot--) {
            ItemStack stack = storage.getItemStack(slot);
            if (ItemStack.isEmpty(stack) || !canStack(stack, sample)) {
                continue;
            }
            int removed = Math.min(stack.getQuantity(), amount - taken);
            storage.setItemStackForSlot(slot, stack.withQuantity(stack.getQuantity() - removed));
            taken += removed;
        }
        return taken > 0 ? sample.withQuantity(taken) : null;
    }

    /**
     * Guarda stack respetando la capacidad de la red. Devuelve cuántos ítems no entraron.
     *
     * @param capacity slots que aporta la red; usar Integer.MAX_VALUE para ignorarla
     */
    public static int insert(@Nonnull ItemContainer storage, @Nonnull ItemStack stack, int capacity) {
        int remaining = stack.getQuantity();
        int max = maxStack(stack);

        // 1. Completar stacks que ya existen: no ocupa slots nuevos.
        for (short slot = 0; slot < storage.getCapacity() && remaining > 0; slot++) {
            ItemStack existing = storage.getItemStack(slot);
            if (ItemStack.isEmpty(existing) || !canStack(existing, stack) || existing.getQuantity() >= max) {
                continue;
            }
            int added = Math.min(max - existing.getQuantity(), remaining);
            storage.setItemStackForSlot(slot, existing.withQuantity(existing.getQuantity() + added));
            remaining -= added;
        }

        // 2. Slots vacíos, solo mientras la red tenga lugar.
        int used = usedSlots(storage);
        for (short slot = 0; slot < storage.getCapacity() && remaining > 0 && used < capacity; slot++) {
            if (!ItemStack.isEmpty(storage.getItemStack(slot))) {
                continue;
            }
            int added = Math.min(max, remaining);
            storage.setItemStackForSlot(slot, stack.withQuantity(added));
            remaining -= added;
            used++;
        }
        return remaining;
    }

    /**
     * Si dos stacks se pueden juntar. Igual que {@link ItemStack#isStackableWith}, pero trata la
     * metadata vacía igual que la ausente: el mismo ítem puede llegar de un lado con una y de otro con la otra.
     */
    public static boolean canStack(@Nullable ItemStack a, @Nullable ItemStack b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getItemId().equals(b.getItemId())
                && Double.compare(a.getDurability(), b.getDurability()) == 0
                && Double.compare(a.getMaxDurability(), b.getMaxDurability()) == 0
                && a.getQualityIndex() == b.getQualityIndex()
                && sameMetadata(a.getMetadata(), b.getMetadata());
    }

    private static boolean sameMetadata(@Nullable BsonDocument a, @Nullable BsonDocument b) {
        boolean aEmpty = a == null || a.isEmpty();
        boolean bEmpty = b == null || b.isEmpty();
        if (aEmpty || bEmpty) {
            return aEmpty && bEmpty;
        }
        return a.equals(b);
    }

    private static int indexOfStackable(@Nonnull List<ItemStack> samples, @Nonnull ItemStack stack) {
        for (int i = 0; i < samples.size(); i++) {
            if (canStack(samples.get(i), stack)) {
                return i;
            }
        }
        return -1;
    }
}
