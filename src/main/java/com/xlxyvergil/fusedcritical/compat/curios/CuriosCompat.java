package com.xlxyvergil.fusedcritical.compat.curios;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

// Curios 可选依赖守卫：只用 ModList.isLoaded 判断，不直接引用 Curios 类型。
// 引用 CurioChangeEvent 的监听隔离在 CuriosCompatEvents，仅在确认 Curios 存在时注册，避免缺失时抛 NoClassDefFoundError。
public final class CuriosCompat {

    public static final String MODID = "curios";

    private CuriosCompat() {}

    // Curios 是否已安装
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    // Curios 存在时把监听注册到事件总线；在 FMLCommonSetupEvent 之后调用，isLoaded 才可靠
    public static void tryInstall() {
        if (isLoaded()) {
            MinecraftForge.EVENT_BUS.register(CuriosCompatEvents.class);
        }
    }
}
