package com.xlxyvergil.fusedcritical.compat.attributeslib;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.ModList;

// Apothic Attributes（attributeslib）可选依赖守卫：只用 ModList.isLoaded 判断，不直接引用 attributeslib 类型。
// 引用 IFormattableAttribute 的属性实现隔离在 PercentRangedAttribute，仅在确认 attributeslib 存在时创建，避免缺失时抛 NoClassDefFoundError。
public final class AttributesLibCompat {

    public static final String MODID = "attributeslib";

    private AttributesLibCompat() {}

    // attributeslib 是否已安装
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    // attributeslib 存在时创建带百分比格式化的属性（1.0 = 100%），否则退回普通 RangedAttribute
    public static Attribute create(String descriptionId, double defaultValue, double min, double max) {
        if (isLoaded()) {
            return PercentRangedAttribute.create(descriptionId, defaultValue, min, max);
        }
        return new RangedAttribute(descriptionId, defaultValue, min, max);
    }
}
