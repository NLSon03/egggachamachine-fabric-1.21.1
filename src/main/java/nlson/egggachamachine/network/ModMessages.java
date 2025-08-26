// src/main/java/nlson/egggachamachine/network/ModMessages.java

package nlson.egggachamachine.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import nlson.egggachamachine.config.GachaConfigManager;
import nlson.egggachamachine.data.GachaPlayerPersistentState;
import nlson.egggachamachine.data.PlayerDataStorage;
import nlson.egggachamachine.data.PlayerPityData;
import nlson.egggachamachine.data.PlayerRollData;
import nlson.egggachamachine.screen.GachaMachineScreenHandler;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModMessages {
    public static final Identifier NAP_TIEN = Identifier.of("egggachamachine", "nap_tien");
    public static final Identifier QUAY = Identifier.of("egggachamachine", "quay");
    // --- server-side rate limit and debounced save ---
    private static final ConcurrentHashMap<java.util.UUID, Long> LAST_ROLL_MS = new ConcurrentHashMap<>();
    // minimum milliseconds between allowed rolls per player
    private static final long ROLL_COOLDOWN_MS = 200; // 200ms default (5 rps)
    // single-threaded scheduler used to flush player data to disk async
    private static final ScheduledExecutorService SAVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "egggacha-save-thread");
        t.setDaemon(true);
        return t;
    });
    // debounce flag to collapse multiple save requests into a single task
    private static final AtomicBoolean SAVE_SCHEDULED = new AtomicBoolean(false);
    private static final long SAVE_DEBOUNCE_SECONDS = 5; // coalesce writes for 5s

    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(NapTienPayload.ID, NapTienPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(QuayPayload.ID, QuayPayload.CODEC);
        // Gói nạp coin

        ServerPlayNetworking.registerGlobalReceiver(NapTienPayload.ID, (payload, context) -> {
            int coinCount = payload.coinCount();
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (coinCount > 0) {
                    // Add to player's roll count
                    PlayerRollData.add(player, coinCount);

                    // If the player currently has the gacha screen open on the server, try to remove
                    // coins from the screen handler's inventory so the server-side slot is consumed and
                    // then sync the screen back to the client. This prevents client-side duplication.
                    if (player.currentScreenHandler instanceof GachaMachineScreenHandler g) {
                        // remove coins from handler inventory slot 0 up to coinCount
                        int available = g.getInventory().getStack(0).getCount();
                        int toRemove = Math.min(available, coinCount);
                        if (toRemove > 0) {
                            g.getInventory().removeStack(0, toRemove);
                            g.sendContentUpdates();
                        }
                    }

                    // persist in-memory state and schedule async disk flush
                    ServerWorld sw = (ServerWorld) player.getWorld();
                    GachaPlayerPersistentState.get(sw).set(player.getUuid(), PlayerPityData.get(player), PlayerRollData.get(player));
                    scheduleDebouncedSave(sw);
                    player.sendMessage(Text.of("Đã nạp " + coinCount + " Gacha Coin!"), false);
                    syncOpenScreen(player);

                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(QuayPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();

                if (!tryConsumeRollCooldown(player)) {
                    player.sendMessage(Text.of("§eBạn quay quá nhanh — vui lòng chờ một chút."), true);
                    player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);
                    return;
                }

                if (!PlayerRollData.use(player)) {
                    player.sendMessage(Text.of("Bạn không đủ lượt quay!"), false);
                    return;
                }

                ServerWorld sw = (ServerWorld) player.getWorld();

                // Lấy thông tin pity trước khi roll
                int pityBefore = PlayerPityData.get(player);

                // Thực hiện roll với pity system
                GachaConfigManager.RewardEntry rewardEntry = rollReward(player);

                // Lấy thông tin pity sau khi roll
                int pityAfter = PlayerPityData.get(player);

                // Thông báo cho player về pity milestone nếu có
                String milestoneInfo = GachaConfigManager.getNextMilestoneInfo(pityAfter);

                // Kiểm tra xem có phải là pity reward không
                if (pityAfter == 0 && pityBefore >= 1000) {
                    // Player vừa nhận legendary pity reward và được reset về 0
                    player.sendMessage(Text.of("§6§l★ PITY LEGENDARY SHINY! ★ " + rewardEntry.pokemon + " - Pity đã được reset!"), false);
                } else if (pityBefore == 850 && pityAfter > pityBefore) {
                    // Player nhận UB pity nhưng pity vẫn tăng
                    player.sendMessage(Text.of("§5§l★ PITY UB SHINY! ★ " + rewardEntry.pokemon + " - Tiếp tục tích lũy đến Legendary!"), false);
                } else if (pityBefore == 700 && pityAfter > pityBefore) {
                    // Player nhận Mythic pity nhưng pity vẫn tăng
                    player.sendMessage(Text.of("§d§l★ PITY MYTHIC SHINY! ★ " + rewardEntry.pokemon + " - Tiếp tục tích lũy!"), false);
                } else if (pityBefore == 400 && pityAfter > pityBefore) {
                    // Player nhận IronGreat pity nhưng pity vẫn tăng
                    player.sendMessage(Text.of("§7§l★ PITY IRONGREAT SHINY! ★ " + rewardEntry.pokemon + " - Tiếp tục tích lũy!"), false);
                } else if (pityBefore == 200 && pityAfter > pityBefore) {
                    // Player nhận Common pity nhưng pity vẫn tăng
                    player.sendMessage(Text.of("§a§l★ PITY COMMON SHINY! ★ " + rewardEntry.pokemon + " - Tiếp tục tích lũy!"), false);
                } else {
                    // Roll bình thường hoặc trúng legendary từ RNG, hiển thị thông tin milestone
                    if (pityAfter == 0 && pityBefore > 0) {
                        // Trúng legendary từ RNG
                        player.sendMessage(Text.of("§6§l★ LUCKY LEGENDARY! ★ " + rewardEntry.pokemon + " - Pity đã được reset!"), false);
                    } else {
                        // Roll bình thường
                        player.sendMessage(Text.of("§7Pity: " + pityAfter + " - " + milestoneInfo), true);
                    }
                }

                // Lưu dữ liệu player
                GachaPlayerPersistentState.get(sw).set(player.getUuid(), PlayerPityData.get(player), PlayerRollData.get(player));
                scheduleDebouncedSave(sw);

                String commandToExecute = rewardEntry.buildCommand(player);

                // capture nearby item entities before running the command so we can detect newly
                // spawned items (the reward command may spawn an item entity if the player's
                // inventory is full)
                var world = player.getWorld();
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();
                Box captureBox = new Box(px - 2.0, py - 2.0, pz - 2.0, px + 2.0, py + 2.0, pz + 2.0);
                Set<Integer> beforeIds = new HashSet<>();
                for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, captureBox, e -> true)) {
                    beforeIds.add(e.getId());
                }

                context.server().getCommandManager().executeWithPrefix(
                        context.server().getCommandSource().withSilent(), // Dùng withSilent() để không spam log của server
                        commandToExecute
                );

                // Process any newly spawned nearby item entities (likely the egg dropped by the
                // external command) and attempt to transfer them to the player.
                collectNearbyDroppedItems(player, beforeIds, captureBox);

                syncOpenScreen(player);
            });
        });
    }

    private static boolean tryConsumeRollCooldown(ServerPlayerEntity player) {
        java.util.UUID id = player.getUuid();
        long now = System.currentTimeMillis();
        Long last = LAST_ROLL_MS.get(id);
        if (last == null || now - last >= ROLL_COOLDOWN_MS) {
            LAST_ROLL_MS.put(id, now);
            return true;
        }
        return false;
    }

    private static void scheduleDebouncedSave(ServerWorld world) {
        // only schedule one save task per debounce window
        if (SAVE_SCHEDULED.compareAndSet(false, true)) {
            SAVE_EXECUTOR.schedule(() -> {
                try {
                    // flush in-memory maps to config file (async disk IO)
                    PlayerDataStorage.saveAll(PlayerPityData.getAll(), PlayerRollData.getAll());
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    SAVE_SCHEDULED.set(false);
                }
            }, SAVE_DEBOUNCE_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static void flushNow(ServerWorld world) {
        // immediate async write (no debounce) for important events; runs off-server thread
        SAVE_EXECUTOR.execute(() -> {
            try {
                PlayerDataStorage.saveAll(PlayerPityData.getAll(), PlayerRollData.getAll());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    /**
     * Public helper: schedule an async flush (non-blocking).
     */
    public static void flushNowAsync(ServerWorld world) {
        flushNow(world);
    }

    /**
     * Public helper: blocking flush on caller thread (use on shutdown to ensure data saved).
     */
    public static void flushNowBlocking() {
        try {
            PlayerDataStorage.saveAll(PlayerPityData.getAll(), PlayerRollData.getAll());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static GachaConfigManager.RewardEntry rollReward(ServerPlayerEntity player) {
        Random rand = new Random();
        // Sử dụng phương thức mới với player parameter để hỗ trợ pity system
        return GachaConfigManager.pickRandomReward(rand, player);
    }

    private static void syncOpenScreen(ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof GachaMachineScreenHandler g) {
            g.setValues(
                    PlayerPityData.get(player),
                    PlayerRollData.get(player)
            );
        }
    }

    /**
     * Process any newly spawned ItemEntity instances inside the given box that were not present
     * before the reward command ran (identified via their entity IDs in beforeIds).
     */
    private static void collectNearbyDroppedItems(ServerPlayerEntity player, Set<Integer> beforeIds, Box box) {
        try {
            var world = player.getWorld();

            for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class, box, e -> true)) {
                // only handle entities that didn't exist before the command
                if (beforeIds.contains(itemEntity.getId())) continue;

                ItemStack stack = itemEntity.getStack();
                if (stack.isEmpty()) continue;

                // Try add to player inventory
                boolean inserted = player.getInventory().insertStack(stack.copy());
                if (inserted) {
                    itemEntity.discard();
                    // ensure the server syncs inventory contents to the client so tooltips and
                    // item names appear immediately in any open GUI
                    try {
                        if (player.currentScreenHandler != null) {
                            player.currentScreenHandler.sendContentUpdates();
                        }
                        syncOpenScreen(player);
                    } catch (Throwable ignored) {}

                    player.sendMessage(Text.of("§aBạn đã nhận được phần thưởng (đã vào hành trang)!"), false);
                    continue;
                }

                // Last resort: keep item on ground but give short pickup delay so player can pick it up
                itemEntity.setPickupDelay(40); // 2 seconds
                player.sendMessage(Text.of("§cHành trang đầy — phần thưởng đã rơi xuống đất, vui lòng nhặt."), false);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public record NapTienPayload(int coinCount) implements CustomPayload {
        public static final CustomPayload.Id<NapTienPayload> ID = new CustomPayload.Id<>(NAP_TIEN);
        public static final PacketCodec<RegistryByteBuf, NapTienPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER,
                NapTienPayload::coinCount,
                NapTienPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record QuayPayload() implements CustomPayload {
        public static final CustomPayload.Id<QuayPayload> ID = new CustomPayload.Id<>(QUAY);
        public static final PacketCodec<RegistryByteBuf, QuayPayload> CODEC = PacketCodec.unit(new QuayPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}