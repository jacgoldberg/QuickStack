package com.TickingClockWork.quickstack.client;

import com.TickingClockWork.quickstack.QuickStackMod;
import com.TickingClockWork.quickstack.network.QuickStackPayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
        public static void onClientTick(ClientTickEvent.Post event) {
            while (QUICK_STACK.consumeClick()) {
                PacketDistributor.sendToServer(new QuickStackPayload());
            }
        }
    }
}
