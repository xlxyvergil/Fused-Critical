package com.xlxyvergil.fusedcritical.normalize;

import java.util.UUID;

// 本模组硬编码修饰符 UUID：
// FC_ZERO 加在外部源属性上，MULTIPLY_TOTAL = -1.0，把源属性归 0；
// FC_MIGRATE 加在本模组自有属性上，ADDITION，承接迁移过来的暴击贡献。固定 UUID，重复添加会替换旧值。
public final class FusedModifierIds {

    private FusedModifierIds() {}

    // 归 0 用：作用在外部源属性上，MULTIPLY_TOTAL = -1.0
    public static final UUID FC_ZERO = UUID.fromString("84c4ec83-c249-4aa3-9db1-71ac125ca935");

    // 迁移用：作用在本模组自有属性上，ADDITION
    public static final UUID FC_MIGRATE = UUID.fromString("67df630e-2b0e-4115-bd88-ce623715eadc");
}
