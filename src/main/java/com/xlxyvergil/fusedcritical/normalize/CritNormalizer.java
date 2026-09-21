package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.detect.CritAttributeDetector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

// 对配置的每个源属性执行「归 0 → 迁移修饰符」。
// 迁移记录放在实体 capability 里，下次归一化只撤销本类加的部分，不误删饰品等外部修饰符。
// 迁移数值会按源属性的换算配置折算（如灾变整数百分点属性 75 -> 0.75），未配置则原样迁移。
public final class CritNormalizer {

    private CritNormalizer() {}

    // 归 0 系数：MULTIPLY_TOTAL = -1.0 把源属性恒乘为 0
    private static final double ZERO_AMOUNT = -1.0D;

    public static void normalize(final LivingEntity entity) {
        if (entity == null) {
            return;
        }
        final CritAttributeDetector.Sources sources = CritAttributeDetector.detect();
        migrate(entity, sources.rate(), FusedConfig.normalizeRate(), FusedConfig::rateConversionOf);
        migrate(entity, sources.damage(), FusedConfig.normalizeDamage(), FusedConfig::damageConversionOf);
    }

    // 迁移一个类别：对每个源属性做「撤销上次 → 收集 → 迁移 → 归 0」，只动本类记录的 UUID。
    // conversion 给源属性的数值换算乘数（未配置为 1.0），用于把整数百分点量纲的源属性折算成接收方的倍率量纲
    private static int migrate(final LivingEntity entity, final Set<Attribute> srcs,
                               final Attribute target, final ToDoubleFunction<Attribute> conversion) {
        final ICritNormalizeCap cap = CritNormalizeCapability.get(entity);
        if (cap == null) {
            return 0;
        }
        final AttributeInstance instance = entity.getAttribute(target);
        if (instance == null) {
            return 0;
        }
        final Map<Attribute, Map<UUID, UUID>> perTarget =
                cap.migration().computeIfAbsent(target, k -> new HashMap<>());
        int moved = 0;
        for (final Attribute src : srcs) {
            final Map<UUID, UUID> srcMap = perTarget.computeIfAbsent(src, k -> new HashMap<>());
            // 撤销上次本类迁移到接收方的、来自该源的修饰符（只撤销本源真实添加过的）
            for (final UUID migratedId : srcMap.values()) {
                instance.removeModifier(migratedId);
            }
            srcMap.clear();
            // 收集源上当前的真实修饰符（排除本类 FC_ZERO/FC_MIGRATE），并按该源属性的换算乘数折算数值
            final List<AttributeModifier> collected = ContributionCalc.collect(entity, src);
            final double scale = conversion.applyAsDouble(src);
            for (final AttributeModifier mod : collected) {
                // 复用源修饰符自身的 UUID：同一加成（如同一个全属性加持）落到多个源属性时，
                // 目标属性上只保留一份，避免同一加成被重复计数
                final UUID newId = mod.getId();
                // 目标属性上已有相同 UUID 则跳过，避免 addTransientModifier 对重复 UUID 崩溃，
                // 也保证「一个加成只生效一次」
                if (!hasModifier(instance, newId)) {
                    instance.addTransientModifier(
                            new AttributeModifier(newId, mod.getName(), mod.getAmount() * scale, mod.getOperation()));
                    srcMap.put(mod.getId(), newId);
                    moved++;
                }
            }
            // 源属性归 0
            zeroOut(entity, src);
        }
        if (perTarget.isEmpty()) {
            cap.migration().remove(target);
        }
        return moved;
    }

    // 实体卸载/死亡/属性表重建时丢弃迁移映射
    public static void clear(final LivingEntity entity) {
        final ICritNormalizeCap cap = CritNormalizeCapability.get(entity);
        if (cap != null) {
            cap.migration().clear();
        }
    }

    // 接收方属性上是否已存在指定 UUID 的修饰符
    private static boolean hasModifier(final AttributeInstance instance, final UUID id) {
        for (final AttributeModifier mod : instance.getModifiers()) {
            if (mod.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // 给源属性加 FC_ZERO（MULTIPLY_TOTAL = -1.0）让其恒为 0
    private static void zeroOut(final LivingEntity entity, final Attribute src) {
        final AttributeInstance instance = entity.getAttribute(src);
        if (instance == null) {
            return;
        }
        instance.removeModifier(FusedModifierIds.FC_ZERO);
        instance.addTransientModifier(new AttributeModifier(FusedModifierIds.FC_ZERO,
                "FC_ZERO", ZERO_AMOUNT, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
