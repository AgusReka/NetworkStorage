package dev.agustinr.networkstorage.network;

import org.joml.Vector3i;

import java.util.List;

/**
 * Resultado de recorrer una red: los bloques conectados entre sí.
 *
 * @param unitSlots suma de los slots que aportan las units, según su tier
 */
public record StorageNetwork(List<Vector3i> terminals, List<Vector3i> units, List<Vector3i> pipes, int unitSlots,
                             boolean truncated) {

    public int capacity() {
        return Math.min(unitSlots, NetworkBlocks.MAX_SLOTS);
    }

    public boolean contains(Vector3i pos) {
        return terminals.contains(pos) || units.contains(pos) || pipes.contains(pos);
    }

    public String describe() {
        String terminal = switch (terminals.size()) {
            case 0 -> "sin terminal";
            case 1 -> "terminal en " + format(terminals.getFirst());
            default -> terminals.size() + " terminales (conflicto)";
        };
        return String.format("%s, %d/%d units (%d slots), %d pipes%s",
                terminal, units.size(), NetworkBlocks.MAX_UNITS, capacity(), pipes.size(),
                truncated ? " [recorrido truncado]" : "");
    }

    public static String format(Vector3i pos) {
        return "(" + pos.x + ", " + pos.y + ", " + pos.z + ")";
    }
}
