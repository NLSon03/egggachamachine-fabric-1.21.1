package nlson.egggachamachine.data;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.UUID;

public class PlayerRollData {
    private static final HashMap<UUID, Integer> ROLL_MAP = new HashMap<>();

    public static int get(UUID id) {
        return ROLL_MAP.getOrDefault(id, 0);
    }

    public static void add(UUID id, int amount) {
        ROLL_MAP.put(id, get(id) + amount);
    }

    public static boolean use(UUID id) {
        int cur = get(id);
        if (cur > 0) {
            ROLL_MAP.put(id, cur - 1);
            return true;
        }
        return false;
    }

    public static int get(PlayerEntity p) {
        return get(p.getUuid());
    }

    public static int get(ServerPlayerEntity p) {
        return get(p.getUuid());
    }

    public static void add(PlayerEntity p, int amount) {
        add(p.getUuid(), amount);
    }

    public static void add(ServerPlayerEntity p, int amount) {
        add(p.getUuid(), amount);
    }

    public static boolean use(PlayerEntity p) {
        return use(p.getUuid());
    }

    public static boolean use(ServerPlayerEntity p) {
        return use(p.getUuid());
    }

    // Additional API for external persistence handlers
    public static void set(UUID id, int rolls) {
        ROLL_MAP.put(id, rolls);
    }

    public static HashMap<UUID, Integer> getAll() {
        return ROLL_MAP;
    }
}
