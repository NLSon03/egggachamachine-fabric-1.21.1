package nlson.egggachamachine.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import nlson.egggachamachine.network.ModMessages;

public class GachaMachineScreen extends HandledScreen<GachaMachineScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("egggachamachine", "textures/gui/gacha_machine.png");

    public GachaMachineScreen(GachaMachineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 184; // Đảm bảo đủ không gian
    }

    @Override
    protected void init() {
        super.init();

        // Nút "Top-up"
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Top-up").setStyle(Style.EMPTY.withColor(0x55FF55).withBold(true)), // Màu xanh lá
                        btn -> {
                            int coins = handler.getInventory().getStack(0).getCount();
                            if (coins > 0) {
                                // Only send the request to server. Server will remove the coins from the
                                // server-side screen handler/inventory and then sync the screen back to client.
                                ClientPlayNetworking.send(new ModMessages.NapTienPayload(coins));
                            }
                        })
                .dimensions(this.x + 20, this.y + 60, 70, 24) // Đặt giữa slot và inventory
                .tooltip(Tooltip.of(Text.literal("Add coins to play Gacha!").setStyle(Style.EMPTY.withColor(0xFFFFFF))))
                .build());

        // Nút "Roll"
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Roll").setStyle(Style.EMPTY.withColor(0xFF5555).withBold(true)), // Màu đỏ
                        btn -> ClientPlayNetworking.send(new ModMessages.QuayPayload()))
                .dimensions(this.x + 100, this.y + 60, 70, 24) // Đặt giữa slot và inventory
                .tooltip(Tooltip.of(Text.literal("Roll for rewards!").setStyle(Style.EMPTY.withColor(0xFFFFFF))))
                .build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        for (Slot slot : this.handler.slots) {
            int slotX = this.x + slot.x;
            int slotY = this.y + slot.y;
            context.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF2A2A2A); // Xám tối
            if (isPointWithinBounds(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFF99); // Vàng nhạt khi hover
            }
            context.drawBorder(slotX, slotY, 16, 16, 0xFFD4D4D4); // Viền sáng
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Vẽ tiêu đề chính của Gacha Machine
        context.drawText(
                this.textRenderer,
                this.title,
                this.titleX, // Thường là 8
                this.titleY, // Thường là 6
                0x404040, // Màu xám đậm
                true // Thêm bóng
        );

        // Vẽ tiêu đề inventory của người chơi
        context.drawText(
                this.textRenderer,
                this.playerInventoryTitle,
                this.playerInventoryTitleX, // Thường là 8
                92, // Đặt sát với inventory (102 - 10)
                0x404040, // Màu xám đậm
                true // Thêm bóng
        );

        // Vẽ text "Pity" và "Rolls" ở phía trên, tránh chồng lấn với buttons
        context.drawText(
                this.textRenderer,
                Text.literal("Pity: " + handler.getPity()).setStyle(Style.EMPTY.withColor(0xFFAA00).withBold(true)), // Màu cam
                20, // Căn trái với button Top-up
                35, // Đặt ở giữa title và buttons
                0xFFFFFF,
                true // Thêm bóng
        );

        context.drawText(
                this.textRenderer,
                Text.literal("Rolls: " + handler.getRolls()).setStyle(Style.EMPTY.withColor(0x00AAFF).withBold(true)), // Màu xanh dương
                100, // Căn trái với button Roll
                35, // Cùng hàng với Pity
                0xFFFFFF,
                true // Thêm bóng
        );
    }
}