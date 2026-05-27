package com.TickingClockWork.quickstack.compat;

import com.TickingClockWork.quickstack.QuickStackConfig;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class SableCompat {

    public record SubLevelInventory(IItemHandler handler, BlockPos worldPos, ServerLevelPlot plot, BlockPos localPos, net.minecraft.world.level.Level beLevel) {}

    public static void fireChestEvent(SubLevelInventory sli, int value) {
        Block block = sli.beLevel().getBlockState(sli.localPos()).getBlock();
        sli.beLevel().blockEvent(sli.localPos(), block, 1, value);
    }

    public static List<SubLevelInventory> findNearbyHandlers(ServerLevel level, BlockPos center, int radius) {
        List<SubLevelInventory> results = new ArrayList<>();

        var container = SubLevelContainer.getContainer(level);
        if (container == null) return results;

        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (!(subLevel instanceof ServerSubLevel serverSubLevel)) continue;

            var plot = serverSubLevel.getPlot();
            if (plot == null) continue;

            var pose = serverSubLevel.logicalPose();

            for (var chunkHolder : plot.getLoadedChunks()) {
                var chunk = chunkHolder.getChunk();
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos localPos = be.getBlockPos();

                    // Transform local sub-level position to world space
                    Vector3d world = pose.transformPosition(
                            new Vector3d(localPos.getX() + 0.5, localPos.getY() + 0.5, localPos.getZ() + 0.5),
                            new Vector3d()
                    );

                    // Distance check
                    double dx = world.x - center.getX();
                    double dy = world.y - center.getY();
                    double dz = world.z - center.getZ();
                    if (Math.sqrt(dx * dx + dy * dy + dz * dz) > radius) continue;

                    // Try all faces for IItemHandler
                    IItemHandler handler = null;
                    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                        handler = be.getLevel() != null
                                ? be.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, localPos, dir)
                                : null;
                        if (handler != null) break;
                    }
                    if (handler == null) continue;
                    if (handler.getSlots() < QuickStackConfig.MIN_SLOTS.get()) continue;

                    BlockPos approxWorldPos = BlockPos.containing(world.x, world.y, world.z);
                    results.add(new SubLevelInventory(handler, approxWorldPos, plot, localPos, be.getLevel()));
                }
            }
        }

        return results;
    }
}
