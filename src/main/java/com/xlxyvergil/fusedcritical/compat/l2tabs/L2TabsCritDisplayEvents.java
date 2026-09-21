package com.xlxyvergil.fusedcritical.compat.l2tabs;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 面板每帧才从 AttributeDisplayConfig 取列表，所以只要每次界面打开时按配置把条目刷新好即可。
// 带 value = Dist.CLIENT，专用服务器不会加载本类。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class L2TabsCritDisplayEvents {

    private L2TabsCritDisplayEvents() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        L2TabsCompat.tryApplyCritDisplay();
    }
}
