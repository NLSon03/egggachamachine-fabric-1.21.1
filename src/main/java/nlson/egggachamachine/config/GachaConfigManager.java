package nlson.egggachamachine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import nlson.egggachamachine.data.PlayerPityData;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GachaConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("gacha_rewards.json");

    private static final Map<String, String> SPECIAL_DEFAULT_PARAMS = Map.of(
            "nature", "bashful",
            "hp_iv", "31",
            "attack_iv", "31",
            "defence_iv", "31",
            "special_attack_iv", "31",
            "special_defence_iv", "31",
            "speed_iv", "31",
            "shiny", "yes"
    );
    private static List<String> LEGENDARY_SHINY_REWARDS;
    private static List<String> LEGENDARY_REGULAR_REWARDS;
    private static List<String> MYTHIC_SHINY_REWARDS;
    private static List<String> MYTHIC_REGULAR_REWARDS;
    private static List<String> UB_SHINY_REWARDS;
    private static List<String> UB_REGULAR_REWARDS;
    private static List<String> IRONGREAT_SHINY_REWARDS;
    private static List<String> IRONGREAT_REGULAR_REWARDS;
    private static List<String> JACKPOT_REWARDS;
    private static List<String> COMMON_REWARDS;
    private static List<String> COMMON_SHINY_REWARDS;
    private static GachaRates RATES;
    private static PityMilestones PITY_MILESTONES;
    private static float LUCK_MULTIPLIER;

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                try (InputStream bundledConfig = GachaConfigManager.class.getResourceAsStream("/gacha_rewards.json")) {
                    if (bundledConfig != null) {
                        Files.copy(bundledConfig, CONFIG_PATH);
                    } else {
                        RewardConfig defaultConfig = new RewardConfig();

                        defaultConfig.luck_multiplier = 0.05f;
                        defaultConfig.rates = new GachaRates();
                        defaultConfig.rates.legendary_shiny = 0.01f;
                        defaultConfig.rates.legendary_regular = 0.5f;
                        defaultConfig.rates.mythic_shiny = 0.05f;
                        defaultConfig.rates.mythic_regular = 0.5f;
                        defaultConfig.rates.ub_shiny = 0.01f;
                        defaultConfig.rates.ub_regular = 0.5f;
                        defaultConfig.rates.irongreat_shiny = 0.05f;
                        defaultConfig.rates.irongreat_regular = 0.5f;
                        defaultConfig.rates.common_shiny = 1.0f;
                        defaultConfig.rates.jackpot = 0.001f;

                        defaultConfig.pity_milestones = new PityMilestones();
                        defaultConfig.pity_milestones.common_shiny_milestone = 200;
                        defaultConfig.pity_milestones.irongreat_shiny_milestone = 400;
                        defaultConfig.pity_milestones.mythic_shiny_milestone = 700;
                        defaultConfig.pity_milestones.ub_shiny_milestone = 850;
                        defaultConfig.pity_milestones.legendary_shiny_milestone = 1000;
                        defaultConfig.pity_milestones.jackpot_shiny_milestone = 1200;

                        defaultConfig.common = List.of("Pidgey", "Rattata", "Zubat", "Magikarp");
                        defaultConfig.legendary_shiny = List.of("Pidgey", "Rattata", "Zubat", "Magikarp");
                        defaultConfig.legendary_regular = List.of("Pidgey", "Rattata", "Zubat", "Magikarp");
                        defaultConfig.mythic_regular = List.of("Pidgey", "Rattata", "Zubat", "Magikarp");
                        defaultConfig.mythic_shiny = List.of("Pidgey", "Rattata", "Zubat", "Magikarp");
                        defaultConfig.irongreat_regular = List.of("Pidgey", "Rattata", "Zubat", "Magikarp");
                        defaultConfig.ub_regular = List.of("Pidgey", "Rattata", "Zubat", "Magikarp");
                        defaultConfig.irongreat_shiny = List.of("Pidgey", "Rattata", "Zubat", "Magikarp");
                        defaultConfig.ub_shiny = List.of("Pidgey", "Zubat", "Magikarp");

                        String json = GSON.toJson(defaultConfig);
                        Files.writeString(CONFIG_PATH, json);
                    }
                }
            }

            String content = Files.readString(CONFIG_PATH);
            RewardConfig config = GSON.fromJson(content, RewardConfig.class);

            LUCK_MULTIPLIER = config.luck_multiplier;
            RATES = config.rates;
            if (RATES == null) {
                throw new RuntimeException("The 'rates' section is missing from gacha_rewards.json!");
            }

            // Treat config rates as percentages (e.g. 0.01 = 0.01%).
            // Convert to fractions used for rolling by dividing by 100.
            RATES.legendary_shiny /= 100.0f;
            RATES.legendary_regular /= 100.0f;
            RATES.mythic_shiny /= 100.0f;
            RATES.mythic_regular /= 100.0f;
            RATES.ub_shiny /= 100.0f;
            RATES.ub_regular /= 100.0f;
            RATES.irongreat_shiny /= 100.0f;
            RATES.irongreat_regular /= 100.0f;
            RATES.jackpot /= 100.0f;
            RATES.common_shiny /= 100.0f;

            PITY_MILESTONES = config.pity_milestones;
            if (PITY_MILESTONES == null) {
                PITY_MILESTONES = new PityMilestones();
                PITY_MILESTONES.common_shiny_milestone = 200;
                PITY_MILESTONES.irongreat_shiny_milestone = 400;
                PITY_MILESTONES.mythic_shiny_milestone = 700;
                PITY_MILESTONES.ub_shiny_milestone = 850;
                PITY_MILESTONES.legendary_shiny_milestone = 1000;
                PITY_MILESTONES.jackpot_shiny_milestone = 1200;
            }

            // Support config key "jackpot_milestone" -> map into internal jackpot_shiny_milestone
            if (PITY_MILESTONES != null) {
                if (PITY_MILESTONES.jackpot_shiny_milestone == 0 && PITY_MILESTONES.jackpot_milestone != 0) {
                    PITY_MILESTONES.jackpot_shiny_milestone = PITY_MILESTONES.jackpot_milestone;
                }
            }

            LEGENDARY_SHINY_REWARDS = config.legendary_shiny;
            LEGENDARY_REGULAR_REWARDS = config.legendary_regular;
            MYTHIC_SHINY_REWARDS = config.mythic_shiny;
            MYTHIC_REGULAR_REWARDS = config.mythic_regular;
            UB_SHINY_REWARDS = config.ub_shiny;
            UB_REGULAR_REWARDS = config.ub_regular;
            IRONGREAT_REGULAR_REWARDS = config.irongreat_regular;
            IRONGREAT_SHINY_REWARDS = config.irongreat_shiny;
            JACKPOT_REWARDS = config.jackpot;
            COMMON_REWARDS = config.common;
            COMMON_SHINY_REWARDS = config.common;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load gacha_rewards.json config", e);
        }
    }

    public static double getPlayerLuck(ServerPlayerEntity player) {
        return player.getAttributeValue(EntityAttributes.GENERIC_LUCK);
    }

    private static float getLuckBonus(ServerPlayerEntity player) {
        double luck = getPlayerLuck(player);
        return (float) (luck * LUCK_MULTIPLIER);
    }

    private static float applyLuckBonus(float baseRate, float luckBonus) {
        return baseRate + (baseRate * luckBonus);
    }

    public static RewardEntry pickRandomReward(Random rand, ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        int currentPity = PlayerPityData.get(playerId);

        RewardEntry pityReward = checkPityMilestone(currentPity, rand, player);
        if (pityReward != null) {
            return pityReward;
        }

        float luckBonus = getLuckBonus(player);

        float roll = rand.nextFloat();
        float cumulativeRate = 0.0f;

        cumulativeRate += applyLuckBonus(RATES.jackpot, luckBonus);
        if (roll < cumulativeRate) {
            String name = JACKPOT_REWARDS.get(rand.nextInt(JACKPOT_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "Jackpot", name, false);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        }

        // Legendary Shiny
        cumulativeRate += applyLuckBonus(RATES.legendary_shiny, luckBonus);
        if (roll < cumulativeRate) {
            String name = LEGENDARY_SHINY_REWARDS.get(rand.nextInt(LEGENDARY_SHINY_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "Legendary Shiny", name, false);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        }

        // Legendary Regular
        cumulativeRate += applyLuckBonus(RATES.legendary_regular, luckBonus);
        if (roll < cumulativeRate) {
            String name = LEGENDARY_REGULAR_REWARDS.get(rand.nextInt(LEGENDARY_REGULAR_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "Legendary", name, false);
            return new RewardEntry(name, Map.of());
        }

        // Mythic Shiny
        cumulativeRate += applyLuckBonus(RATES.mythic_shiny, luckBonus);
        if (roll < cumulativeRate) {
            String name = MYTHIC_SHINY_REWARDS.get(rand.nextInt(MYTHIC_SHINY_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "Mythic Shiny", name, false);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        }

        // Mythic Regular
        cumulativeRate += applyLuckBonus(RATES.mythic_regular, luckBonus);
        if (roll < cumulativeRate) {
            String name = MYTHIC_REGULAR_REWARDS.get(rand.nextInt(MYTHIC_REGULAR_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "Mythic", name, false);
            return new RewardEntry(name, Map.of());
        }

        // UB Shiny
        cumulativeRate += applyLuckBonus(RATES.ub_shiny, luckBonus);
        if (roll < cumulativeRate) {
            String name = UB_SHINY_REWARDS.get(rand.nextInt(UB_SHINY_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "UB Shiny", name, false);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        }

        // UB Regular
        cumulativeRate += applyLuckBonus(RATES.ub_regular, luckBonus);
        if (roll < cumulativeRate) {
            String name = UB_REGULAR_REWARDS.get(rand.nextInt(UB_REGULAR_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "UB", name, false);
            return new RewardEntry(name, Map.of());
        }

        // Paradox Shiny
        cumulativeRate += applyLuckBonus(RATES.irongreat_shiny, luckBonus);
        if (roll < cumulativeRate) {
            String name = IRONGREAT_SHINY_REWARDS.get(rand.nextInt(IRONGREAT_SHINY_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "Paradox Shiny", name, false);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        }

        // Paradox Regular
        cumulativeRate += applyLuckBonus(RATES.irongreat_regular, luckBonus);
        if (roll < cumulativeRate) {
            String name = IRONGREAT_REGULAR_REWARDS.get(rand.nextInt(IRONGREAT_REGULAR_REWARDS.size()));
            PlayerPityData.increase(playerId);
            notifyNonCommon(player, "Paradox", name, false);
            return new RewardEntry(name, Map.of());
        }

        // Common Shiny
        cumulativeRate += applyLuckBonus(RATES.common_shiny, luckBonus);
        if (roll < cumulativeRate) {
            String name = COMMON_SHINY_REWARDS.get(rand.nextInt(COMMON_SHINY_REWARDS.size()));
            PlayerPityData.increase(playerId); // Tăng pity counter
            notifyNonCommon(player, "Common Shiny", name, false);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        }

        // Common Regular
        String name = COMMON_REWARDS.get(rand.nextInt(COMMON_REWARDS.size()));
        PlayerPityData.increase(playerId);
        return new RewardEntry(name, Map.of());
    }

    private static RewardEntry checkPityMilestone(int pityCount, Random rand, ServerPlayerEntity player) {
        if (PITY_MILESTONES == null) {
            return null;
        }

        if (PITY_MILESTONES.jackpot_shiny_milestone != 0 && pityCount == PITY_MILESTONES.jackpot_shiny_milestone) {
            String name;
            if (JACKPOT_REWARDS != null && !JACKPOT_REWARDS.isEmpty()) {
                name = JACKPOT_REWARDS.get(rand.nextInt(JACKPOT_REWARDS.size()));
            } else {
                name = LEGENDARY_SHINY_REWARDS.get(rand.nextInt(LEGENDARY_SHINY_REWARDS.size()));
            }
            PlayerPityData.reset(player.getUuid());
            notifyNonCommon(player, "JACKPOT!", name, true);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        } else if (pityCount == PITY_MILESTONES.legendary_shiny_milestone) {
            String name = LEGENDARY_SHINY_REWARDS.get(rand.nextInt(LEGENDARY_SHINY_REWARDS.size()));
            PlayerPityData.increase(player.getUuid());
            notifyNonCommon(player, "Legendary Shiny", name, true);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        } else if (pityCount == PITY_MILESTONES.ub_shiny_milestone) {
            String name = UB_SHINY_REWARDS.get(rand.nextInt(UB_SHINY_REWARDS.size()));
            PlayerPityData.increase(player.getUuid());
            notifyNonCommon(player, "UB Shiny", name, true);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        } else if (pityCount == PITY_MILESTONES.mythic_shiny_milestone) {
            String name = MYTHIC_SHINY_REWARDS.get(rand.nextInt(MYTHIC_SHINY_REWARDS.size()));
            PlayerPityData.increase(player.getUuid());
            notifyNonCommon(player, "Mythic Shiny", name, true);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        } else if (pityCount == PITY_MILESTONES.irongreat_shiny_milestone) {
            String name = IRONGREAT_SHINY_REWARDS.get(rand.nextInt(IRONGREAT_SHINY_REWARDS.size()));
            PlayerPityData.increase(player.getUuid());
            notifyNonCommon(player, "Paradox Shiny", name, true);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        } else if (pityCount == PITY_MILESTONES.common_shiny_milestone) {
            String name = COMMON_SHINY_REWARDS.get(rand.nextInt(COMMON_SHINY_REWARDS.size()));
            PlayerPityData.increase(player.getUuid());
            notifyNonCommon(player, "Common Shiny", name, true);
            return new RewardEntry(name, SPECIAL_DEFAULT_PARAMS);
        }
        return null;
    }

    /**
     * Gửi thông báo cho người chơi và toàn server khi họ nhận được phần thưởng không phải loại thường.
     *
     * @param player      Người chơi nhận thưởng
     * @param tier        Bậc của phần thưởng
     * @param name        Tên của phần thưởng
     * @param isMilestone Phần thưởng có phải từ mốc pity không
     */
    private static void notifyNonCommon(ServerPlayerEntity player, String tier, String name, boolean isMilestone) {
        if (player == null || player.getServer() == null) return;

        try {
            // 1. Thông báo cá nhân cho người chơi
            Text personalMsg = Text.literal("You won: ").formatted(Formatting.GREEN)
                    .append(Text.literal("[" + tier + "] ").formatted(Formatting.YELLOW))
                    .append(Text.literal(name).formatted(Formatting.AQUA));
            player.sendMessage(personalMsg, false);

            // 2. Xây dựng và gửi thông báo toàn server
            MutableText broadcastMessage;
            if (isMilestone) {
                // Định dạng cho phần thưởng mốc
                broadcastMessage = Text.literal("").append(Text.literal(player.getName().getString()).formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal(" has reached the ").formatted(Formatting.AQUA))
                        .append(Text.literal(tier + " milestone").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                        .append(Text.literal(" and received a ").formatted(Formatting.AQUA))
                        .append(Text.literal(name + "!").formatted(Formatting.YELLOW));
            } else {
                // Định dạng cho phần thưởng thông thường (không phải mốc)
                broadcastMessage = Text.literal("").append(Text.literal(player.getName().getString()).formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal(" has won a ").formatted(Formatting.AQUA))
                        .append(Text.literal(tier).formatted(Formatting.LIGHT_PURPLE))
                        .append(Text.literal(" reward: ").formatted(Formatting.AQUA))
                        .append(Text.literal(name + "!").formatted(Formatting.YELLOW));
            }

            // Thêm tiền tố [Gacha] vào thông báo
            MutableText finalMessage = Text.literal("[Gacha] ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                    .append(broadcastMessage);

            player.getServer().getPlayerManager().broadcast(finalMessage, false);

        } catch (Throwable ignored) {
        }
    }


    public static String getNextMilestoneInfo(int currentPity) {
        if (PITY_MILESTONES == null) {
            return "Pity system is not configured.";
        }

        if (currentPity < PITY_MILESTONES.common_shiny_milestone) {
            return "Next: Common Shiny in " + (PITY_MILESTONES.common_shiny_milestone - currentPity) + " rolls.";
        } else if (currentPity < PITY_MILESTONES.irongreat_shiny_milestone) {
            return "Next: Paradox Shiny in " + (PITY_MILESTONES.irongreat_shiny_milestone - currentPity) + " rolls.";
        } else if (currentPity < PITY_MILESTONES.mythic_shiny_milestone) {
            return "Next: Mythic Shiny in " + (PITY_MILESTONES.mythic_shiny_milestone - currentPity) + " rolls.";
        } else if (currentPity < PITY_MILESTONES.ub_shiny_milestone) {
            return "Next: UB Shiny in " + (PITY_MILESTONES.ub_shiny_milestone - currentPity) + " rolls.";
        } else if (currentPity < PITY_MILESTONES.legendary_shiny_milestone) {
            return "Next: Legendary Shiny in " + (PITY_MILESTONES.legendary_shiny_milestone - currentPity) + " rolls.";
        } else if (PITY_MILESTONES.jackpot_shiny_milestone != 0 && currentPity < PITY_MILESTONES.jackpot_shiny_milestone) {
            return "Next: Jackpot in " + (PITY_MILESTONES.jackpot_shiny_milestone - currentPity) + " rolls.";
        } else {
            return "Guaranteed Jackpot next!";
        }
    }

    public static class PityMilestones {
        int common_shiny_milestone;
        int irongreat_shiny_milestone;
        int mythic_shiny_milestone;
        int ub_shiny_milestone;
        int legendary_shiny_milestone;
        int jackpot_shiny_milestone;
        int jackpot_milestone;
    }

    public static class GachaRates {
        float jackpot;
        float legendary_shiny;
        float legendary_regular;
        float mythic_shiny;
        float mythic_regular;
        float ub_shiny;
        float ub_regular;
        float irongreat_shiny;
        float irongreat_regular;
        float common_shiny;
    }

    public static class RewardConfig {
        float luck_multiplier = 0.05f;
        GachaRates rates;
        PityMilestones pity_milestones;
        List<String> jackpot;
        List<String> legendary_shiny;
        List<String> legendary_regular;
        List<String> mythic_shiny;
        List<String> mythic_regular;
        List<String> ub_shiny;
        List<String> ub_regular;
        List<String> irongreat_shiny;
        List<String> irongreat_regular;
        List<String> common;
    }

    public static class RewardEntry {
        public final String pokemon;
        public final Map<String, String> params;

        public RewardEntry(String pokemon, Map<String, String> params) {
            this.pokemon = pokemon;
            this.params = params;
        }

        public String buildCommand(ServerPlayerEntity player) {
            StringBuilder cmd = new StringBuilder("givepokemonegg ")
                    .append(player.getName().getString()).append(" ")
                    .append(pokemon);

            params.forEach((k, v) -> cmd.append(" ").append(k).append("=").append(v));
            return cmd.toString();
        }
    }
}