package dev.agustinr.networkstorage.network;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import javax.annotation.Nullable;

/** Ids de los bloques del mod y cuántos slots aporta cada uno. */
public final class NetworkBlocks {

    /** Centro de la red: guarda los ítems y abre la UI. Una por red. */
    public static final String TERMINAL = "NetworkStorage_Terminal";
    /** Units por tier. La T1 conserva el id original para no romper las que ya están puestas. */
    public static final String UNIT = "NetworkStorage_Unit";
    public static final String UNIT_T2 = "NetworkStorage_Unit_T2";
    public static final String UNIT_T3 = "NetworkStorage_Unit_T3";
    /** Solo conecta: no aporta slots. */
    public static final String PIPE = "NetworkStorage_Pipe";

    /** Slots que aporta cada tier de Unit: 2, 4 y 6 filas. */
    public static final int UNIT_T1_SLOTS = 18;
    public static final int UNIT_T2_SLOTS = 36;
    public static final int UNIT_T3_SLOTS = 54;

    /** Máximo de Units conectadas a una red (de cualquier tier). */
    public static final int MAX_UNITS = 16;

    /**
     * Slots reales del inventario de la terminal. Tiene que coincidir con "Capacity" en
     * NetworkStorage_Terminal.json, y no se puede bajar: al cargar, el juego tiraría los ítems que
     * no entren. Es mucho más que el máximo alcanzable (16 Units T3 = 864 slots).
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
        return unitSlots(id) > 0;
    }

    /** Slots que aporta el bloque, o 0 si no es una Unit. */
    public static int unitSlots(@Nullable String id) {
        if (id == null) {
            return 0;
        }
        return switch (id) {
            case UNIT -> UNIT_T1_SLOTS;
            case UNIT_T2 -> UNIT_T2_SLOTS;
            case UNIT_T3 -> UNIT_T3_SLOTS;
            default -> 0;
        };
    }
}
