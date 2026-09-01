package com.xlxyvergil.fusedcritical.compat.curios;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

// Curios 可选依赖的守卫门面。
// 本类只用 ModList.isLoaded("curios") 判断是否安装，不直接引用任何 Curios 类型。
// 真正引用 CurioChangeEvent 的监听逻辑被隔离在 CuriosCompatEvents，只有确认 Curios 存在才注册它，
// 从而避免 Curios 缺失时 ClassLoader 解析到 Curios 类型而抛 NoClassDefFoundError。
public final class CuriosCompat {

    public static final String MODID = "curios";

    private CuriosCompat() {}

    // Curios 是否已安装
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    // Curios 存在时，把 CuriosCompatEvents 注册到 Forge 事件总线；没装就静默跳过。
    // 只能在 FMLCommonSetupEvent（或之后）调用，届时长 ModList 已初始化，isLoaded 结果才可靠。
    public static void tryInstall() {
        if (isLoaded()) {
            MinecraftForge.EVENT_BUS.register(CuriosCompatEvents.class);
        }
    }
}
