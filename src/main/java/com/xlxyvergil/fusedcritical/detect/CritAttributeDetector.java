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

// 属性注册表扫描器：只在首次生成配置时跑一次（见 ConfigSeeder），把候选属性按暴击率 / 暴伤分类成 id 列表。
// 运行期归一化完全不经过这里 —— 归一化只读配置里的两个列表（见 FusedConfig#critRate / #critDamage）。
//
// 名单只有一个来源：扫属性注册表。没有内置已知表 —— 扫不到就不产生条目，想要什么自己往配置里加。
// 命名规则是启发式的，所以这里做三件事：
//   1. 词根覆盖多种写法：crit / critical / cri_（myosotis）/ luck_hit（FlameChaseArtifacts 把幸运一击当暴击）；
//   2. 反向语义词直接排除：命中词根的防御向属性（"受到的暴伤""暴击抗性"）一旦被当成加成归 0，
//      修饰符会反向迁到接收方属性上，伤害数字错得毫无征兆、在配置里也看不出来。
//      这些词是配置项 scanBlacklistWords 的默认值（reduction / resist / taken …），用户可自行增删，清空 = 不再排除；
//   3. 一个属性只落进一个分类（先判率、再判伤害），避免同一属性被两边各归 0 一次、修饰符被搬两次。
// 扫描结果只是"写入配置的初始内容"，且只做并集（不删用户已有的条目），之后可在配置里任意增删。
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
