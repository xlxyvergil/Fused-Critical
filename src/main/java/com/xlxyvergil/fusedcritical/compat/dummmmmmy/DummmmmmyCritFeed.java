package com.xlxyvergil.fusedcritical.compat.dummmmmmy;

import net.mehvahdjukaar.dummmmmmy.common.TargetDummyEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// 隔离类：真正引用 DuMmmMmmy 的 TargetDummyEntity 类型。
// 只能由 DummmmmmyCompat.tryFeedCrit() 在确认 Dummmmmmy 已安装后才调用，
// 避免 Dummmmmmy 缺失时本类被 ClassLoader 解析而抛 NoClassDefFoundError。
public final class DummmmmmyCritFeed {

    private DummmmmmyCritFeed() {}

    // 把暴击倍率喂给假人：TargetDummyEntity.moist(attacker, critModifier) 会把本次攻击记为暴击，
    // 稍后受击时 DuMmmMmmy 自己以暴击色/倍率显示数字，避免我们重复发包造成双重数字。
    public static void feed(Entity target, LivingEntity attacker, float critMultiplier) {
        if (attacker != null && target instanceof TargetDummyEntity dummy) {
            dummy.moist(attacker, critMultiplier);
        }
    }
}
