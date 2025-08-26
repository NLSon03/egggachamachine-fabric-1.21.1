package nlson.egggachamachine;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import nlson.egggachamachine.block.ModBlocks;
import nlson.egggachamachine.block.entity.ModBlockEntities;
import nlson.egggachamachine.command.PityCommand;
import nlson.egggachamachine.config.GachaConfigManager;
import nlson.egggachamachine.data.PlayerDataStorage;
import nlson.egggachamachine.data.PlayerPityData;
import nlson.egggachamachine.data.PlayerRollData;
import nlson.egggachamachine.item.ModItems;
import nlson.egggachamachine.network.ModMessages;
import nlson.egggachamachine.screen.ModScreenHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EggGachaMachine implements ModInitializer {
    public static final String MOD_ID = "egggachamachine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModBlockEntities.register();
        // Load gacha config first so RATES and reward lists are initialized before any rolls
        GachaConfigManager.load();
        ModMessages.registerC2SPackets();
        ModScreenHandlers.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PityCommand.register(dispatcher);
        });

        // Load persisted player data (if any)
        PlayerDataStorage.loadAll(PlayerPityData.getAll(), PlayerRollData.getAll());

        // Save when the server is stopping to ensure data persists on world/server shutdown
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // Ensure immediate blocking flush of player data on server stop
            ModMessages.flushNowBlocking();
        });
        LOGGER.info("Egg Gacha Machine mod initialized successfully!");
    }
}
