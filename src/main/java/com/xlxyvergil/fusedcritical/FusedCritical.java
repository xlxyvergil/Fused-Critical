package com.xlxyvergil.fusedcritical;

import com.mojang.logging.LogUtils;
import com.xlxyvergil.fusedcritical.attribute.FusedAttributes;
import com.xlxyvergil.fusedcritical.compat.curios.CuriosCompat;
import com.xlxyvergil.fusedcritical.config.ConfigSeeder;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.network.NetworkHandler;
import com.xlxyvergil.fusedcritical.normalize.CritNormalizeAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(FusedCritical.MODID)
public class FusedCritical
{
    public static final String MODID = "fusedcritical";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FusedCritical(IEventBus modEventBus, ModContainer modContainer)
    {
        // 必须在 registerConfig 之前采样：加载器读取配置时会把缺失的键补进去并保存，
        // 采晚了就永远看到"键都齐了"，首次生成/升级时的自动扫描就再也不会触发
        ConfigSeeder.captureConfigStateBeforeLoad();

        modEventBus.addListener(this::commonSetup);

        // 自定义网络包注册（NeoForge 的 payload 体系在 RegisterPayloadHandlersEvent 中完成）
        modEventBus.addListener(NetworkHandler::register);

        FusedAttributes.ATTRIBUTES.register(modEventBus);
        CritNormalizeAttachments.ATTACHMENTS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, FusedConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // 属性注册已完成（COMMON_SETUP 阶段），这时才能扫描注册表。
        // 两件事都走 enqueueWork：本事件默认在并行 worker 线程上执行，注册 game bus 监听与写配置文件都不该在那里做。
        event.enqueueWork(() -> {
            // 首次生成配置时把扫描到的暴击属性并入配置，之后永不自动改动
            ConfigSeeder.seedIfNeeded();
            CuriosCompat.tryInstall();
        });
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
    }
}
