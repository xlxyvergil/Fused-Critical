package com.xlxyvergil.fusedcritical.compat.l2tabs;

import net.minecraft.core.registries.Registries;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;

// L2 Tabs 可选依赖守卫：只用 ModList.isLoaded 判断，不直接引用 L2 Tabs 类型。
// 引用 l2tabs:attribute_entry 数据表的面板改写隔离在 L2TabsCritDisplay，仅在确认 L2 Tabs 存在时调用，避免缺失时抛 NoClassDefFoundError。
public final class L2TabsCompat {

    public static final String MODID = "l2tabs";

    private L2TabsCompat() {}

    // L2 Tabs 是否已安装
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    // 数据包重载后属性数据表刚重建完成，此时按配置把面板里的暴击两行改成归一化接收方属性
    public static void onDataMapsUpdated(final DataMapsUpdatedEvent event) {
        if (!isLoaded()) {
            return;
        }
        // 只在服务端重载时注入：客户端同步下来的数据表是不可变包装，写不进去；
        // 单机下客户端与服务端共用同一套内置注册表，服务端注入后客户端直接可见。
        if (event.getCause() != DataMapsUpdatedEvent.UpdateCause.SERVER_RELOAD) {
            return;
        }
        event.ifRegistry(Registries.ATTRIBUTE, L2TabsCritDisplay::apply);
    }
}
