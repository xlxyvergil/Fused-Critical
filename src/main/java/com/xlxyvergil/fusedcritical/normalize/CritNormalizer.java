package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.config.FusedConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

// 对配置里列出的每个源属性执行「归 0 → 迁移修饰符」。
// 源集合只来自配置（critRateIds / critDamageIds），本类不做任何注册表扫描。
// 迁移记录放在实体 Data Attachment 里，下次归一化只撤销本类加的部分，不误删饰品等外部修饰符。
// 迁移数值会按源属性的换算配置折算（如灾变整数百分点属性 75 -> 0.75），未配置则原样迁移。
public final class CritNormalizer {

    private CritNormalizer() {}

    // 归 0 系数：ADD_MULTIPLIED_TOTAL = -1.0 把源属性恒乘为 0（1.21.1 起 MULTIPLY_TOTAL 更名为 ADD_MULTIPLIED_TOTAL）
    private static final double ZERO_AMOUNT = -1.0D;

    public static void normalize(final LivingEntity entity) {
        if (entity == null || !FusedConfig.hasAnySource()) {
            // 配置里一个源属性都没列（比如整合包没装任何其它暴击 mod）：整体短路，不做任何属性查询与记账
            return;
        }
        migrate(entity, FusedConfig.critRate(), FusedConfig.normalizeRate(), FusedConfig::rateConversionOf);
        migrate(entity, FusedConfig.critDamage(), FusedConfig.normalizeDamage(), FusedConfig::damageConversionOf);
    }

    // 迁移一个类别：对每个源属性做「撤销上次 → 收集 → 迁移 → 归 0」，只动本类记录的修饰符 id。
    // conversion 给源属性的数值换算乘数（未配置为 1.0），用于把整数百分点量纲的源属性折算成接收方的倍率量纲
    private static int migrate(final LivingEntity entity, final Set<Attribute> srcs,
                               final Attribute target, final ToDoubleFunction<Attribute> conversion) {
        if (srcs.isEmpty()) {
            // 空集合直接返回：否则下面会给接收方建一个空的账本条目，留下永不清理的垃圾
            return 0;
        }
        final CritNormalizeData data = CritNormalizeAttachments.get(entity);
        // 1.21.1 的 LivingEntity#getAttribute 只接受 Holder，且查询未绑定的属性会凭空建实例
        final Holder<Attribute> targetHolder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(target);
        if (!entity.getAttributes().hasAttribute(targetHolder)) {
            return 0;
        }
        final AttributeInstance instance = entity.getAttribute(targetHolder);
        if (instance == null) {
            return 0;
        }
        final Map<Attribute, Map<ResourceLocation, ResourceLocation>> perTarget =
                data.migration().computeIfAbsent(target, k -> new HashMap<>());
        int moved = 0;
        for (final Attribute src : srcs) {
            final Map<ResourceLocation, ResourceLocation> srcMap = perTarget.computeIfAbsent(src, k -> new HashMap<>());
            // 撤销上次本类迁移到接收方的、来自该源的修饰符（只撤销本源真实添加过的）
            for (final ResourceLocation migratedId : srcMap.values()) {
                instance.removeModifier(migratedId);
            }
            srcMap.clear();
            // 收集源上当前的真实修饰符（排除本类 FC_ZERO/FC_MIGRATE），并按该源属性的换算乘数折算数值
            final List<AttributeModifier> collected = ContributionCalc.collect(entity, src);
            final double scale = conversion.applyAsDouble(src);
            for (final AttributeModifier mod : collected) {
                // 复用源修饰符自身的 id：同一加成（如同一个全属性加持）落到多个源属性时，
                // 目标属性上只保留一份，避免同一加成被重复计数
                final ResourceLocation newId = mod.id();
                // 目标属性上已有相同 id 则跳过，避免 addTransientModifier 对重复 id 崩溃，
                // 也保证「一个加成只生效一次」
                if (!instance.hasModifier(newId)) {
                    instance.addTransientModifier(
                            new AttributeModifier(newId, mod.amount() * scale, mod.operation()));
                    srcMap.put(mod.id(), newId);
                    moved++;
                }
            }
            // 源属性归 0
            zeroOut(entity, src);
        }
        if (perTarget.isEmpty()) {
            data.migration().remove(target);
        }
        return moved;
    }

    // 实体卸载/死亡/属性表重建时丢弃迁移映射
    public static void clear(final LivingEntity entity) {
        CritNormalizeAttachments.get(entity).migration().clear();
    }

    // 给源属性加 FC_ZERO（ADD_MULTIPLIED_TOTAL = -1.0）让其恒为 0
    private static void zeroOut(final LivingEntity entity, final Attribute src) {
        final AttributeInstance instance = instanceOf(entity, src);
        if (instance == null) {
            return;
        }
        instance.removeModifier(FusedModifierIds.FC_ZERO);
        instance.addTransientModifier(new AttributeModifier(FusedModifierIds.FC_ZERO,
                ZERO_AMOUNT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // 取实体上的属性实例；该属性未绑定到实体时返回 null，不凭空建实例
    private static AttributeInstance instanceOf(final LivingEntity entity, final Attribute attr) {
        final Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attr);
        return entity.getAttributes().hasAttribute(holder) ? entity.getAttribute(holder) : null;
    }
}
