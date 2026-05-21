package com.TickingClockWork.quickstack.client;

import com.TickingClockWork.quickstack.QuickStackMod;
import com.TickingClockWork.quickstack.network.AssignMiscChestPayload;
import com.TickingClockWork.quickstack.network.QuickStackPayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

public class QuickStackKeybinds {

    public static final KeyMapping QUICK_STACK = new KeyMapping(
            "key.quickstack.quick_stack",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.category.quickstack"
    );

    // True while the key is physically held down
    public static boolean isKeyHeld = false;
    // True if a right-click assignment happened during this keypress — suppresses quick stack on release
    private static boolean assignedThisPress = false;

    @EventBusSubscriber(modid = QuickStackMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(QUICK_STACK);
        }
    }

    @EventBusSubscriber(modid = QuickStackMod.MOD_ID, value = Dist.CLIENT)
    public static class GameEvents {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            int key = QUICK_STACK.getKey().getValue();
            if (event.getKey() != key) return;

            if (event.getAction() == GLFW.GLFW_PRESS) {
                isKeyHeld = true;
                assignedThisPress = false;
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                isKeyHeld = false;
                if (!assignedThisPress) {
                    boolean shift = InputConstants.isKeyDown(
                            Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                            || InputConstants.isKeyDown(
                            Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                    PacketDistributor.sendToServer(new QuickStackPayload(shift));
                }
                assignedThisPress = false;
            }
        }

        @SubscribeEvent
        public static void onMouseInput(InputEvent.MouseButton.Pre event) {
            // Right-click while V is held — assign misc chest
            if (!isKeyHeld) return;
            if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
            if (event.getAction() != GLFW.GLFW_PRESS) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
            PacketDistributor.sendToServer(new AssignMiscChestPayload(pos));
            assignedThisPress = true;
            event.setCanceled(true); // prevent the normal right-click interaction
        }
    }
}
