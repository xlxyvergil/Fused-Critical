package com.xlxyvergil.fusedcritical;

import com.mojang.logging.LogUtils;
import com.xlxyvergil.fusedcritical.attribute.FusedAttributes;
import com.xlxyvergil.fusedcritical.compat.curios.CuriosCompat;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FusedCritical.MODID)
public class FusedCritical
{
    public static final String MODID = "fusedcritical";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FusedCritical(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        FusedAttributes.ATTRIBUTES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, FusedConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        NetworkHandler.register();
        CuriosCompat.tryInstall();
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
    }
}
