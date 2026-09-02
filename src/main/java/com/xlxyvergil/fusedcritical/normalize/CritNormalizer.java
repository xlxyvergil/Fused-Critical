package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.detect.CritAttributeDetector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// 单实体归一化：对每个外部源属性执行「归 0 → 迁移修饰符」。
// 归 0 是给源属性加 FC_ZERO（MULTIPLY_TOTAL = -1.0）让它恒为 0；迁移是把源属性上「有效」的修饰符原样复制到归一接收方（默认本模组自有属性）上。
// 迁移不做数值换算，沿用源修饰符的名字/op/amount，但 UUID 改为新生成的随机值（需要避开源上已用的 UUID），
// 从而避免全属性加成饰品复用同一 UUID 时重复添加崩溃；所以源属性默认值不会带入。
public final class CritNormalizer {

    private CritNormalizer() {}

    // 归 0 系数：MULTIPLY_TOTAL = -1.0 把源属性恒乘为 0
    private static final double ZERO_AMOUNT = -1.0D;

    // 对单个实体执行归一化
    public static void normalize(final LivingEntity entity) {
        if (entity == null) {
            return;
        }
        final CritAttributeDetector.Sources sources = CritAttributeDetector.detect();

        // 迁移「暴击率」类别：把各源属性上的有效修饰符改挂到配置指定的归一接收方（默认本模组自有暴击率）上
        migrate(entity, sources.rate(), FusedConfig.normalizeRate());

        // 迁移「暴击伤害」类别：同上
        migrate(entity, sources.damage(), FusedConfig.normalizeDamage());
    }

    // 迁移一个类别：先清空归一接收方上一次的修饰符，再遍历该类别所有源属性，
    // 读取它们各自的属性修饰符数据（UUID/名字/数值/算法），用这些数据新建一个「临时」修饰符，
    // 挂到归一接收方上（归属属性用的是接收方，不写入存档，死亡或属性表重建时会自动清空，由事件驱动重新镜像），
    // 最后给该源属性归 0。
    // 注意：只能在整个迁移前清空一次；若每迭代一个源就清空重加，同类别有多个源时后一个会覆盖前一个。
    private static int migrate(final LivingEntity entity, final Set<Attribute> srcs,
                               final Attribute target) {
        final AttributeInstance instance = entity.getAttribute(target);
        if (instance == null) {
            return 0;
        }
        // 自有属性内容由本类全量维护，先清空再重新挂，保证「源上有则迁移、源上没了则清除」，且不累积残留
        for (final AttributeModifier mod : instance.getModifiers()) {
            instance.removeModifier(mod.getId());
        }
        // 先把本类别所有源属性上「要迁移」的修饰符汇总，同时记下源上已有的全部 UUID。
        // 某些「全属性加成」饰品会用同一个 UUID 把一条修饰符挂到多个源属性上；若直接复用该 UUID 再次挂到自有属性，
        // 会触发 AttributeInstance.addTransientModifier 的重复 UUID 检查而抛 IllegalArgumentException。
        // 因此迁移时改用新生成的随机 UUID，并保证不与源上已有的 UUID 冲突。
        final List<AttributeModifier> collected = new ArrayList<>();
        final Set<UUID> occupied = new HashSet<>();
        for (final Attribute src : srcs) {
            for (final AttributeModifier mod : ContributionCalc.collect(entity, src)) {
                collected.add(mod);
                occupied.add(mod.getId());
            }
        }
        int moved = 0;
        for (final AttributeModifier mod : collected) {
            final UUID newId = newAvailableUuid(occupied);
            occupied.add(newId);
            instance.addTransientModifier(
                    new AttributeModifier(newId, mod.getName(), mod.getAmount(), mod.getOperation()));
            moved++;
        }
        for (final Attribute src : srcs) {
            zeroOut(entity, src);
        }
        return moved;
    }

    // 生成一个不与已占用 UUID 冲突的随机 UUID（随机碰撞概率几乎为零，加上防重兜底）
    private static UUID newAvailableUuid(final Set<UUID> occupied) {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (occupied.contains(id));
        return id;
    }

    // 给源属性加 FC_ZERO（MULTIPLY_TOTAL = -1.0），使源属性最终值恒为 0
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
