package com.xlxyvergil.fusedcritical.compat.dummmmmmy;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

// DuMmmMmmy 可选依赖守卫：只用 ModList.isLoaded 判断，不直接引用 DuMmmMmmy 类型。
// 真正调用 TargetDummyEntity#moist 的逻辑被隔离在 DummmmmmyCritFeed，只在确认安装后才调用，避免缺失时抛 NoClassDefFoundError。
public final class DummmmmmyCompat {

    public static final String MODID = "dummmmmmy";

    private DummmmmmyCompat() {}

    // DuMmmMmmy 是否已安装
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    // 假人被暴击命中时，把统一后的暴击倍率喂给 DuMmmMmmy，让它按暴击规则显示。只在安装时走隔离类。
    public static void tryFeedCrit(Entity target, LivingEntity attacker, float critMultiplier) {
        if (isLoaded()) {
            DummmmmmyCritFeed.feed(target, attacker, critMultiplier);
        }
    }
}
