package com.xlxyvergil.fusedcritical.compat.curios;

import com.xlxyvergil.fusedcritical.normalize.CritNormalizer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

// Curios 事件监听，由 CuriosCompat.tryInstall() 在确认 Curios 存在后手动注册。
// 不带 @Mod.EventBusSubscriber，避免 Curios 缺失时加载 CurioChangeEvent 类抛 NoClassDefFoundError。
// 饰品穿脱/变化时源属性修饰符会变，需要重新归一化。
public final class CuriosCompatEvents {

    private CuriosCompatEvents() {}

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        CritNormalizer.normalize(event.getEntity());
    }
}
