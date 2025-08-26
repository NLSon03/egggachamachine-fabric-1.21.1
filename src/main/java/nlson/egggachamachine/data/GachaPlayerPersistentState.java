package nlson.egggachamachine.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GachaPlayerPersistentState extends PersistentState {
    public static final Type<GachaPlayerPersistentState> TYPE =
            new Type<>(
                    GachaPlayerPersistentState::new,
                    GachaPlayerPersistentState::fromNbt,
                    null
            );
    private static final String NAME = "egggachamachine_player_data";
    private final Map<UUID, Integer> pityMap = new HashMap<>();
    private final Map<UUID, Integer> rollsMap = new HashMap<>();

    public GachaPlayerPersistentState() {
    }

    public static GachaPlayerPersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, NAME);
    }

    public static GachaPlayerPersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        GachaPlayerPersistentState s = new GachaPlayerPersistentState();
        if (nbt.contains("players")) {
            NbtList list = nbt.getList("players", 10);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompound(i);
                UUID id = entry.getUuid("id");
                s.pityMap.put(id, entry.getInt("pity"));
                s.rollsMap.put(id, entry.getInt("rolls"));
            }
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        var keys = new java.util.HashSet<>(pityMap.keySet());
        keys.addAll(rollsMap.keySet());

        for (UUID id : keys) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("id", id);
            entry.putInt("pity", pityMap.getOrDefault(id, 0));
            entry.putInt("rolls", rollsMap.getOrDefault(id, 0));
            list.add(entry);
        }
        nbt.put("players", list);
        return nbt;
    }

    public int getPity(UUID id) {
        return pityMap.getOrDefault(id, 0);
    }

    public int getRolls(UUID id) {
        return rollsMap.getOrDefault(id, 0);
    }

    public void set(UUID id, int pity, int rolls) {
        pityMap.put(id, pity);
        rollsMap.put(id, rolls);
        markDirty();
    }
}
