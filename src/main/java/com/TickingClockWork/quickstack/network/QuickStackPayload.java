package com.TickingClockWork.quickstack.network;

import com.TickingClockWork.quickstack.QuickStackMod;
import com.TickingClockWork.quickstack.server.QuickStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuickStackPayload() implements CustomPacketPayload {

    public static final Type<QuickStackPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuickStackMod.MOD_ID, "quick_stack"));

    public static final StreamCodec<FriendlyByteBuf, QuickStackPayload> STREAM_CODEC =
            StreamCodec.unit(new QuickStackPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuickStackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                int moved = QuickStackHandler.quickStack(player);
                System.out.println("Quick stacked " + moved + " items.");
            }
        });
    }
}
