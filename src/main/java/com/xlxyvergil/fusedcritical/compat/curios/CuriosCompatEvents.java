package com.xlxyvergil.fusedcritical.compat.curios;

import com.xlxyvergil.fusedcritical.normalize.CritNormalizer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

// Curios 专属事件监听（隔离类，不带 @Mod.EventBusSubscriber 注解）。
// 不用注解的原因：注解会在 mod 加载阶段自动扫描并注册本类，即使 Curios 没装也会触发 ClassLoader 解析
// CurioChangeEvent 等类型，导致 NoClassDefFoundError。所以本类必须由 CuriosCompat.tryInstall()
// 在确认 Curios 存在后才手动注册，避免类加载崩溃。
// 监听 CurioChangeEvent：饰品穿脱/实体加入世界/实体克隆时触发，源属性上的暴击修饰符可能随之变化，需要重新归一化。
public final class CuriosCompatEvents {

    private CuriosCompatEvents() {}

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        CritNormalizer.normalize(event.getEntity());
    }
}
