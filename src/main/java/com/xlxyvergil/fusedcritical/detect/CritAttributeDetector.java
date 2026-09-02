package com.xlxyvergil.fusedcritical.detect;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

// 归 0 集 = 自动扫描注册表（按路径关键字归纳）∪ zeroRateIds/zeroDamageIds 白名单，剔除黑名单与归一接收方。
public final class CritAttributeDetector {

    private CritAttributeDetector() {}

    // 归一化源集合，每次按最新配置计算
    public static Sources detect() {
        final Attribute rateTarget = FusedConfig.normalizeRate();
        final Attribute damageTarget = FusedConfig.normalizeDamage();
        return new Sources(
                refine(scanRate(), FusedConfig.zeroRate(), FusedConfig.blacklistRate(), rateTarget, damageTarget),
                refine(scanDamage(), FusedConfig.zeroDamage(), FusedConfig.blacklistDamage(), rateTarget, damageTarget));
    }

    // 自动扫描：按路径关键字归纳暴击率属性
    private static Set<Attribute> scanRate() {
        final Set<Attribute> result = new LinkedHashSet<>();
        for (final Attribute attr : ForgeRegistries.ATTRIBUTES) {
            final ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attr);
            if (key == null || FusedCritical.MODID.equals(key.getNamespace())) {
                continue;
            }
            if (matchesRate(key.getPath().toLowerCase(Locale.ROOT))) {
                result.add(attr);
            }
        }
        return result;
    }

    // 自动扫描：按路径关键字归纳暴击伤害属性
    private static Set<Attribute> scanDamage() {
        final Set<Attribute> result = new LinkedHashSet<>();
        for (final Attribute attr : ForgeRegistries.ATTRIBUTES) {
            final ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attr);
            if (key == null || FusedCritical.MODID.equals(key.getNamespace())) {
                continue;
            }
            if (matchesDamage(key.getPath().toLowerCase(Locale.ROOT))) {
                result.add(attr);
            }
        }
        return result;
    }

    private static boolean matchesRate(final String path) {
        return path.contains("crit")
                && (path.contains("chance") || path.contains("rate") || path.contains("prob"));
    }

    private static boolean matchesDamage(final String path) {
        return path.contains("crit")
                && (path.contains("damage") || path.contains("dmg") || path.contains("amplifier")
                || path.contains("bonus") || path.contains("multiplier"));
    }

    // 并集白名单、剔除黑名单与归一接收方
    private static Set<Attribute> refine(final Set<Attribute> scanned,
                                         final Set<Attribute> whitelist,
                                         final Set<Attribute> blacklist,
                                         final Attribute rateTarget,
                                         final Attribute damageTarget) {
        final Set<Attribute> result = new LinkedHashSet<>();
        result.addAll(scanned);
        result.addAll(whitelist);
        result.removeAll(blacklist);
        result.remove(rateTarget);
        result.remove(damageTarget);
        return Set.copyOf(result);
    }

    // 扫描结果快照
    public record Sources(Set<Attribute> rate, Set<Attribute> damage) {
    }
}
