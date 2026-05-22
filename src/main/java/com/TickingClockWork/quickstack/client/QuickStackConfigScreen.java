package com.TickingClockWork.quickstack.client;

import com.TickingClockWork.quickstack.QuickStackConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class QuickStackConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.quickstack.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.quickstack.category.general"));

        general.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.quickstack.radius"), QuickStackConfig.RADIUS.get(), 1, 16)
                .setDefaultValue(8)
                .setTooltip(Component.translatable("config.quickstack.radius.tooltip"))
                .setSaveConsumer(val -> QuickStackConfig.RADIUS.set(val))
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.quickstack.min_slots"), QuickStackConfig.MIN_SLOTS.get(), 1, 54)
                .setDefaultValue(9)
                .setTooltip(Component.translatable("config.quickstack.min_slots.tooltip"))
                .setSaveConsumer(val -> QuickStackConfig.MIN_SLOTS.set(val))
                .build());

        ConfigCategory button = builder.getOrCreateCategory(Component.translatable("config.quickstack.category.button"));

        button.addEntry(entryBuilder
                .startIntField(Component.translatable("config.quickstack.button_offset_x"), QuickStackConfig.BUTTON_OFFSET_X.get())
                .setDefaultValue(176)
                .setTooltip(Component.translatable("config.quickstack.button_offset_x.tooltip"))
                .setSaveConsumer(val -> QuickStackConfig.BUTTON_OFFSET_X.set(val))
                .build());

        button.addEntry(entryBuilder
                .startIntField(Component.translatable("config.quickstack.button_offset_y"), QuickStackConfig.BUTTON_OFFSET_Y.get())
                .setDefaultValue(120)
                .setTooltip(Component.translatable("config.quickstack.button_offset_y.tooltip"))
                .setSaveConsumer(val -> QuickStackConfig.BUTTON_OFFSET_Y.set(val))
                .build());

        return builder.build();
    }
}
