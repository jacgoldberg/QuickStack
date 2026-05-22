package com.TickingClockWork.quickstack;

import com.TickingClockWork.quickstack.client.QuickStackConfigScreen;
import com.TickingClockWork.quickstack.network.AssignMiscChestPayload;
import com.TickingClockWork.quickstack.network.QuickStackPayload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(QuickStackMod.MOD_ID)
public class QuickStackMod {

    public static final String MOD_ID = "quickstack";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuickStackMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.COMMON, QuickStackConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (mc, parent) -> QuickStackConfigScreen.create(parent));
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MOD_ID);
        registrar.playToServer(
                QuickStackPayload.TYPE,
                QuickStackPayload.STREAM_CODEC,
                QuickStackPayload::handle
        );
        registrar.playToServer(
                AssignMiscChestPayload.TYPE,
                AssignMiscChestPayload.STREAM_CODEC,
                AssignMiscChestPayload::handle
        );
    }
}
