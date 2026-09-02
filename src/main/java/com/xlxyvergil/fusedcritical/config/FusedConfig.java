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

// 配置项。常规扫描是自动的（按属性路径关键字），这里只在「绝对特殊情况」下手动补漏；默认空即可。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FusedConfig {

    private FusedConfig() {}

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 额外补充：按自动扫描规则没命中、但实际是暴击率的完整属性 id（如 attributeslib:crit_chance），只有特殊情况才填
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CRIT_RATE_IDS = BUILDER
            .comment("自动扫描漏掉、需要额外当作暴击率的完整属性 id（格式 modid:path），仅特殊情况填写。")
            .defineListAllowEmpty("critRateIds", List.of(), o -> o instanceof String);

    // 额外补充：自动扫描漏掉的暴击伤害属性完整 id
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CRIT_DAMAGE_IDS = BUILDER
            .comment("自动扫描漏掉、需要额外当作暴击伤害的完整属性 id（格式 modid:path），仅特殊情况填写。")
            .defineListAllowEmpty("critDamageIds", List.of(), o -> o instanceof String);

    // 归一接收方（暴击率）：所有被收集的暴击率修饰符都会迁移到该属性上，默认是本模组自有的 crit_chance。
    // 该属性自身会被排除出源集合（不归 0、不收集），因此「想排除某个暴击率属性」等价于「把它设为归一接收方」。
    private static final ForgeConfigSpec.ConfigValue<? extends String> NORMALIZE_RATE_ID = BUILDER
            .comment("归一接收方（暴击率）：收集到的暴击率修饰符迁移到该属性上。默认 fusedcritical:crit_chance，可改为其他 mod 的属性 id（modid:path）。")
            .define("normalizeRateId", FusedCritical.MODID + ":crit_chance");

    // 归一接收方（暴击伤害）：同上，默认是本模组自有的 crit_damage。
    private static final ForgeConfigSpec.ConfigValue<? extends String> NORMALIZE_DAMAGE_ID = BUILDER
            .comment("归一接收方（暴击伤害）：收集到的暴击伤害修饰符迁移到该属性上。默认 fusedcritical:crit_damage，可改为其他 mod 的属性 id（modid:path）。")
            .define("normalizeDamageId", FusedCritical.MODID + ":crit_damage");

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<String> critRateIds;
    public static Set<String> critDamageIds;
    public static String normalizeRateId;
    public static String normalizeDamageId;

    // 归一接收方解析缓存（懒加载，配置重载时在 onLoad 里清空）
    private static Attribute cachedNormalizeRate;
    private static Attribute cachedNormalizeDamage;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        critRateIds = Setcopy(CRIT_RATE_IDS.get());
        critDamageIds = Setcopy(CRIT_DAMAGE_IDS.get());
        normalizeRateId = NORMALIZE_RATE_ID.get();
        normalizeDamageId = NORMALIZE_DAMAGE_ID.get();
        cachedNormalizeRate = null;
        cachedNormalizeDamage = null;
    }

    // 解析归一接收方（暴击率）为实际 Attribute；id 为空/非法/未注册时回退到本模组自有属性
    public static Attribute normalizeRate() {
        if (cachedNormalizeRate == null) {
            cachedNormalizeRate = resolve(normalizeRateId, FusedAttributes.CRIT_CHANCE.get());
        }
        return cachedNormalizeRate;
    }

    // 解析归一接收方（暴击伤害）为实际 Attribute；id 为空/非法/未注册时回退到本模组自有属性
    public static Attribute normalizeDamage() {
        if (cachedNormalizeDamage == null) {
            cachedNormalizeDamage = resolve(normalizeDamageId, FusedAttributes.CRIT_DAMAGE.get());
        }
        return cachedNormalizeDamage;
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

    private static Set<String> Setcopy(final List<? extends String> list) {
        return list == null ? Set.of() : Set.copyOf(list);
    }
}
