package dev.agustinr.networkstorage.network;

import org.joml.Vector3i;

import java.util.List;

/** Resultado de recorrer una red: los bloques conectados entre sí. */
public record StorageNetwork(List<Vector3i> terminals, List<Vector3i> units, List<Vector3i> pipes, boolean truncated) {

    public int capacity() {
        return Math.min(units.size() * NetworkBlocks.UNIT_SLOTS, NetworkBlocks.MAX_SLOTS);
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
        return String.format("%s, %d units (%d slots), %d pipes%s",
                terminal, units.size(), capacity(), pipes.size(), truncated ? " [recorrido truncado]" : "");
    }

    public static String format(Vector3i pos) {
        return "(" + pos.x + ", " + pos.y + ", " + pos.z + ")";
    }
}
