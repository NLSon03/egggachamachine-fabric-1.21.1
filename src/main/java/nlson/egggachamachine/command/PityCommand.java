// src/main/java/nlson/egggachamachine/command/PityCommand.java

package nlson.egggachamachine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import nlson.egggachamachine.config.GachaConfigManager;
import nlson.egggachamachine.data.PlayerPityData;
import nlson.egggachamachine.data.PlayerRollData;

public class PityCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("gachapity")
                .requires(source -> source.hasPermissionLevel(2)) // Yêu cầu OP level 2

                // /gachapity check [player] - Kiểm tra pity của player
                .then(CommandManager.literal("check")
                        .executes(PityCommand::checkSelfPity) // Kiểm tra pity của chính mình
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(PityCommand::checkPlayerPity))) // Kiểm tra pity của player khác

                // /gachapity set <player> <amount> - Set pity cho player
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(PityCommand::setPityAmount))))

                // /gachapity reset [player] - Reset pity về 0
                .then(CommandManager.literal("reset")
                        .executes(PityCommand::resetSelfPity) // Reset pity của chính mình
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(PityCommand::resetPlayerPity))) // Reset pity của player khác
        );
    }

    private static int checkSelfPity(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            return showPityStatus(source, player);
        } catch (Exception e) {
            source.sendError(Text.of("Lệnh này chỉ có thể được sử dụng bởi player!"));
            return 0;
        }
    }

    private static int checkPlayerPity(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");

        return showPityStatus(source, targetPlayer);
    }

    private static int setPityAmount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        PlayerPityData.set(targetPlayer.getUuid(), amount);

        source.sendFeedback(() -> Text.of("§aĐã set pity của " + targetPlayer.getName().getString() + " thành " + amount), true);
        targetPlayer.sendMessage(Text.of("§ePity counter của bạn đã được set thành " + amount + " bởi admin"), false);

        return 1;
    }

    private static int resetSelfPity(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            PlayerPityData.reset(player);
            source.sendFeedback(() -> Text.of("§aĐã reset pity counter của bạn về 0"), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.of("Lệnh này chỉ có thể được sử dụng bởi player!"));
            return 0;
        }
    }

    private static int resetPlayerPity(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");

        PlayerPityData.reset(targetPlayer);

        source.sendFeedback(() -> Text.of("§aĐã reset pity của " + targetPlayer.getName().getString() + " về 0"), true);
        targetPlayer.sendMessage(Text.of("§ePity counter của bạn đã được reset về 0 bởi admin"), false);

        return 1;
    }

    private static int showPityStatus(ServerCommandSource source, ServerPlayerEntity player) {
        int currentPity = PlayerPityData.get(player);
        int currentRolls = PlayerRollData.get(player);
        String nextMilestone = GachaConfigManager.getNextMilestoneInfo(currentPity);

        source.sendFeedback(() -> Text.of("§6===== PITY STATUS ====="), false);
        source.sendFeedback(() -> Text.of("§ePlayer: §f" + player.getName().getString()), false);
        source.sendFeedback(() -> Text.of("§ePity Counter: §f" + currentPity), false);
        source.sendFeedback(() -> Text.of("§eGacha Coins: §f" + currentRolls), false);
        source.sendFeedback(() -> Text.of("§eNext Milestone: §f" + nextMilestone), false);

        // Hiển thị các milestone
        source.sendFeedback(() -> Text.of("§6===== MILESTONES ====="), false);
        source.sendFeedback(() -> Text.of("§a200 lần: §fCommon Shiny"), false);
        source.sendFeedback(() -> Text.of("§7400 lần: §fIronGreat Shiny"), false);
        source.sendFeedback(() -> Text.of("§d700 lần: §fMythic Shiny"), false);
        source.sendFeedback(() -> Text.of("§5850 lần: §fUB Shiny"), false);
        source.sendFeedback(() -> Text.of("§61000 lần: §fLegendary Shiny"), false);
        source.sendFeedback(() -> Text.of("§71200 lần: §fLegendary Shiny"), false);
        return 1;
    }
}