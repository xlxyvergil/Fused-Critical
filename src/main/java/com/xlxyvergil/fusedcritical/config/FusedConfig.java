package com.xlxyvergil.fusedcritical.config;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.attribute.FusedAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 配置：指定归 0 集（哪些暴击属性被归 0、修饰符被迁移）和归一接收方（修饰符迁到哪个属性）。
// 归 0 集 = 自动扫描注册表（按路径关键字归纳）∪ zeroRateIds/zeroDamageIds 白名单，剔除 blacklist 与归一接收方。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FusedConfig {

    private FusedConfig() {}

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 归 0 的暴击率属性 id 列表（modid:path）。默认含各 mod 暴击率属性，含 FlameChaseArtifacts 把「幸运一击率」当暴击率。
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ZERO_RATE_IDS = BUILDER
            .comment("归 0 的暴击率属性 id 列表（格式 modid:path），默认含 attributeslib:crit_chance 等各 mod 暴击率及 flame_chase_artifacts:luck_hit_chance。这些属性被归 0，修饰符迁移到 normalizeRateId 指定接收方。")
            .defineListAllowEmpty("zeroRateIds", List.of(
                    "attributeslib:crit_chance",
                    "confluence:crit_chance",
                    "l2damagetracker:crit_rate",
                    "myosotis:cri_chance",
                    "reshapes:critical_chance",
                    "flame_chase_artifacts:luck_hit_chance"), o -> o instanceof String);

    // 归 0 的暴击伤害属性 id 列表，默认含各 mod 暴击伤害属性及 flame_chase_artifacts:luck_hit_amplifier
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ZERO_DAMAGE_IDS = BUILDER
            .comment("归 0 的暴击伤害属性 id 列表（格式 modid:path），默认含 attributeslib:crit_damage 等各 mod 暴击伤害及 flame_chase_artifacts:luck_hit_amplifier。这些属性被归 0，修饰符迁移到 normalizeDamageId 指定接收方。")
            .defineListAllowEmpty("zeroDamageIds", List.of(
                    "attributeslib:crit_damage",
                    "l2damagetracker:crit_damage",
                    "cataclysm:additional_critical_damage",
                    "myosotis:cri_damage",
                    "reshapes:critical_damage",
                    "flame_chase_artifacts:luck_hit_amplifier"), o -> o instanceof String);

    // 数值换算：部分 mod 的暴击属性以整数百分点计数（如灾变 +75 点 = +75%），迁移到本模组倍率属性前需折算。
    // 只有列在这里的源属性才折算：目标数值 = 源数值 x 乘数。未列出的属性按 1.0 原样迁移。
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> RATE_CONVERSION = BUILDER
            .comment("暴击率源属性的数值换算（格式 modid:path=乘数，可多项）。仅列出的属性参与折算：迁移到归一接收方时 目标数值 = 源数值 x 乘数。\n"
                    + "归一接收方是倍率量纲（1.0 = 100%），若源属性同为倍率量纲则不要配置。\n"
                    + "示例：某 mod +1 点表示 +1%（即 0.01），写 mymod:crit_chance=0.01。默认空。")
            .defineListAllowEmpty("rateConversion", List.of(), o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DAMAGE_CONVERSION = BUILDER
            .comment("暴击伤害源属性的数值换算（格式 modid:path=乘数，可多项）。仅列出的属性参与折算：迁移到归一接收方时 目标数值 = 源数值 x 乘数。\n"
                    + "归一接收方是倍率量纲（1.5 = 150%），若源属性同为倍率量纲则不要配置。\n"
                    + "示例：灾变 cataclysm:additional_critical_damage 是整数百分点（+75 点 = +75%），按 0.01 折算成 +0.75。\n"
                    + "默认含 cataclysm:additional_critical_damage=0.01。")
            .defineListAllowEmpty("damageConversion", List.of(
                    "cataclysm:additional_critical_damage=0.01"), o -> o instanceof String);

    // 归 0 集黑名单：从「自动扫描 + 白名单」结果中剔除，用于排除误捕或不想归 0 的属性
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_RATE_IDS = BUILDER
            .comment("归 0 集黑名单（暴击率，格式 modid:path）。")
            .defineListAllowEmpty("blacklistRateIds", List.of(), o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_DAMAGE_IDS = BUILDER
            .comment("归 0 集黑名单（暴击伤害，格式 modid:path）。")
            .defineListAllowEmpty("blacklistDamageIds", List.of(), o -> o instanceof String);

    // 归一接收方（暴击率）：收集到的暴击率修饰符全迁到该属性，默认本模组自有 crit_chance
    // 该属性自身会从源集合排除（不归 0、不收集），因此「排除某属性」等价「设为归一接收方」
    private static final ForgeConfigSpec.ConfigValue<? extends String> NORMALIZE_RATE_ID = BUILDER
            .comment("归一接收方（暴击率）：收集到的暴击率修饰符迁移到该属性上。默认 fusedcritical:crit_chance。")
            .define("normalizeRateId", FusedCritical.MODID + ":crit_chance");

    // 归一接收方（暴击伤害）：同上，默认本模组自有 crit_damage
    private static final ForgeConfigSpec.ConfigValue<? extends String> NORMALIZE_DAMAGE_ID = BUILDER
            .comment("归一接收方（暴击伤害）：收集到的暴击伤害修饰符迁移到该属性上。默认 fusedcritical:crit_damage。")
            .define("normalizeDamageId", FusedCritical.MODID + ":crit_damage");

    // 不显示伤害数字的实体 id 列表（modid:path）：命中这些实体时本模组不发伤害数字包。默认屏蔽 Dummmmmmy 靶子，因其数字由自身显示
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DAMAGE_NUMBER_HIDDEN_ENTITY_IDS = BUILDER
            .comment("不显示伤害数字的实体 id 列表（格式 modid:path）。命中时本模组不发送伤害数字包。默认仅 Dummmmmmy 靶子（dummmmmmy:target_dummy）。")
            .defineListAllowEmpty("damageNumberHiddenEntityIds", List.of(
                    "dummmmmmy:target_dummy"), o -> o instanceof String);

    // L2 Tabs 属性面板里暴击率/暴击伤害的显示方式（仅加载 L2 Tabs 时生效）
    private static final ForgeConfigSpec.EnumValue<L2TabsDisplayMode> L2TABS_CRIT_DISPLAY_MODE = BUILDER
            .comment("L2 Tabs 属性面板中暴击率/暴击伤害的显示方式（仅在加载 L2 Tabs 时生效）：\n"
                    + "REPLACE = 用归一化接收方属性替换面板原有的两行；\n"
                    + "APPEND = 保留面板原有两行，并在其后追加归一化接收方属性；\n"
                    + "NONE = 不改动面板，保持 L2 Tabs / l2damagetracker 原样。")
            .defineEnum("l2TabsCritDisplayMode", L2TabsDisplayMode.REPLACE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<String> zeroRateIds;
    public static Set<String> zeroDamageIds;
    public static Set<String> blacklistRateIds;
    public static Set<String> blacklistDamageIds;
    public static String normalizeRateId;
    public static String normalizeDamageId;
    public static Set<String> damageNumberHiddenEntityIds;
    public static L2TabsDisplayMode l2TabsCritDisplayMode;
    // 数值换算表：源属性 id -> 乘数
    public static Map<String, Double> rateConversion;
    public static Map<String, Double> damageConversion;

    // 接收方解析缓存
    private static Attribute cachedNormalizeRate;
    private static Attribute cachedNormalizeDamage;

    // 归 0 集解析缓存
    private static Set<Attribute> cachedZeroRate;
    private static Set<Attribute> cachedZeroDamage;
    private static Set<Attribute> cachedBlacklistRate;
    private static Set<Attribute> cachedBlacklistDamage;

    // 数值换算解析缓存
    private static Map<Attribute, Double> cachedRateConversion;
    private static Map<Attribute, Double> cachedDamageConversion;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        zeroRateIds = Setcopy(ZERO_RATE_IDS.get());
        zeroDamageIds = Setcopy(ZERO_DAMAGE_IDS.get());
        blacklistRateIds = Setcopy(BLACKLIST_RATE_IDS.get());
        blacklistDamageIds = Setcopy(BLACKLIST_DAMAGE_IDS.get());
        normalizeRateId = NORMALIZE_RATE_ID.get();
        normalizeDamageId = NORMALIZE_DAMAGE_ID.get();
        damageNumberHiddenEntityIds = Setcopy(DAMAGE_NUMBER_HIDDEN_ENTITY_IDS.get());
        l2TabsCritDisplayMode = L2TABS_CRIT_DISPLAY_MODE.get();
        rateConversion = parseConversion(RATE_CONVERSION.get());
        damageConversion = parseConversion(DAMAGE_CONVERSION.get());
        cachedNormalizeRate = null;
        cachedNormalizeDamage = null;
        cachedZeroRate = null;
        cachedZeroDamage = null;
        cachedBlacklistRate = null;
        cachedBlacklistDamage = null;
        cachedRateConversion = null;
        cachedDamageConversion = null;
    }

    // 解析归一接收方，非法 id 回退本模组自有属性
    public static Attribute normalizeRate() {
        if (cachedNormalizeRate == null) {
            cachedNormalizeRate = resolve(normalizeRateId, FusedAttributes.CRIT_CHANCE.get());
        }
        return cachedNormalizeRate;
    }

    // 解析归一接收方，非法 id 回退本模组自有属性
    public static Attribute normalizeDamage() {
        if (cachedNormalizeDamage == null) {
            cachedNormalizeDamage = resolve(normalizeDamageId, FusedAttributes.CRIT_DAMAGE.get());
        }
        return cachedNormalizeDamage;
    }

    // 解析归 0 集，跳过未注册 id
    public static Set<Attribute> zeroRate() {
        if (cachedZeroRate == null) {
            cachedZeroRate = resolveAll(zeroRateIds);
        }
        return cachedZeroRate;
    }

    // 解析归 0 集，跳过未注册 id
    public static Set<Attribute> zeroDamage() {
        if (cachedZeroDamage == null) {
            cachedZeroDamage = resolveAll(zeroDamageIds);
        }
        return cachedZeroDamage;
    }

    // 解析归 0 集黑名单，跳过未注册 id
    public static Set<Attribute> blacklistRate() {
        if (cachedBlacklistRate == null) {
            cachedBlacklistRate = resolveAll(blacklistRateIds);
        }
        return cachedBlacklistRate;
    }

    // 解析归 0 集黑名单，跳过未注册 id
    public static Set<Attribute> blacklistDamage() {
        if (cachedBlacklistDamage == null) {
            cachedBlacklistDamage = resolveAll(blacklistDamageIds);
        }
        return cachedBlacklistDamage;
    }

    // 该实体类型 id（modid:path）是否命中「不显示伤害数字」屏蔽列表
    public static boolean isDamageNumberHidden(String entityTypeId) {
        return damageNumberHiddenEntityIds != null && damageNumberHiddenEntityIds.contains(entityTypeId);
    }

    // 暴击率源属性的数值换算乘数，未配置按 1.0（原样迁移）
    public static double rateConversionOf(final Attribute src) {
        if (cachedRateConversion == null) {
            cachedRateConversion = resolveConversion(rateConversion);
        }
        return cachedRateConversion.getOrDefault(src, 1.0D);
    }

    // 暴击伤害源属性的数值换算乘数，未配置按 1.0（原样迁移）
    public static double damageConversionOf(final Attribute src) {
        if (cachedDamageConversion == null) {
            cachedDamageConversion = resolveConversion(damageConversion);
        }
        return cachedDamageConversion.getOrDefault(src, 1.0D);
    }

    private static Attribute resolve(final String id, final Attribute fallback) {
        if (id == null || id.isBlank()) {
            return fallback;
        }
        final ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return fallback;
        }
        final Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
        return attr != null ? attr : fallback;
    }

    // 解析 "modid:path=乘数" 形式的换算配置，非法项跳过
    private static Map<String, Double> parseConversion(final List<? extends String> list) {
        final Map<String, Double> result = new HashMap<>();
        if (list == null) {
            return Map.copyOf(result);
        }
        for (final String raw : list) {
            if (raw == null) {
                continue;
            }
            final int split = raw.indexOf('=');
            if (split <= 0) {
                continue;
            }
            try {
                result.put(raw.substring(0, split).trim(), Double.parseDouble(raw.substring(split + 1).trim()));
            } catch (final NumberFormatException ignored) {
            }
        }
        return Map.copyOf(result);
    }

    // 解析换算表为 属性 -> 乘数，跳过非法/未注册的 id
    private static Map<Attribute, Double> resolveConversion(final Map<String, Double> entries) {
        final Map<Attribute, Double> result = new HashMap<>();
        if (entries == null) {
            return Map.copyOf(result);
        }
        for (final Map.Entry<String, Double> entry : entries.entrySet()) {
            final ResourceLocation rl = ResourceLocation.tryParse(entry.getKey());
            if (rl == null) {
                continue;
            }
            final Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
            if (attr != null) {
                result.put(attr, entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    // 解析 id 列表为属性集合，跳过非法/未注册的 id
    private static Set<Attribute> resolveAll(final Set<String> ids) {
        final Set<Attribute> result = new java.util.LinkedHashSet<>();
        for (final String id : ids) {
            final ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) {
                continue;
            }
            final Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
            if (attr != null) {
                result.add(attr);
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> Setcopy(final List<? extends String> list) {
        return list == null ? Set.of() : Set.copyOf(list);
    }
}
