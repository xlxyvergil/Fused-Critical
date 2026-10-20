package com.xlxyvergil.fusedcritical.compat.l2tabs;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;

// 面板条目现在由 l2tabs:attribute_entry 数据表驱动（每次取用都现读），所以只需在数据表重建后按配置注入一次，
// 不再需要 1.20.1 时代的「每次打开界面刷新列表」钩子。改配置后 /reload 即生效。
@EventBusSubscriber(modid = FusedCritical.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class L2TabsCritDisplayEvents {

    private L2TabsCritDisplayEvents() {}

    @SubscribeEvent
    public static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
        L2TabsCompat.onDataMapsUpdated(event);
    }
}
