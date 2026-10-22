package dev.agustinr.networkstorage.network;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import javax.annotation.Nullable;

/** Ids de los bloques del mod y cuántos slots aporta cada uno. */
public final class NetworkBlocks {

    /** Centro de la red: guarda los ítems y abre la UI. Una por red. */
    public static final String TERMINAL = "NetworkStorage_Terminal";
    public static final String UNIT = "NetworkStorage_Unit";
    /** Solo conecta: no aporta slots. */
    public static final String PIPE = "NetworkStorage_Pipe";

    /** Slots que aporta cada Storage Unit: lo mismo que un cofre chico (2 filas). */
    public static final int UNIT_SLOTS = 18;

    /**
     * Slots reales del inventario de la terminal. Tiene que coincidir con "Capacity" en
     * NetworkStorage_Terminal.json. Es el tope de la red: 512 units.
     */
    public static final int MAX_SLOTS = 9216;

    private NetworkBlocks() {}

    /**
     * Id base del bloque. Las formas del pipe (esquina, T, etc.) son estados del bloque y tienen
     * su propio id; esto devuelve el del bloque original para poder compararlo con las constantes.
     */
    @Nullable
    public static String baseId(@Nullable BlockType type) {
        if (type == null) {
            return null;
        }
        String base = type.getDefaultStateKey();
        return base != null ? base : type.getId();
    }

    public static boolean isNetworkBlock(@Nullable String id) {
        return isTerminal(id) || isUnit(id) || isPipe(id);
    }

    public static boolean isPipe(@Nullable String id) {
        return PIPE.equals(id);
    }

    public static boolean isTerminal(@Nullable String id) {
        return TERMINAL.equals(id);
    }

    public static boolean isUnit(@Nullable String id) {
        return UNIT.equals(id);
    }
}
