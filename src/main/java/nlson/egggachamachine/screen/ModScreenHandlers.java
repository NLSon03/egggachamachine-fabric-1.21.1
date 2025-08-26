// src/main/java/nlson/egggachamachine/screen/ModScreenHandlers.java

package nlson.egggachamachine.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static void register() {
    }    public static final ScreenHandlerType<GachaMachineScreenHandler> GACHA_MACHINE =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of("egggachamachine", "egg_gacha_machine_block"),
                    new ScreenHandlerType<>(GachaMachineScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
            );


}
