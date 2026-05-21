package com.TickingClockWork.quickstack.network;

import com.TickingClockWork.quickstack.QuickStackMod;
import com.TickingClockWork.quickstack.server.QuickStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssignMiscChestPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<AssignMiscChestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuickStackMod.MOD_ID, "assign_misc_chest"));

    public static final StreamCodec<FriendlyByteBuf, AssignMiscChestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AssignMiscChestPayload::pos,
                    AssignMiscChestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssignMiscChestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                QuickStackHandler.assignMiscChest(player, payload.pos());
            }
        });
    }
}
