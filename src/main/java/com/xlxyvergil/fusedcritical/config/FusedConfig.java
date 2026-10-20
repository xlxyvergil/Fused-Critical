package com.xlxyvergil.fusedcritical.config;

import com.mojang.logging.LogUtils;
import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.attribute.FusedAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 配置：哪些暴击率 / 暴击伤害属性要被归 0，修饰符迁到哪个归一接收方属性上。
//
// 解耦约定：归一化只认配置里的 critRateIds / critDamageIds 两个列表，运行期不再扫描属性注册表。
// 首次生成配置文件时，ConfigSeeder 会把扫描结果写进这两个列表（判据是 scanSeedVersion 是否为 "0"），
// 之后列表完全由用户维护 —— 删掉即不再归一化，新装模组后想补就手动加一行，不会被自动改回去。
@EventBusSubscriber(modid = FusedCritical.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FusedConfig {

    private FusedConfig() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 归一化源（暴击率）：唯一权威列表。默认值为内置已知表，首次生成时会替换成扫描结果
    private static final ModConfigSpec.ConfigValue<List<? extends String>> CRIT_RATE_IDS = BUILDER
            .comment("需要归一化的暴击率属性 id 列表（格式 modid:path）。归一化只认这里的条目：",
                    "列出的属性会被归 0，其修饰符迁移到 normalizeRateId 指定的接收方属性上。",
                    "首次生成配置时会自动写入扫描到的属性；之后本列表完全由你维护 —— 删掉即不再归一化，",
                    "想补就手动加一行（不会自动再写入，避免覆盖你的改动）。",
                    "默认值只是内置已知表；扫描到更多属性时，首次生成会用扫描结果整体替换它。")
            .defineListAllowEmpty("critRateIds", KnownCritIds.RATE, o -> o instanceof String);

    // 归一化源（暴伤）：同上
    private static final ModConfigSpec.ConfigValue<List<? extends String>> CRIT_DAMAGE_IDS = BUILDER
            .comment("需要归一化的暴击伤害属性 id 列表（格式 modid:path）。语义同 critRateIds：",
                    "列出的属性被归 0，修饰符迁移到 normalizeDamageId 指定的接收方属性上。",
                    "默认值只是内置已知表；首次生成时会被扫描结果整体替换。")
            .defineListAllowEmpty("critDamageIds", KnownCritIds.DAMAGE, o -> o instanceof String);

    // 扫描写入标记：判据不是"配置文件在不在"，而是"配置项有没有被写入过"，
    // 所以老配置文件升级上来也能自然补上新键并写入一次。
    private static final ModConfigSpec.ConfigValue<String> SCAN_SEED_VERSION = BUILDER
            .comment("内部标记：扫描结果是否已经写入过 critRateIds / critDamageIds。",
                    "\"0\" = 还没写过（首次生成、或从旧版本升级上来），启动时会扫描并写入一次；",
                    "其它值 = 已写入，之后永不再自动改这两个列表。",
                    "请勿手改。确实想让扫描结果重新覆盖两个列表时，把它改回 \"0\" 再重启。")
            .define("scanSeedVersion", "0");

    // 数值换算：部分 mod 的暴击属性以整数百分点计数（如灾变 +75 点 = +75%），迁移到本模组倍率属性前需折算。
    // 只有列在这里的源属性才折算：目标数值 = 源数值 x 乘数。未列出的属性按 1.0 原样迁移。
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RATE_CONVERSION = BUILDER
            .comment("暴击率源属性的数值换算（格式 modid:path=乘数，可多项）。仅列出的属性参与折算：迁移到归一接收方时 目标数值 = 源数值 x 乘数。\n"
                    + "归一接收方是倍率量纲（1.0 = 100%），若源属性同为倍率量纲则不要配置。\n"
                    + "示例：某 mod +1 点表示 +1%（即 0.01），写 mymod:crit_chance=0.01。默认空。")
            .defineListAllowEmpty("rateConversion", List.of(), o -> o instanceof String);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> DAMAGE_CONVERSION = BUILDER
            .comment("暴击伤害源属性的数值换算（格式 modid:path=乘数，可多项）。仅列出的属性参与折算：迁移到归一接收方时 目标数值 = 源数值 x 乘数。\n"
                    + "归一接收方是倍率量纲（1.5 = 150%），若源属性同为倍率量纲则不要配置。\n"
                    + "示例：灾变 cataclysm:additional_critical_damage 是整数百分点（+75 点 = +75%），按 0.01 折算成 +0.75。\n"
                    + "默认含 cataclysm:additional_critical_damage=0.01。")
            .defineListAllowEmpty("damageConversion", List.of(
                    "cataclysm:additional_critical_damage=0.01"), o -> o instanceof String);

    // 归一接收方（暴击率）：收集到的暴击率修饰符全迁到该属性，默认本模组自有 crit_chance
    // 该属性自身会从源集合排除（不归 0、不收集），因此「排除某属性」等价「设为归一接收方」
    private static final ModConfigSpec.ConfigValue<String> NORMALIZE_RATE_ID = BUILDER
            .comment("归一接收方（暴击率）：收集到的暴击率修饰符迁移到该属性上。默认 fusedcritical:crit_chance。")
            .define("normalizeRateId", FusedCritical.MODID + ":crit_chance");

    // 归一接收方（暴击伤害）：同上，默认本模组自有 crit_damage
    private static final ModConfigSpec.ConfigValue<String> NORMALIZE_DAMAGE_ID = BUILDER
            .comment("归一接收方（暴击伤害）：收集到的暴击伤害修饰符迁移到该属性上。默认 fusedcritical:crit_damage。")
            .define("normalizeDamageId", FusedCritical.MODID + ":crit_damage");

    // 不显示伤害数字的实体 id 列表（modid:path）：命中这些实体时本模组不发伤害数字包。默认屏蔽 Dummmmmmy 靶子，因其数字由自身显示
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DAMAGE_NUMBER_HIDDEN_ENTITY_IDS = BUILDER
            .comment("不显示伤害数字的实体 id 列表（格式 modid:path）。命中时本模组不发送伤害数字包。默认仅 Dummmmmmy 靶子（dummmmmmy:target_dummy）。")
            .defineListAllowEmpty("damageNumberHiddenEntityIds", List.of(
                    "dummmmmmy:target_dummy"), o -> o instanceof String);

    // L2 Tabs 属性面板里暴击率/暴击伤害的显示方式（仅加载 L2 Tabs 时生效）
    private static final ModConfigSpec.EnumValue<L2TabsDisplayMode> L2TABS_CRIT_DISPLAY_MODE = BUILDER
            .comment("L2 Tabs 属性面板中暴击率/暴击伤害的显示方式（仅在加载 L2 Tabs 时生效）：\n"
                    + "REPLACE = 用归一化接收方属性替换面板原有的两行；\n"
                    + "APPEND = 保留面板原有两行，并在其后追加归一化接收方属性；\n"
                    + "NONE = 不改动面板，保持 L2 Tabs / l2damagetracker 原样。")
            .defineEnum("l2TabsCritDisplayMode", L2TabsDisplayMode.REPLACE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // 归一化源（配置快照）
    public static Set<String> critRateIds;
    public static Set<String> critDamageIds;
    public static String scanSeedVersion;
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

    // 归一化源解析缓存
    private static Set<Attribute> cachedCritRate;
    private static Set<Attribute> cachedCritDamage;
    // 「有没有任何源属性」缓存：为假时整条归一化链路直接短路
    private static Boolean cachedHasAnySource;

    // 数值换算解析缓存
    private static Map<Attribute, Double> cachedRateConversion;
    private static Map<Attribute, Double> cachedDamageConversion;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        reloadFromValues();
    }

    // 从配置值刷新静态快照并清空解析缓存。ConfigSeeder 写入扫描结果后也会调一次
    public static void reloadFromValues() {
        critRateIds = toSet(CRIT_RATE_IDS.get());
        critDamageIds = toSet(CRIT_DAMAGE_IDS.get());
        scanSeedVersion = SCAN_SEED_VERSION.get();
        normalizeRateId = NORMALIZE_RATE_ID.get();
        normalizeDamageId = NORMALIZE_DAMAGE_ID.get();
        damageNumberHiddenEntityIds = toSet(DAMAGE_NUMBER_HIDDEN_ENTITY_IDS.get());
        l2TabsCritDisplayMode = L2TABS_CRIT_DISPLAY_MODE.get();
        rateConversion = parseConversion(RATE_CONVERSION.get());
        damageConversion = parseConversion(DAMAGE_CONVERSION.get());
        cachedNormalizeRate = null;
        cachedNormalizeDamage = null;
        cachedCritRate = null;
        cachedCritDamage = null;
        cachedHasAnySource = null;
        cachedRateConversion = null;
        cachedDamageConversion = null;
    }

    // 当前扫描写入标记（直接读配置值，不依赖静态快照的刷新时序）
    public static String seedVersion() {
        return SCAN_SEED_VERSION.get();
    }

    // ConfigSeeder 专用：把扫描结果整体写进两个列表并打上已写入标记，然后刷新快照
    public static void writeScannedIds(final List<String> rate, final List<String> damage) {
        CRIT_RATE_IDS.set(List.copyOf(rate));
        CRIT_RATE_IDS.save();
        CRIT_DAMAGE_IDS.set(List.copyOf(damage));
        CRIT_DAMAGE_IDS.save();
        SCAN_SEED_VERSION.set("1");
        SCAN_SEED_VERSION.save();
        reloadFromValues();
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

    // 归一化源集合（暴击率）：只读配置列表，剔除两个归一接收方（否则会把自己归 0）
    public static Set<Attribute> critRate() {
        if (cachedCritRate == null) {
            cachedCritRate = withoutTargets(resolveAll(critRateIds));
        }
        return cachedCritRate;
    }

    // 归一化源集合（暴伤）：只读配置列表
    public static Set<Attribute> critDamage() {
        if (cachedCritDamage == null) {
            cachedCritDamage = withoutTargets(resolveAll(critDamageIds));
        }
        return cachedCritDamage;
    }

    // 是否配置了任何归一化源属性。为假时（整合包没装任何其它暴击 mod，或用户把两个列表都清空了）
    // CritNormalizer 整体短路，不做属性查询、不建迁移账本
    public static boolean hasAnySource() {
        if (cachedHasAnySource == null) {
            cachedHasAnySource = !critRate().isEmpty() || !critDamage().isEmpty();
        }
        return cachedHasAnySource;
    }

    // 该实体类型 id（modid:path）是否命中「不显示伤害数字」屏蔽列表
    public static boolean isDamageNumberHidden(final String entityTypeId) {
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

    // 剔除归一接收方：它们不该被归 0，也不该被当成源属性收集
    private static Set<Attribute> withoutTargets(final Set<Attribute> src) {
        final Set<Attribute> result = new LinkedHashSet<>(src);
        result.remove(normalizeRate());
        result.remove(normalizeDamage());
        return Set.copyOf(result);
    }

    private static Attribute resolve(final String id, final Attribute fallback) {
        if (id == null || id.isBlank()) {
            return fallback;
        }
        final ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return fallback;
        }
        final Attribute attr = BuiltInRegistries.ATTRIBUTE.get(rl);
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
            final Attribute attr = BuiltInRegistries.ATTRIBUTE.get(rl);
            if (attr != null) {
                result.put(attr, entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    // 解析 id 列表为属性集合，跳过非法/未注册的 id（列在配置里但没装对应 mod 时会走到这里）
    private static Set<Attribute> resolveAll(final Set<String> ids) {
        final Set<Attribute> result = new LinkedHashSet<>();
        if (ids == null) {
            return Set.of();
        }
        for (final String id : ids) {
            final ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) {
                LOGGER.debug("[fusedcritical] 配置里的属性 id 格式非法，已忽略：{}", id);
                continue;
            }
            final Attribute attr = BuiltInRegistries.ATTRIBUTE.get(rl);
            if (attr == null) {
                LOGGER.debug("[fusedcritical] 配置里的属性未注册（对应 mod 可能没装），已忽略：{}", id);
                continue;
            }
            result.add(attr);
        }
        return Set.copyOf(result);
    }

    private static Set<String> toSet(final List<? extends String> list) {
        return list == null ? Set.of() : Set.copyOf(list);
    }
}
