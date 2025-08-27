package nlson.egggachamachine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import nlson.egggachamachine.config.GachaConfigManager;

public class LuckCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("checkluck")
                .executes(context -> checkOwnLuck(context))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> checkPlayerLuck(context))));
    }

    private static int checkOwnLuck(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            return showLuckInfo(context, player);
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("This command can only be used by players!"));
            return 0;
        }
    }

    private static int checkPlayerLuck(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            return showLuckInfo(context, target);
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Player not found!"));
            return 0;
        }
    }

    private static int showLuckInfo(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        String luckInfo = String.valueOf(GachaConfigManager.getPlayerLuck(player));

        Text message = Text.literal("🍀 ").formatted(Formatting.GREEN)
                .append(Text.literal(player.getName().getString()).formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal("'s ").formatted(Formatting.WHITE))
                .append(Text.literal(luckInfo).formatted(Formatting.AQUA));

        context.getSource().sendFeedback(() -> message, false);
        return 1;
    }
}