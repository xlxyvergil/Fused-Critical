package com.xlxyvergil.fusedcritical.config;

import com.mojang.logging.LogUtils;
import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.attribute.FusedAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

// 配置：哪些暴击率 / 暴伤属性要被归 0，修饰符迁到哪个接收方属性上。
// 归一化只读 critRateIds / critDamageIds，运行期不扫描注册表。
// 两个列表默认每次启动由 ConfigSeeder 按注册表扫描结果覆写；disableAutoScan = true 时完全不碰它们。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FusedConfig {

    private FusedConfig() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 自动扫描黑名单默认值：防御向 / 减伤向词根，命中即不自动写入
    private static final List<String> DEFAULT_SCAN_BLACKLIST = List.of(
            "reduction", "resist", "taken", "received", "immun", "penalty", "dampen", "absorb");

    // 归一化源（暴击率）：唯一权威列表。默认每次启动按扫描结果覆写；disableAutoScan = true 时由你手写维护
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CRIT_RATE_IDS = BUILDER
            .comment("需要归一化的暴击率属性 id 列表（格式 modid:path）。列出的属性被归 0，",
                    "其修饰符迁移到 normalizeRateId 指定的属性上。",
                    "默认每次启动都会按属性注册表扫描结果覆写本列表（手写条目会被清掉）。",
                    "想手写维护：把 disableAutoScan 设为 true。删掉某项即不再归一化该属性。")
            .defineListAllowEmpty("critRateIds", List.of(), o -> o instanceof String);

    // 归一化源（暴伤）：同上
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CRIT_DAMAGE_IDS = BUILDER
            .comment("需要归一化的暴击伤害属性 id 列表（格式 modid:path），语义同 critRateIds。",
                    "默认每次启动都会按扫描结果覆写本列表。")
            .defineListAllowEmpty("critDamageIds", List.of(), o -> o instanceof String);

    // 自动扫描总开关：true 时不扫描注册表、不改动上面两个列表，完全按手写内容归一化
    private static final ForgeConfigSpec.BooleanValue DISABLE_AUTO_SCAN = BUILDER
            .comment("是否关闭自动扫描：true 时启动不扫描属性注册表，也不改动 critRateIds / critDamageIds，",
                    "完全按你手写的内容归一化。",
                    "默认 false：每次启动都扫描注册表，并用扫描结果覆写上面两个列表（手写条目会被清掉）。",
                    "装了新 mod 想让列表自动跟上就保持默认；想长期手写维护就设为 true。")
            .define("disableAutoScan", false);

    // 自动扫描黑名单（子串匹配）：path 含这些词的属性不会被自动写进上面两个列表
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCAN_BLACKLIST_WORDS = BUILDER
            .comment("自动扫描黑名单（子串匹配）：属性 id 的 path 含任一关键词就不被自动写入。",
                    "默认 reduction / resist / taken / received / immun / penalty / dampen / absorb，",
                    "用来避开「受到的暴伤」「暴击抗性」这类防御向属性。",
                    "清空即不排除任何词；只影响自动扫描，不影响你手动写进两个列表的条目。")
            .defineListAllowEmpty("scanBlacklistWords", DEFAULT_SCAN_BLACKLIST, o -> o instanceof String);

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

    // 归一接收方（暴击率）：收集到的暴击率修饰符全迁到该属性，默认本模组自有 crit_chance
    // 该属性自身会从源集合排除（不归 0、不收集），因此「排除某属性」等价「设为归一接收方」
    private static final ForgeConfigSpec.ConfigValue<? extends String> NORMALIZE_RATE_ID = BUILDER
            .comment("归一接收方（暴击率）：收集到的暴击率修饰符迁移到该属性上。默认 fusedcritical:crit_chance。")
            .define("normalizeRateId", FusedCritical.MODID + ":crit_chance");

    // 归一接收方（暴击伤害）：同上，默认本模组自有 crit_damage
    private static final ForgeConfigSpec.ConfigValue<? extends String> NORMALIZE_DAMAGE_ID = BUILDER
            .comment("归一接收方（暴击伤害）：收集到的暴击伤害修饰符迁移到该属性上。默认 fusedcritical:crit_damage。")
            .define("normalizeDamageId", FusedCritical.MODID + ":crit_damage");

    // 伤害数字总开关：默认关闭。关闭时本模组既不发送也不渲染任何伤害数字，交给其它伤害显示模组接管；
    // 只影响显示，暴击结算与属性归一化照常进行
    private static final ForgeConfigSpec.BooleanValue DAMAGE_NUMBER_ENABLED = BUILDER
            .comment("是否显示本模组自带的伤害数字。",
                    "默认 false（关闭）：不发送也不渲染任何伤害数字，交给你另外安装的伤害显示模组接管。",
                    "只影响显示；暴击结算与属性归一化不受影响。",
                    "设为 true 恢复本模组自己的伤害数字。")
            .define("damageNumberEnabled", false);

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

    // 归一化源（配置快照）
    public static Set<String> critRateIds;
    public static Set<String> critDamageIds;
    // 自动扫描黑名单（已小写、已去空白）
    public static Set<String> scanBlacklistWords;
    public static String normalizeRateId;
    public static String normalizeDamageId;
    public static Set<String> damageNumberHiddenEntityIds;
    // 伤害数字总开关（配置快照，默认 false）
    public static boolean damageNumberEnabled;
    // 自动扫描总开关（配置快照，默认 false = 每次启动扫描并覆写两个 id 列表）
    public static boolean disableAutoScan;
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
        scanBlacklistWords = toLowerSet(SCAN_BLACKLIST_WORDS.get());
        normalizeRateId = NORMALIZE_RATE_ID.get();
        normalizeDamageId = NORMALIZE_DAMAGE_ID.get();
        damageNumberHiddenEntityIds = toSet(DAMAGE_NUMBER_HIDDEN_ENTITY_IDS.get());
        damageNumberEnabled = DAMAGE_NUMBER_ENABLED.get();
        disableAutoScan = DISABLE_AUTO_SCAN.get();
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

    // ConfigSeeder 专用：用扫描结果覆写两个列表（不是并集），然后刷新快照
    public static void writeScannedIds(final List<String> rate, final List<String> damage) {
        CRIT_RATE_IDS.set(sortedCopy(rate));
        CRIT_RATE_IDS.save();
        CRIT_DAMAGE_IDS.set(sortedCopy(damage));
        CRIT_DAMAGE_IDS.save();
        reloadFromValues();
    }

    // 扫描结果直接覆写：排序后写入，和旧内容无关
    private static List<String> sortedCopy(final List<String> scanned) {
        final SortedSet<String> sorted = new TreeSet<>();
        if (scanned != null) {
            sorted.addAll(scanned);
        }
        return List.copyOf(sorted);
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

    // 伤害数字总开关：关闭时本模组不发送、不渲染任何伤害数字
    public static boolean isDamageNumberEnabled() {
        return damageNumberEnabled;
    }

    // 该实体类型 id（modid:path）是否命中「不显示伤害数字」屏蔽列表
    public static boolean isDamageNumberHidden(final String entityTypeId) {
        return damageNumberHiddenEntityIds != null && damageNumberHiddenEntityIds.contains(entityTypeId);
    }

    // path（已转小写）是否命中自动扫描黑名单
    public static boolean isScanBlacklisted(final String lowerPath) {
        // 配置未加载时退回默认黑名单
        final Collection<String> words = scanBlacklistWords != null ? scanBlacklistWords : DEFAULT_SCAN_BLACKLIST;
        for (final String word : words) {
            if (!word.isEmpty() && lowerPath.contains(word)) {
                return true;
            }
        }
        return false;
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
            final Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
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

    // 词表归一化：去首尾空白 + 转小写 + 丢掉空项（空项会 contains 命中一切，必须剔掉）
    private static Set<String> toLowerSet(final List<? extends String> list) {
        if (list == null) {
            return Set.of();
        }
        final Set<String> result = new LinkedHashSet<>();
        for (final String raw : list) {
            if (raw == null) {
                continue;
            }
            final String word = raw.trim().toLowerCase(Locale.ROOT);
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        return Set.copyOf(result);
    }
}
