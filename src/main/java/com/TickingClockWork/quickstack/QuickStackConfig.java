package com.TickingClockWork.quickstack;

import net.neoforged.neoforge.common.ModConfigSpec;

public class QuickStackConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RADIUS;
    public static final ModConfigSpec.IntValue BUTTON_OFFSET_X;
    public static final ModConfigSpec.IntValue BUTTON_OFFSET_Y;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        RADIUS = builder
                .comment("Radius in blocks to scan for nearby containers (1-16). Larger values scan more blocks per keypress.")
                .defineInRange("radius", 8, 1, 16);

        builder.comment("Position of the Quick Stack button in the player inventory screen, relative to the inventory background's top-left corner.");

        BUTTON_OFFSET_X = builder
                .comment("Horizontal offset in pixels from the left edge of the inventory background.")
                .defineInRange("button_offset_x", 176, -512, 512);

        BUTTON_OFFSET_Y = builder
                .comment("Vertical offset in pixels from the top edge of the inventory background.")
                .defineInRange("button_offset_y", 120, -512, 512);

        SPEC = builder.build();
    }
}
