package com.xlxyvergil.fusedcritical.compat.l2tabs;

import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.config.L2TabsDisplayMode;
import dev.xkmc.l2tabs.init.data.AttributeDisplayConfig;
import dev.xkmc.l2tabs.tabs.contents.AttributeEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 按配置项 l2TabsCritDisplayMode 决定数值面板里暴击率/暴击伤害怎么显示：
// REPLACE 用归一化接收方属性替换面板原有的两行（order/usePercent/intrinsic 沿用原条目，显示位置与格式不变）；
// APPEND 保留原有两行并紧随其后追加归一化接收方属性；
// NONE 完全不改，面板保持 L2 Tabs / l2damagetracker 原样。
// 归一化接收方每次刷新都从配置重新解析，因此改 normalizeRateId / normalizeDamageId 后立即生效。
// 本类直接引用 L2 Tabs 类型，只允许在 L2TabsCompat#isLoaded() 为真后调用。
public final class L2TabsCritDisplay {

    // 面板默认暴击两行的来源属性 id（面板配置见 l2damagetracker 的 l2tabs_config/attribute_entry/l2extra.json）
    private static final ResourceLocation CRIT_RATE_ID = ResourceLocation.tryParse("l2damagetracker:crit_rate");
    private static final ResourceLocation CRIT_DAMAGE_ID = ResourceLocation.tryParse("l2damagetracker:crit_damage");

    // 追加条目的 order：面板原有暴击两行为 11000 / 12000，紧随其后
    private static final int APPEND_RATE_ORDER = 11001;
    private static final int APPEND_DAMAGE_ORDER = 12001;

    // 面板列表的当前实例与未经改动的原始快照。
    // L2 Tabs 重建列表（资源重载 / 服务端同步）后二者一起失效，靠引用比较发现。
    private static List<AttributeEntry> lastList;
    private static List<AttributeEntry> original;

    private L2TabsCritDisplay() {}

    // 按当前模式刷新面板条目（幂等，可反复调用）
    public static void apply() {
        final List<AttributeEntry> entries = AttributeDisplayConfig.get();
        if (entries != lastList) {
            lastList = entries;
            original = List.copyOf(entries);
        }
        // 先还原成原始状态再按模式应用，这样模式切换或归一接收方改动都不会留下残留条目
        entries.clear();
        entries.addAll(original);
        final L2TabsDisplayMode mode = FusedConfig.l2TabsCritDisplayMode;
        if (mode == L2TabsDisplayMode.REPLACE) {
            replace(entries);
        } else if (mode == L2TabsDisplayMode.APPEND) {
            append(entries);
        }
    }

    // 把面板条目里的来源暴击属性换成归一接收方属性；面板没显示这两行时什么都不做
    private static void replace(final List<AttributeEntry> entries) {
        final Attribute srcRate = ForgeRegistries.ATTRIBUTES.getValue(CRIT_RATE_ID);
        final Attribute srcDamage = ForgeRegistries.ATTRIBUTES.getValue(CRIT_DAMAGE_ID);
        if (srcRate == null && srcDamage == null) {
            return;
        }
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

    // 保留面板原有条目，紧随其后追加归一接收方属性
    private static void append(final List<AttributeEntry> entries) {
        boolean added = addIfAbsent(entries, FusedConfig.normalizeRate(), APPEND_RATE_ORDER);
        added |= addIfAbsent(entries, FusedConfig.normalizeDamage(), APPEND_DAMAGE_ORDER);
        if (added) {
            // 追加的条目不在 L2 Tabs postMerge 的排序范围内，需要自己重排。
            // 稳定排序保证原有条目相对顺序不变，新条目落到对应 order 的位置。
            entries.sort(Comparator.comparingInt(AttributeEntry::order));
        }
    }

    // 面板里已有该属性（无论来源）就不追加，避免重复行；usePercent 与面板原有暴击两行保持一致
    private static boolean addIfAbsent(final List<AttributeEntry> entries, final Attribute attr, final int order) {
        for (final AttributeEntry entry : entries) {
            if (entry.attr() == attr) {
                return false;
            }
        }
        entries.add(new AttributeEntry(attr, true, order, 0));
        return true;
    }
}
