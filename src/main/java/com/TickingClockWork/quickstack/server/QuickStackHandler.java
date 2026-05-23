package com.TickingClockWork.quickstack.server;

import com.TickingClockWork.quickstack.QuickStackConfig;
import com.TickingClockWork.quickstack.QuickStackMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = QuickStackMod.MOD_ID)
public class QuickStackHandler {

    private static final int FLIGHT_TICKS = 30;
    private static final String MISC_CHEST_KEY = "quickstack_misc_chest";

    private record OpenChest(ServerLevel level, BlockPos pos, List<Integer> entityIds, long closeAt) {}

    private static final List<OpenChest> OPEN_CHESTS = new ArrayList<>();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (OPEN_CHESTS.isEmpty()) return;

        long now = level.getGameTime();

        Iterator<OpenChest> it = OPEN_CHESTS.iterator();
        while (it.hasNext()) {
            OpenChest oc = it.next();
            if (oc.level() != level) continue;
            if (now < oc.closeAt()) continue;

            for (int id : oc.entityIds()) {
                net.minecraft.world.entity.Entity e = level.getEntity(id);
                if (e != null) e.discard();
            }
            level.blockEvent(oc.pos(), level.getBlockState(oc.pos()).getBlock(), 1, 0);
            it.remove();
        }
    }

    public static void assignMiscChest(ServerPlayer player, BlockPos pos) {
        if (!isApprovedInventory((ServerLevel) player.level(), pos)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.quickstack.misc_chest_invalid"),
                    true
            );
            return;
        }
        CompoundTag data = player.getPersistentData();
        ListTag list = data.contains(MISC_CHEST_KEY, Tag.TAG_LIST)
                ? data.getList(MISC_CHEST_KEY, Tag.TAG_COMPOUND)
                : new ListTag();

        // If already assigned, remove it (toggle off)
        for (int i = 0; i < list.size(); i++) {
            if (NbtUtils.readBlockPos(list.getCompound(i), "pos").orElse(BlockPos.ZERO).equals(pos)) {
                list.remove(i);
                data.put(MISC_CHEST_KEY, list);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.quickstack.misc_chest_removed")
                                .withStyle(net.minecraft.ChatFormatting.RED),
                        true
                );
                return;
            }
        }

        CompoundTag entry = new CompoundTag();
        entry.put("pos", NbtUtils.writeBlockPos(pos));
        list.add(entry);
        data.put(MISC_CHEST_KEY, list);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.quickstack.misc_chest_assigned")
                        .withStyle(net.minecraft.ChatFormatting.GREEN),
                true
        );
    }

    public static List<BlockPos> getMiscChests(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(MISC_CHEST_KEY, Tag.TAG_LIST)) return List.of();
        ListTag list = data.getList(MISC_CHEST_KEY, Tag.TAG_COMPOUND);
        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            NbtUtils.readBlockPos(list.getCompound(i), "pos").ifPresent(result::add);
        }
        return result;
    }

    public static void quickStack(ServerPlayer player, boolean shift) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        Inventory playerInventory = player.getInventory();
        Set<IItemHandler> processedHandlers = new HashSet<>();
        List<BlockPos> miscChests = getMiscChests(player);
        Set<BlockPos> miscChestSet = new HashSet<>(miscChests);

        int radius = QuickStackConfig.RADIUS.get();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius)
        )) {
            if (miscChestSet.contains(pos)) continue;

            IItemHandler handler = getHandler(level, pos);
            if (handler == null) continue;
            if (handler.getSlots() < QuickStackConfig.MIN_SLOTS.get()) continue;
            if (processedHandlers.contains(handler)) continue;
            processedHandlers.add(handler);

            for (int playerSlot = 9; playerSlot < 36; playerSlot++) {
                ItemStack playerStack = playerInventory.getItem(playerSlot);
                if (playerStack.isEmpty()) continue;
                if (!handlerContainsMatchingItem(handler, playerStack)) continue;

                ItemStack visualStack = playerStack.copy();
                visualStack.setCount(1);

                int moved = insertMatchingItem(handler, playerStack);

                if (moved > 0) {
                    playerInventory.setItem(playerSlot, playerStack);
                    notifyBlockEntity(level, pos);
                    spawnFlyingItem(level, player, pos.immutable(), visualStack);
                }
            }
        }

        // Shift mode: dump remaining non-empty slots into the first misc inventory in range
        if (shift && !miscChests.isEmpty()) {
            for (BlockPos miscPos : miscChests) {
                if (center.distSqr(miscPos) > (long) radius * radius) continue;

                IItemHandler miscHandler = getHandler(level, miscPos);
                if (miscHandler == null) continue;

                for (int playerSlot = 9; playerSlot < 36; playerSlot++) {
                    ItemStack playerStack = playerInventory.getItem(playerSlot);
                    if (playerStack.isEmpty()) continue;

                    ItemStack visualStack = playerStack.copy();
                    visualStack.setCount(1);

                    int moved = insertAnyItem(miscHandler, playerStack);
                    if (moved > 0) {
                        playerInventory.setItem(playerSlot, playerStack);
                        notifyBlockEntity(level, miscPos);
                        spawnFlyingItem(level, player, miscPos.immutable(), visualStack);
                    }
                }
                break; // only use the first misc inventory in range
            }
        }

        playerInventory.setChanged();
    }

    private static IItemHandler getHandler(ServerLevel level, BlockPos pos) {
        // Prefer null (face-agnostic) — gives full unrestricted access
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler != null) return handler;
        // Fall back to faces for mods that only register per-face handlers
        for (Direction dir : Direction.values()) {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            if (handler != null) return handler;
        }
        return null;
    }

    private static void spawnFlyingItem(ServerLevel level, ServerPlayer player, BlockPos targetPos, ItemStack stack) {
        Vec3 start = player.position().add(0, 1.2, 0);
        Vec3 end = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);

        double vx = (end.x - start.x) / FLIGHT_TICKS;
        double vy = (end.y - start.y) / FLIGHT_TICKS;
        double vz = (end.z - start.z) / FLIGHT_TICKS;

        ItemEntity itemEntity = new ItemEntity(level, start.x, start.y, start.z, stack, vx, vy, vz);
        itemEntity.setNoGravity(true);
        itemEntity.setPickUpDelay(FLIGHT_TICKS);
        level.addFreshEntity(itemEntity);

        long closeAt = level.getGameTime() + FLIGHT_TICKS;

        for (int i = 0; i < OPEN_CHESTS.size(); i++) {
            OpenChest oc = OPEN_CHESTS.get(i);
            if (oc.level() == level && oc.pos().equals(targetPos)) {
                oc.entityIds().add(itemEntity.getId());
                if (closeAt > oc.closeAt()) {
                    OPEN_CHESTS.set(i, new OpenChest(level, targetPos, oc.entityIds(), closeAt));
                }
                return;
            }
        }

        level.blockEvent(targetPos, level.getBlockState(targetPos).getBlock(), 1, 1);
        List<Integer> ids = new ArrayList<>();
        ids.add(itemEntity.getId());
        OPEN_CHESTS.add(new OpenChest(level, targetPos, ids, closeAt));
    }

    private static boolean handlerContainsMatchingItem(IItemHandler handler, ItemStack playerStack) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(playerStack, stack)) return true;
        }
        return false;
    }

    private static int insertMatchingItem(IItemHandler handler, ItemStack playerStack) {
        int movedTotal = 0;

        // Fill partial stacks of matching items first
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (playerStack.isEmpty()) return movedTotal;
            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(playerStack, existing)) continue;

            ItemStack toInsert = playerStack.copy();
            ItemStack remainder = handler.insertItem(slot, toInsert, false);
            int moved = toInsert.getCount() - remainder.getCount();
            playerStack.shrink(moved);
            movedTotal += moved;
        }

        // Then fill empty slots
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (playerStack.isEmpty()) return movedTotal;
            if (!handler.getStackInSlot(slot).isEmpty()) continue;

            ItemStack toInsert = playerStack.copy();
            ItemStack remainder = handler.insertItem(slot, toInsert, false);
            int moved = toInsert.getCount() - remainder.getCount();
            playerStack.shrink(moved);
            movedTotal += moved;
        }

        return movedTotal;
    }

    private static int insertAnyItem(IItemHandler handler, ItemStack playerStack) {
        int movedTotal = 0;

        // Fill partial stacks first
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (playerStack.isEmpty()) return movedTotal;
            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(playerStack, existing)) continue;

            ItemStack toInsert = playerStack.copy();
            ItemStack remainder = handler.insertItem(slot, toInsert, false);
            int moved = toInsert.getCount() - remainder.getCount();
            playerStack.shrink(moved);
            movedTotal += moved;
        }

        // Then empty slots
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (playerStack.isEmpty()) return movedTotal;
            if (!handler.getStackInSlot(slot).isEmpty()) continue;

            ItemStack toInsert = playerStack.copy();
            ItemStack remainder = handler.insertItem(slot, toInsert, false);
            int moved = toInsert.getCount() - remainder.getCount();
            playerStack.shrink(moved);
            movedTotal += moved;
        }

        return movedTotal;
    }

    private static void notifyBlockEntity(ServerLevel level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be != null) {
            be.setChanged();
            var state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 2 | 4);
        }
    }

    private static boolean isApprovedInventory(ServerLevel level, BlockPos pos) {
        IItemHandler handler = getHandler(level, pos);
        if (handler == null) return false;
        return handler.getSlots() >= QuickStackConfig.MIN_SLOTS.get();
    }
}
