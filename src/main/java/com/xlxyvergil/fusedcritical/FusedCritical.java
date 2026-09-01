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

// 值需与 META-INF/mods.toml 里的 mod id 一致
@Mod(FusedCritical.MODID)
public class FusedCritical
{
    // 统一的 mod id，大家都引用这里
    public static final String MODID = "fusedcritical";
    // slf4j 日志器
    private static final Logger LOGGER = LogUtils.getLogger();

    public FusedCritical(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // 加载流程里注册 commonSetup
        modEventBus.addListener(this::commonSetup);

        FusedAttributes.ATTRIBUTES.register(modEventBus);

        // 把自己注册到 Forge 事件总线，接收游戏内事件
        MinecraftForge.EVENT_BUS.register(this);

        // 注册本 mod 的配置，让 Forge 生成并加载配置文件
        context.registerConfig(ModConfig.Type.COMMON, FusedConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // 注册网络通道（建通道 + 注册所有消息）
        NetworkHandler.register();

        CuriosCompat.tryInstall();

        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // 用 SubscribeEvent 让事件总线发现并调用标记的方法
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // 服务端启动时做点事
        LOGGER.info("HELLO from server starting");
    }
}
