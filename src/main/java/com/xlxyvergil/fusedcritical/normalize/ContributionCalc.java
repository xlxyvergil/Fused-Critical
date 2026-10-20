package com.xlxyvergil.fusedcritical.normalize;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;

// 收集外部源属性上的有效暴击修饰符，原样返回交给 CritNormalizer 复制
public final class ContributionCalc {

    private ContributionCalc() {}

    // 收集某源属性在实体上的有效修饰符
    public static List<AttributeModifier> collect(final LivingEntity entity, final Attribute src) {
        // 1.21.1 的 LivingEntity#getAttribute 只接受 Holder，且查询未绑定的属性会凭空建实例，
        // 所以先用 hasAttribute 判断（等价于 1.20.1 里 getAttribute 返回 null 的情况）
        final Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(src);
        if (!entity.getAttributes().hasAttribute(holder)) {
            return List.of();
        }
        final AttributeInstance instance = entity.getAttribute(holder);
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

    // 排除本模组的 FC_ZERO / FC_MIGRATE
    private static boolean isOurs(final AttributeModifier mod) {
        return mod.is(FusedModifierIds.FC_ZERO) || mod.is(FusedModifierIds.FC_MIGRATE);
    }
}
