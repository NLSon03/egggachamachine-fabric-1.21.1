package nlson.egggachamachine.data;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPityData {
    private static final Map<UUID, Integer> PITY_MAP = new HashMap<>();

    public static int get(UUID id) {
        return PITY_MAP.getOrDefault(id, 0);
    }

    public static void increase(UUID id) {
        PITY_MAP.put(id, get(id) + 1);
    }

    public static void reset(UUID id) {
        PITY_MAP.put(id, 0);
    }

    public static int get(PlayerEntity p) {
        return get(p.getUuid());
    }

    public static int get(ServerPlayerEntity p) {
        return get(p.getUuid());
    }

    public static void increase(PlayerEntity p) {
        increase(p.getUuid());
    }

    public static void increase(ServerPlayerEntity p) {
        increase(p.getUuid());
    }

    public static void reset(PlayerEntity p) {
        reset(p.getUuid());
    }

    public static void reset(ServerPlayerEntity p) {
        reset(p.getUuid());
    }

    // Persistence to player persistent NBT
    public static void saveToPlayer(PlayerEntity player) {
    }

    public static void loadFromPlayer(PlayerEntity player) {
    }

    // Clear in-memory map (optional helper)
    public static void clear() {
        PITY_MAP.clear();
    }

    // Additional API for external persistence handlers
    public static void set(UUID id, int pity) {
        PITY_MAP.put(id, pity);
    }

    public static Map<UUID, Integer> getAll() {
        return PITY_MAP;
    }
}
