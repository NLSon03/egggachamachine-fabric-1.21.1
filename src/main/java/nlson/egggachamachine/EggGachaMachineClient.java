package nlson.egggachamachine;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import nlson.egggachamachine.screen.GachaMachineScreen;
import nlson.egggachamachine.screen.ModScreenHandlers;

public class EggGachaMachineClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.GACHA_MACHINE, GachaMachineScreen::new);
    }
}
