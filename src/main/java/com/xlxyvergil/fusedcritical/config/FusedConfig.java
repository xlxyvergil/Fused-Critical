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

import java.util.List;
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

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<String> zeroRateIds;
    public static Set<String> zeroDamageIds;
    public static Set<String> blacklistRateIds;
    public static Set<String> blacklistDamageIds;
    public static String normalizeRateId;
    public static String normalizeDamageId;
    public static Set<String> damageNumberHiddenEntityIds;

    // 接收方解析缓存
    private static Attribute cachedNormalizeRate;
    private static Attribute cachedNormalizeDamage;

    // 归 0 集解析缓存
    private static Set<Attribute> cachedZeroRate;
    private static Set<Attribute> cachedZeroDamage;
    private static Set<Attribute> cachedBlacklistRate;
    private static Set<Attribute> cachedBlacklistDamage;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        zeroRateIds = Setcopy(ZERO_RATE_IDS.get());
        zeroDamageIds = Setcopy(ZERO_DAMAGE_IDS.get());
        blacklistRateIds = Setcopy(BLACKLIST_RATE_IDS.get());
        blacklistDamageIds = Setcopy(BLACKLIST_DAMAGE_IDS.get());
        normalizeRateId = NORMALIZE_RATE_ID.get();
        normalizeDamageId = NORMALIZE_DAMAGE_ID.get();
        damageNumberHiddenEntityIds = Setcopy(DAMAGE_NUMBER_HIDDEN_ENTITY_IDS.get());
        cachedNormalizeRate = null;
        cachedNormalizeDamage = null;
        cachedZeroRate = null;
        cachedZeroDamage = null;
        cachedBlacklistRate = null;
        cachedBlacklistDamage = null;
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
