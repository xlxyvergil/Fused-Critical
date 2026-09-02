package com.xlxyvergil.fusedcritical.detect;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Set;

// 扫描注册表，把「归属于其他 mod」的暴击属性分成「暴击率」和「暴击伤害」两组。
// 判定依据是属性路径关键字（crit_rate/crit_chance 或 crit_damage/crit_dmg），全局扫描、排除 fusedcritical:* 自身、并排除归一接收方。
// 配置里的 critRateIds/critDamageIds 只作为「绝对特殊」情况的额外补充（完整属性 id），默认为空。
public final class CritAttributeDetector {

    private CritAttributeDetector() {}

    private static Set<Attribute> rateSources;
    private static Set<Attribute> damageSources;

    // 扫描结果快照
    public record Sources(Set<Attribute> rate, Set<Attribute> damage) {
    }

    // 扫描并返回，结果缓存，首次调用后不再变化
    public static Sources detect() {
        if (rateSources == null || damageSources == null) {
            rateSources = new LinkedHashSet<>();
            damageSources = new LinkedHashSet<>();
            // 归一接收方（指定归一属性）也不能作为源收集，否则会从自己迁移到自己、并把自己归 0
            final Attribute rateTarget = FusedConfig.normalizeRate();
            final Attribute damageTarget = FusedConfig.normalizeDamage();
            // 自动扫描：按路径关键字归类，跳过 fusedcritical 自己、归一接收方
            for (final Attribute attr : ForgeRegistries.ATTRIBUTES) {
                final ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attr);
                if (key == null || FusedCritical.MODID.equals(key.getNamespace())) {
                    continue;
                }
                if (isTarget(attr, rateTarget, damageTarget)) {
                    continue;
                }
                if (matchesRate(key)) {
                    rateSources.add(attr);
                } else if (matchesDamage(key)) {
                    damageSources.add(attr);
                }
            }
            // 额外补充：手动指定的完整源 id（特殊情况才填），直接按配置加入，不额外判断接收方（配置里同时填接收方属于配置错误，不兜底）
            for (final String id : FusedConfig.critRateIds) {
                final ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null && !FusedCritical.MODID.equals(rl.getNamespace())) {
                    final Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
                    if (attr != null) {
                        rateSources.add(attr);
                    }
                }
            }
            for (final String id : FusedConfig.critDamageIds) {
                final ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null && !FusedCritical.MODID.equals(rl.getNamespace())) {
                    final Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
                    if (attr != null) {
                        damageSources.add(attr);
                    }
                }
            }
        }
        return new Sources(Set.copyOf(rateSources), Set.copyOf(damageSources));
    }

    // 判断是否为归一接收方（指定归一属性），是则不能作为源（不归 0、不收集）
    private static boolean isTarget(final Attribute attr, final Attribute rateTarget, final Attribute damageTarget) {
        return attr == rateTarget || attr == damageTarget;
    }

    private static boolean matchesRate(final ResourceLocation key) {
        final String p = key.getPath().toLowerCase();
        return p.contains("crit_rate") || p.contains("crit_chance");
    }

    private static boolean matchesDamage(final ResourceLocation key) {
        final String p = key.getPath().toLowerCase();
        return p.contains("crit_damage") || p.contains("crit_dmg");
    }
}
