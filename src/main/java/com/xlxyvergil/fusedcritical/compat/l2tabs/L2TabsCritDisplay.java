package com.xlxyvergil.fusedcritical.compat.l2tabs;

import com.xlxyvergil.fusedcritical.config.FusedConfig;
import dev.xkmc.l2tabs.init.data.AttributeDisplayConfig;
import dev.xkmc.l2tabs.tabs.contents.AttributeEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 只把数值面板里默认的两行「暴击率 / 暴击伤害」（面板配置里由 l2damagetracker 提供）替换成归一化接收方属性，
// 其余条目一律不动：order/usePercent/intrinsic 全部沿用原条目，所以显示位置与格式不变，变成「统合暴击: 5%」「统合暴伤: 150%」。
// 本类直接引用 L2 Tabs 类型，只允许在 L2TabsCompat#isLoaded() 为真后调用。
public final class L2TabsCritDisplay {

    // 面板默认暴击两行的来源属性 id（面板配置见 l2damagetracker 的 l2tabs_config/attribute_entry/l2extra.json）
    private static final ResourceLocation CRIT_RATE_ID = ResourceLocation.tryParse("l2damagetracker:crit_rate");
    private static final ResourceLocation CRIT_DAMAGE_ID = ResourceLocation.tryParse("l2damagetracker:crit_damage");

    private L2TabsCritDisplay() {}

    // 把面板条目中的来源暴击属性换成归一接收方属性；面板没显示这两行时什么都不做（替换幂等，可反复调用）
    public static void replaceCritEntries() {
        final Attribute srcRate = ForgeRegistries.ATTRIBUTES.getValue(CRIT_RATE_ID);
        final Attribute srcDamage = ForgeRegistries.ATTRIBUTES.getValue(CRIT_DAMAGE_ID);
        if (srcRate == null && srcDamage == null) {
            return;
        }
        final List<AttributeEntry> entries = AttributeDisplayConfig.get();
        boolean found = false;
        for (final AttributeEntry entry : entries) {
            if (entry.attr() == srcRate || entry.attr() == srcDamage) {
                found = true;
                break;
            }
        }
        if (!found) {
            return;
        }
        final Attribute dstRate = FusedConfig.normalizeRate();
        final Attribute dstDamage = FusedConfig.normalizeDamage();
        final List<AttributeEntry> replaced = new ArrayList<>(entries.size());
        final Set<Attribute> seen = new HashSet<>();
        for (final AttributeEntry entry : entries) {
            Attribute attr = entry.attr();
            if (attr == srcRate) {
                attr = dstRate;
            } else if (attr == srcDamage) {
                attr = dstDamage;
            }
            // 多个来源属性映射到同一接收方时只保留先出现的那条，避免重复行
            if (!seen.add(attr)) {
                continue;
            }
            replaced.add(attr == entry.attr() ? entry : new AttributeEntry(attr, entry.usePercent(), entry.order(), entry.intrinsic()));
        }
        entries.clear();
        entries.addAll(replaced);
    }
}
