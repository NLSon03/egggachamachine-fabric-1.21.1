package nlson.egggachamachine.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import nlson.egggachamachine.EggGachaMachine;

public class ModBlocks {
    public static final Block EGG_GACHA_MACHINE_BLOCK =
            new EggGachaMachineBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
                    .strength(1.5f).requiresTool().nonOpaque());

    private static void registerBlocks() {
        Registry.register(
                Registries.BLOCK,
                Identifier.of("egggachamachine", "egg_gacha_machine_block"),
                EGG_GACHA_MACHINE_BLOCK
        );

        Registry.register(
                Registries.ITEM,
                Identifier.of("egggachamachine", "egg_gacha_machine_block"),
                new BlockItem(EGG_GACHA_MACHINE_BLOCK, new Item.Settings())
        );
    }

    public static void registerModBlocks() {
        EggGachaMachine.LOGGER.info("Registering ModBlocks for " + EggGachaMachine.MOD_ID);
        registerBlocks();
    }
}
