package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.resources.ResourceLocation;

// 本模组硬编码修饰符 id（1.21.1 起 AttributeModifier 以 ResourceLocation 标识）：
// FC_ZERO 加在外部源属性上，ADD_MULTIPLIED_TOTAL = -1.0，把源属性归 0；
// FC_MIGRATE 加在本模组自有属性上，ADDITION，承接迁移过来的暴击贡献。固定 id，重复添加会替换旧值。
public final class FusedModifierIds {

    private FusedModifierIds() {}

    // 归 0 用：作用在外部源属性上，ADD_MULTIPLIED_TOTAL = -1.0
    public static final ResourceLocation FC_ZERO = ResourceLocation.fromNamespaceAndPath(FusedCritical.MODID, "crit_zero");

    // 迁移用：作用在本模组自有属性上，ADDITION
    public static final ResourceLocation FC_MIGRATE = ResourceLocation.fromNamespaceAndPath(FusedCritical.MODID, "crit_migrate");
}
