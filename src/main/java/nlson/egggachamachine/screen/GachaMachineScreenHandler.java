package nlson.egggachamachine.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import nlson.egggachamachine.item.ModItems;

public class GachaMachineScreenHandler extends ScreenHandler {
    private final SimpleInventory inventory = new SimpleInventory(1);
    private final PropertyDelegate data;

    public GachaMachineScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new ArrayPropertyDelegate(2));
    }

    public GachaMachineScreenHandler(int syncId, PlayerInventory playerInventory, PropertyDelegate data) {
        super(ModScreenHandlers.GACHA_MACHINE, syncId);

        this.addSlot(new Slot(inventory, 0, 80, 35) { // Dịch slot sang phải
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.GACHA_COIN);
            }
        });

        // player inventory
        int m, l;
        for (m = 0; m < 3; ++m) {
            for (l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 102 + m * 18)); // Dịch xuống dưới
            }
        }

        // hotbar
        for (m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 160)); // Dịch xuống dưới
        }

        this.data = data;
        this.addProperties(this.data);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    public SimpleInventory getInventory() {
        return inventory;
    }

    public int getPity() {
        return data.get(0);
    }

    public int getRolls() {
        return data.get(1);
    }

    public void setValues(int pity, int rolls) {
        data.set(0, pity);
        data.set(1, rolls);
        this.sendContentUpdates();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (index == 0) {
                if (!this.insertItem(originalStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.isOf(ModItems.GACHA_COIN)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        if (index < 28) {
                            if (!this.insertItem(originalStack, 28, 37, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            if (!this.insertItem(originalStack, 1, 28, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                } else if (index < 28) {
                    if (!this.insertItem(originalStack, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.insertItem(originalStack, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }
}