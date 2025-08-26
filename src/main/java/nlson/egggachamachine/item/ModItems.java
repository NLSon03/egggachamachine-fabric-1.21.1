package nlson.egggachamachine.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import nlson.egggachamachine.EggGachaMachine;

public class ModItems {
    public static final Item GACHA_COIN = new Item(new Item.Settings());

    public static void registerItems() {
        Registry.register(
                Registries.ITEM,
                Identifier.of(EggGachaMachine.MOD_ID, "gacha_coin"),
                GACHA_COIN
        );
    }

    public static void registerModItems() {
        EggGachaMachine.LOGGER.info("Registering ModItems for " + EggGachaMachine.MOD_ID);
        registerItems();
    }
}
