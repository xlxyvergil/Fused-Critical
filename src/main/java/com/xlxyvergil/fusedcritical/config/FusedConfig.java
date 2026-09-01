package com.xlxyvergil.fusedcritical.config;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

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

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<String> critRateIds;
    public static Set<String> critDamageIds;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        critRateIds = Setcopy(CRIT_RATE_IDS.get());
        critDamageIds = Setcopy(CRIT_DAMAGE_IDS.get());
    }

    private static Set<String> Setcopy(final List<? extends String> list) {
        return list == null ? Set.of() : Set.copyOf(list);
    }
}
