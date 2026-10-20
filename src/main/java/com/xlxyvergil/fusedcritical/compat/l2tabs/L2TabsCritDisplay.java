package com.xlxyvergil.fusedcritical.compat.l2tabs;

import dev.xkmc.l2tabs.init.L2Tabs;
import dev.xkmc.l2tabs.init.data.AttrDispEntry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.config.L2TabsDisplayMode;

import java.util.Map;
import java.util.Optional;

// 按配置项 l2TabsCritDisplayMode 决定数值面板里暴击率/暴击伤害怎么显示：
// L2 Tabs 3.0 起面板条目改由 l2tabs:attribute_entry 数据表驱动（AttrDispEntry#get 每次现读数据表并按 order 排序），
// 因此这里不再改写内存列表，而是直接改写该数据表：
// REPLACE 摘掉来源两行，用同样的 order/usePercent/intrinsic 换上归一化接收方属性（显示位置与格式不变）；
// APPEND 保留来源两行，追加 order 紧随其后的归一化接收方属性；
// NONE 完全不改，面板保持 L2 Tabs / l2damagetracker 原样。
// 数据表在每次数据包重载时由 DataMapLoader 重建（重建完成后才 post 事件），所以无需清理上一轮的注入残留。
// 本类直接引用 L2 Tabs 类型，只允许在 L2TabsCompat#isLoaded() 为真后调用。
public final class L2TabsCritDisplay {

    // 面板默认暴击两行的来源属性 id（见 l2damagetracker 的 data_maps/attribute/attribute_entry.json）
    private static final ResourceLocation CRIT_RATE_ID = ResourceLocation.tryParse("l2damagetracker:crit_rate");
    private static final ResourceLocation CRIT_DAMAGE_ID = ResourceLocation.tryParse("l2damagetracker:crit_damage");

    // 追加条目的 order：面板原有暴击两行为 11000 / 12000，紧随其后
    private static final int APPEND_RATE_ORDER = 11001;
    private static final int APPEND_DAMAGE_ORDER = 12001;

    private L2TabsCritDisplay() {}

    // 按当前模式改写面板里的暴击率/暴击伤害条目，只应在属性数据表刚重建完成（可写）时调用
    public static void apply(final Registry<Attribute> registry) {
        final Map<ResourceKey<Attribute>, AttrDispEntry> entries = registry.getDataMap(L2Tabs.ATTRIBUTE_ENTRY.reg());
        // 该数据表类型没有任何数据包文件时，注册表给的是不可变空表（Map.of），写入会抛异常
        if (entries.isEmpty()) {
            return;
        }
        final L2TabsDisplayMode mode = FusedConfig.l2TabsCritDisplayMode;
        if (mode == L2TabsDisplayMode.REPLACE) {
            replace(registry, entries);
        } else if (mode == L2TabsDisplayMode.APPEND) {
            append(registry, entries);
        }
    }

    // 把面板里的来源暴击属性换成归一化接收方属性；面板没显示这两行时什么都不做
    private static void replace(final Registry<Attribute> registry, final Map<ResourceKey<Attribute>, AttrDispEntry> entries) {
        move(registry, entries, CRIT_RATE_ID, FusedConfig.normalizeRate());
        move(registry, entries, CRIT_DAMAGE_ID, FusedConfig.normalizeDamage());
    }

    // 摘下来源属性那一行，换成接收方属性，order/usePercent/intrinsic 沿用原条目
    private static void move(final Registry<Attribute> registry, final Map<ResourceKey<Attribute>, AttrDispEntry> entries,
                             final ResourceLocation srcId, final Attribute dst) {
        final Attribute src = BuiltInRegistries.ATTRIBUTE.get(srcId);
        if (src == null) {
            return;
        }
        final Optional<ResourceKey<Attribute>> srcKey = registry.getResourceKey(src);
        if (srcKey.isEmpty()) {
            return;
        }
        // 数据表用注册表内部的 ResourceKey 实例作键，取值也要用注册表自己的实例
        final AttrDispEntry entry = entries.remove(srcKey.get());
        if (entry == null || dst == null) {
            return;
        }
        registry.getResourceKey(dst).ifPresent(key -> entries.put(key, entry));
    }

    // 保留面板原有条目，紧随其后追加归一化接收方属性
    private static void append(final Registry<Attribute> registry, final Map<ResourceKey<Attribute>, AttrDispEntry> entries) {
        addIfAbsent(registry, entries, FusedConfig.normalizeRate(), APPEND_RATE_ORDER);
        addIfAbsent(registry, entries, FusedConfig.normalizeDamage(), APPEND_DAMAGE_ORDER);
    }

    // 面板里已有该属性（无论来源）就不追加，避免重复行；usePercent 与面板原有暴击两行保持一致
    private static void addIfAbsent(final Registry<Attribute> registry, final Map<ResourceKey<Attribute>, AttrDispEntry> entries,
                                    final Attribute attr, final int order) {
        if (attr == null) {
            return;
        }
        registry.getResourceKey(attr).ifPresent(key -> entries.putIfAbsent(key, new AttrDispEntry(true, order, 0)));
    }
}
