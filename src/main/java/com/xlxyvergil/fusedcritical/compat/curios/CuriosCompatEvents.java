package com.xlxyvergil.fusedcritical.compat.curios;

import com.xlxyvergil.fusedcritical.normalize.NormalizeSyncEngine;
import net.neoforged.bus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

// Curios 事件监听，由 CuriosCompat.tryInstall() 在确认 Curios 存在后手动注册。
// 不带 @EventBusSubscriber，避免 Curios 缺失时加载 CurioChangeEvent 类抛 NoClassDefFoundError。
// 饰品穿脱/变化时源属性修饰符会变，需要重新归一化。
// 注意 Curios 也是先 post CurioChangeEvent、之后才改 attributeMap，
// 所以这里只登记，交给 NormalizeSyncEngine 在该实体本 tick 结束后统一归一化。
public final class CuriosCompatEvents {

    private CuriosCompatEvents() {}

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        NormalizeSyncEngine.schedule(event.getEntity());
    }
}
