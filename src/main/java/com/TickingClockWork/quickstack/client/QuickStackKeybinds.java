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

    public static final KeyMapping QUICK_STACK_DUMP = new KeyMapping(
            "key.quickstack.quick_stack_dump",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            "key.category.quickstack"
    );

    public static boolean isKeyHeld = false;
    private static boolean assignedThisPress = false;

    public static boolean isDumpModifierHeld() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Key key = QUICK_STACK_DUMP.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        }
        return false;
    }

    @EventBusSubscriber(modid = QuickStackMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(QUICK_STACK);
            event.register(QUICK_STACK_DUMP);
        }
    }

    @EventBusSubscriber(modid = QuickStackMod.MOD_ID, value = Dist.CLIENT)
    public static class GameEvents {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (Minecraft.getInstance().screen != null) return;
            int key = QUICK_STACK.getKey().getValue();
            if (event.getKey() != key) return;

            if (event.getAction() == GLFW.GLFW_PRESS) {
                isKeyHeld = true;
                assignedThisPress = false;
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                isKeyHeld = false;
                if (!assignedThisPress) {
                    boolean dump = isDumpModifierHeld();
                    PacketDistributor.sendToServer(new QuickStackPayload(dump));
                }
                assignedThisPress = false;
            }
        }

        @SubscribeEvent
        public static void onMouseInput(InputEvent.MouseButton.Pre event) {
            if (!isKeyHeld) return;
            if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
            if (event.getAction() != GLFW.GLFW_PRESS) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
            PacketDistributor.sendToServer(new AssignMiscChestPayload(pos));
            assignedThisPress = true;
            event.setCanceled(true);
        }
    }
}
