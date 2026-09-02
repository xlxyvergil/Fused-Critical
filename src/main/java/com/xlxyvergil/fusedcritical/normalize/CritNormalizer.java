package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.detect.CritAttributeDetector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 对配置的每个源属性执行「归 0 → 迁移修饰符」。
// 迁移记录放在实体 capability 里，下次归一化只撤销本类加的部分，不误删饰品等外部修饰符。
public final class CritNormalizer {

    private CritNormalizer() {}

    // 归 0 系数：MULTIPLY_TOTAL = -1.0 把源属性恒乘为 0
    private static final double ZERO_AMOUNT = -1.0D;

    public static void normalize(final LivingEntity entity) {
        if (entity == null) {
            return;
        }
        final CritAttributeDetector.Sources sources = CritAttributeDetector.detect();
        migrate(entity, sources.rate(), FusedConfig.normalizeRate());
        migrate(entity, sources.damage(), FusedConfig.normalizeDamage());
    }

    // 迁移一个类别：对每个源属性做「撤销上次 → 收集 → 迁移 → 归 0」，只动本类记录的 UUID
    private static int migrate(final LivingEntity entity, final Set<Attribute> srcs,
                               final Attribute target) {
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
        // 跨源累积已占用 UUID，避免迁移新 UUID 时撞上
        final Set<UUID> occupied = new HashSet<>();
        int moved = 0;
        for (final Attribute src : srcs) {
            final Map<UUID, UUID> srcMap = perTarget.computeIfAbsent(src, k -> new HashMap<>());
            // 撤销上次本类迁移到接收方的、来自该源的修饰符
            for (final UUID migratedId : srcMap.values()) {
                instance.removeModifier(migratedId);
            }
            srcMap.clear();
            // 收集源上当前的真实修饰符（排除本类 FC_ZERO/FC_MIGRATE），记下它们占用的 UUID
            final List<AttributeModifier> collected = ContributionCalc.collect(entity, src);
            for (final AttributeModifier mod : collected) {
                occupied.add(mod.getId());
            }
            // 挑不冲突的新 UUID 加到接收方，记录源 UUID → 接收方新 UUID
            for (final AttributeModifier mod : collected) {
                final UUID newId = newAvailableUuid(occupied);
                occupied.add(newId);
                instance.addTransientModifier(
                        new AttributeModifier(newId, mod.getName(), mod.getAmount(), mod.getOperation()));
                srcMap.put(mod.getId(), newId);
                moved++;
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

    // 生成一个不与已占用 UUID 冲突的随机 UUID
    private static UUID newAvailableUuid(final Set<UUID> occupied) {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (occupied.contains(id));
        return id;
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
