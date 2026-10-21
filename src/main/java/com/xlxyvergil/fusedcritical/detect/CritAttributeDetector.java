package com.xlxyvergil.fusedcritical.detect;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

// 只在首次生成配置时扫描一次属性注册表（见 ConfigSeeder），按暴击率 / 暴伤分类成 id 列表。
// 运行期归一化不经过这里，只读 FusedConfig#critRate / #critDamage。
// 词根 crit / cri_ / luck_hit 命中后再看量纲词：chance / rate 归暴击率，damage / amplifier 归暴伤，
// 一个属性只进一个分类；命中 scanBlacklistWords 的跳过。结果只并入，不删用户已有条目。
public final class CritAttributeDetector {

    private CritAttributeDetector() {}

    // 暴击族词根
    private static final String[] CRIT_STEMS = {"crit", "cri_", "luck_hit", "luckhit"};

    // 暴击率词
    private static final String[] RATE_WORDS = {"chance", "rate", "prob"};

    // 暴击伤害词
    private static final String[] DAMAGE_WORDS = {"damage", "dmg", "amplifier", "multiplier", "mult", "bonus"};

    public static Result scan() {
        final SortedSet<String> rate = new TreeSet<>();
        final SortedSet<String> damage = new TreeSet<>();
        for (final Attribute attr : ForgeRegistries.ATTRIBUTES) {
            final ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attr);
            // 跳过本模组自有属性（归 0 扫到自己会自噬）
            if (key == null || FusedCritical.MODID.equals(key.getNamespace())) {
                continue;
            }
            final String path = key.getPath().toLowerCase(Locale.ROOT);
            if (!hasAny(path, CRIT_STEMS) || FusedConfig.isScanBlacklisted(path)) {
                continue;
            }
            if (hasAny(path, RATE_WORDS)) {
                rate.add(key.toString());
            } else if (hasAny(path, DAMAGE_WORDS)) {
                damage.add(key.toString());
            }
        }
        return new Result(List.copyOf(rate), List.copyOf(damage));
    }

    private static boolean hasAny(final String path, final String[] words) {
        for (final String word : words) {
            if (path.contains(word)) {
                return true;
            }
        }
        return false;
    }

    // 扫描结果：两个已排序、可直接写进配置的 id 列表
    public record Result(List<String> rate, List<String> damage) {
    }
}
