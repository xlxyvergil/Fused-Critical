package com.xlxyvergil.fusedcritical.compat.dummmmmmy;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

// DuMmmMmmy 可选依赖的守卫门面。
// 本类只用 ModList.isLoaded("dummmmmmy") 判断是否安装，不直接引用任何 DuMmmMmmy 类型。
// 真正引用 TargetDummyEntity#moist 的逻辑被隔离在 DummmmmmyCritFeed，只有确认 Dummmmmmy 存在才调用它，
// 从而避免 Dummmmmmy 缺失时 ClassLoader 解析到其类型而抛 NoClassDefFoundError。
public final class DummmmmmyCompat {

    public static final String MODID = "dummmmmmy";

    private DummmmmmyCompat() {}

    // DuMmmMmmy 是否已安装
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    // 假人被暴击命中时调用：把我们 mod 统合的暴击倍率喂给 DuMmmMmmy，让它按暴击规则显示数字。
    // 只在 DuMmmMmmy 已安装时才走隔离类，否则静默跳过。调用方应保证只有产生暴击时才调用。
    public static void tryFeedCrit(Entity target, LivingEntity attacker, float critMultiplier) {
        if (isLoaded()) {
            DummmmmmyCritFeed.feed(target, attacker, critMultiplier);
        }
    }
}
