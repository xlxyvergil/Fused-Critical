package com.xlxyvergil.fusedcritical.detect;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Set;

// 扫描注册表，把目标源属性分成「暴击率」和「暴击伤害」两组。
// 判定依据是属性路径关键字（crit_rate/crit_chance 或 crit_damage/crit_dmg），全局扫描、按命名空间排除 fusedcritical:*。
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
            // 自动扫描：按路径关键字归类，跳过 fusedcritical 自己
            for (final Attribute attr : ForgeRegistries.ATTRIBUTES) {
                final ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attr);
                if (key == null || FusedCritical.MODID.equals(key.getNamespace())) {
                    continue;
                }
                if (matchesRate(key)) {
                    rateSources.add(attr);
                } else if (matchesDamage(key)) {
                    damageSources.add(attr);
                }
            }
            // 额外补充：手动指定的完整源 id（特殊情况才填）
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

    private static boolean matchesRate(final ResourceLocation key) {
        final String p = key.getPath().toLowerCase();
        return p.contains("crit_rate") || p.contains("crit_chance");
    }

    private static boolean matchesDamage(final ResourceLocation key) {
        final String p = key.getPath().toLowerCase();
        return p.contains("crit_damage") || p.contains("crit_dmg");
    }
}
