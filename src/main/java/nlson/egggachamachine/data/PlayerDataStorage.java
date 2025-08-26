package nlson.egggachamachine.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public class PlayerDataStorage {
    private static final String FILE_NAME = "config/egggachamachine_playerdata.nbt";

    public static void saveAll(Map<UUID, Integer> pityMap, Map<UUID, Integer> rollsMap) {
        try {
            Path path = Paths.get(FILE_NAME);
            if (path.getParent() != null) Files.createDirectories(path.getParent());

            NbtCompound root = new NbtCompound();
            NbtList list = new NbtList();
            // iterate over union of keys so we don't lose entries present only in one map
            java.util.Set<UUID> keys = new java.util.HashSet<>();
            keys.addAll(pityMap.keySet());
            keys.addAll(rollsMap.keySet());
            for (UUID id : keys) {
                NbtCompound e = new NbtCompound();
                e.putUuid("id", id);
                e.putInt("pity", pityMap.getOrDefault(id, 0));
                e.putInt("rolls", rollsMap.getOrDefault(id, 0));
                list.add(e);
            }
            root.put("players", list);

            NbtIo.write(root, path);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void loadAll(Map<UUID, Integer> pityMap, Map<UUID, Integer> rollsMap) {
        try {
            Path path = Paths.get(FILE_NAME);
            if (!Files.exists(path)) return;

            NbtCompound root = NbtIo.read(path);
            if (root == null) return;
            NbtList list = root.getList("players", 10);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound e = list.getCompound(i);
                UUID id = e.getUuid("id");
                int pity = e.getInt("pity");
                int rolls = e.getInt("rolls");
                pityMap.put(id, pity);
                rollsMap.put(id, rolls);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
