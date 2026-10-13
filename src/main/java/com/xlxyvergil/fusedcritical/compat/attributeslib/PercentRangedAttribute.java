package com.xlxyvergil.fusedcritical.compat.attributeslib;

import dev.shadowsoffire.attributeslib.api.IFormattableAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// 显示层恒为百分比的属性（1.0 = 100%）。
// 本类直接引用 attributeslib 类型，只允许在 AttributesLibCompat#isLoaded() 为真后创建，避免缺失时抛 NoClassDefFoundError。
public class PercentRangedAttribute extends RangedAttribute implements IFormattableAttribute {

    public PercentRangedAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue, min, max);
    }

    // 工厂：返回类型刻意用 Attribute 而不是本类，这样 AttributesLibCompat 的字节码里不出现本类，
    // 未安装 attributeslib 时本类不会被验证/加载，守卫才真正生效。
    public static Attribute create(String descriptionId, double defaultValue, double min, double max) {
        return new PercentRangedAttribute(descriptionId, defaultValue, min, max);
    }

    @Override
    public MutableComponent toValueComponent(AttributeModifier.Operation op, double value, TooltipFlag flag) {
        return Component.translatable("attributeslib.value.percent", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value * 100.0D));
    }
}
