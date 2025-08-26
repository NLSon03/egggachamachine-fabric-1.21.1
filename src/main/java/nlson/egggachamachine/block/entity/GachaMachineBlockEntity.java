package nlson.egggachamachine.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import nlson.egggachamachine.data.PlayerPityData;
import nlson.egggachamachine.data.PlayerRollData;
import nlson.egggachamachine.screen.GachaMachineScreenHandler;

public class GachaMachineBlockEntity extends BlockEntity {
    private final PropertyDelegate propertyDelegate;

    public GachaMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EGG_GACHA_MACHINE_BLOCK, pos, state);

        this.propertyDelegate = new PropertyDelegate() {
            private int pity = 0;
            private int rolls = 0;

            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> pity;
                    case 1 -> rolls;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> pity = value;
                    case 1 -> rolls = value;
                }
            }

            @Override
            public int size() {
                return 2;
            }
        };
    }

    public void updatePlayerData(PlayerEntity player) {
        this.propertyDelegate.set(0, PlayerPityData.get(player));
        this.propertyDelegate.set(1, PlayerRollData.get(player));
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        updatePlayerData(player);
        return new GachaMachineScreenHandler(syncId, playerInventory, this.propertyDelegate);
    }
}