package com.xlxyvergil.fusedcritical.normalize;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;

// 迁移源：收集外部源属性上「有效」的暴击修饰符。
// 不做数值换算，只枚举修饰符、过滤我们自家的 FC_ZERO/FC_MIGRATE，其余原样返回交给 CritNormalizer 复制。
public final class ContributionCalc {

    private ContributionCalc() {}

    // 收集某源属性在实体上的有效修饰符（保留原 UUID/op/数值，不换算）
    public static List<AttributeModifier> collect(final LivingEntity entity, final Attribute src) {
        final AttributeInstance instance = entity.getAttribute(src);
        if (instance == null) {
            return List.of();
        }
        final List<AttributeModifier> result = new ArrayList<>();
        for (final AttributeModifier mod : instance.getModifiers()) {
            if (isOurs(mod)) {
                continue;
            }
            result.add(mod);
        }
        return result;
    }

    // 排除我们自己加的 FC_ZERO / FC_MIGRATE
    private static boolean isOurs(final AttributeModifier mod) {
        final java.util.UUID id = mod.getId();
        return FusedModifierIds.FC_ZERO.equals(id) || FusedModifierIds.FC_MIGRATE.equals(id);
    }
}
