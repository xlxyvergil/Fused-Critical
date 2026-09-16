package com.xlxyvergil.fusedcritical.compat.l2tabs;

import net.minecraftforge.fml.ModList;

// L2 Tabs 可选依赖守卫：只用 ModList.isLoaded 判断，不直接引用 L2 Tabs 类型。
// 引用 AttributeDisplayConfig 的显示替换隔离在 L2TabsCritDisplay，仅在确认 L2 Tabs 存在时调用，避免缺失时抛 NoClassDefFoundError。
public final class L2TabsCompat {

    public static final String MODID = "l2tabs";

    private L2TabsCompat() {}

    // L2 Tabs 是否已安装
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    // L2 Tabs 存在时替换数值面板里的暴击率/暴击伤害条目
    public static void tryReplaceCritDisplay() {
        if (isLoaded()) {
            L2TabsCritDisplay.replaceCritEntries();
        }
    }
}
