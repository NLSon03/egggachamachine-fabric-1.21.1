package nlson.egggachamachine.block.entity;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import nlson.egggachamachine.block.ModBlocks;

public class ModBlockEntities {
    public static BlockEntityType<GachaMachineBlockEntity> EGG_GACHA_MACHINE_BLOCK;

    public static void register() {
        EGG_GACHA_MACHINE_BLOCK = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("egggachamachine", "gacha_machine"),
                BlockEntityType.Builder.create(GachaMachineBlockEntity::new, ModBlocks.EGG_GACHA_MACHINE_BLOCK).build(null)
        );
    }
}