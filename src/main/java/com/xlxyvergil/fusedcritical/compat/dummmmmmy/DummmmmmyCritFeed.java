package com.xlxyvergil.fusedcritical.compat.dummmmmmy;

import net.mehvahdjukaar.dummmmmmy.common.TargetDummyEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// 隔离类：真正引用 DuMmmMmmy 的 TargetDummyEntity 类型，只由 DummmmmmyCompat.tryFeedCrit() 在确认安装后调用。
public final class DummmmmmyCritFeed {

    private DummmmmmyCritFeed() {}

    // 把本次暴击倍率喂给假人，让 DuMmmMmmy 自己以暴击色/倍率显示数字，避免我们重复发包。
    public static void feed(Entity target, LivingEntity attacker, float critMultiplier) {
        if (attacker != null && target instanceof TargetDummyEntity dummy) {
            dummy.moist(attacker, critMultiplier);
        }
    }
}
