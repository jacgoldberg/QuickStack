package com.TickingClockWork.quickstack.client;

import com.TickingClockWork.quickstack.QuickStackConfig;
import com.TickingClockWork.quickstack.QuickStackMod;
import com.TickingClockWork.quickstack.network.QuickStackPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = QuickStackMod.MOD_ID, value = Dist.CLIENT)
public class QuickStackButton {

    private static final ResourceLocation BUTTON_SPRITE = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        int x = screen.getGuiLeft() + QuickStackConfig.BUTTON_OFFSET_X.get();
        int y = screen.getGuiTop() + QuickStackConfig.BUTTON_OFFSET_Y.get();

        event.addListener(new Button(x, y, 20, 18, Component.empty(),
                btn -> {
                    boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
                    PacketDistributor.sendToServer(new QuickStackPayload(shift));
                },
                btn -> Component.translatable("button.quickstack.quick_stack")) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation sprite = isHovered() ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE;
                guiGraphics.blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, "QS", getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, 0xFFFFFF);
                if (isHovered()) {
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("button.quickstack.quick_stack"), mouseX, mouseY);
                }
            }
        });
    }
}
