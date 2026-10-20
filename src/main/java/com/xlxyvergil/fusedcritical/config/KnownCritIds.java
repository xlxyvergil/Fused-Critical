package com.xlxyvergil.fusedcritical.config;

import java.util.List;

// 内置已知暴击属性表：作为 critRateIds / critDamageIds 的默认值，并在扫描时并入结果。
// 作用有二：
//   1. 扫描器万一没跑成（注册表为空等），默认值仍能保证老用户原先生效的属性继续归一化；
//   2. 命名规则是 heuristic，命中不了的（如 myosotis:cri_chance 不含 "crit"）靠这张表兜底。
// 未安装对应 mod 时这些 id 在解析阶段会被跳过，不会报错。
public final class KnownCritIds {

    private KnownCritIds() {}

    // 暴击率
    public static final List<String> RATE = List.of(
            "attributeslib:crit_chance",
            "confluence:crit_chance",
            "flame_chase_artifacts:luck_hit_chance",
            "l2damagetracker:crit_rate",
            "myosotis:cri_chance",
            "reshapes:critical_chance");

    // 暴击伤害
    public static final List<String> DAMAGE = List.of(
            "attributeslib:crit_damage",
            "cataclysm:additional_critical_damage",
            "flame_chase_artifacts:luck_hit_amplifier",
            "l2damagetracker:crit_damage",
            "myosotis:cri_damage",
            "reshapes:critical_damage");
}
