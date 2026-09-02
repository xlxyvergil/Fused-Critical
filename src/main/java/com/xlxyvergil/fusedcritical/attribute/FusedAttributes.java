package com.xlxyvergil.fusedcritical.attribute;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 本模组自有暴击属性：承接外部 mod 归 0 后迁移来的暴击修饰符并统一结算。
// 这两个属性属 fusedcritical 命名空间，归 0 扫描时必须跳过，不能给自己加 FC_ZERO。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FusedAttributes {

    private FusedAttributes() {}

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, FusedCritical.MODID);

    // 统一暴击率：1.0 = 100%，默认 0.05，不设最大值（分级暴击 tier = floor(值)）
    public static final RegistryObject<Attribute> CRIT_CHANCE = ATTRIBUTES.register(
            "crit_chance",
            () -> new RangedAttribute("attribute.fusedcritical.crit_chance", 0.05D, 0.0D, Double.MAX_VALUE)
                    .setSyncable(true));

    // 统一暴击伤害（倍率）：1.0 = 无加成，默认 1.5，不设最大值
    public static final RegistryObject<Attribute> CRIT_DAMAGE = ATTRIBUTES.register(
            "crit_damage",
            () -> new RangedAttribute("attribute.fusedcritical.crit_damage", 1.5D, 1.0D, Double.MAX_VALUE)
                    .setSyncable(true));

    // 把自有属性和配置指定的归一接收方挂到所有实体类型上，保证宠物/怪/假人也能读到。
    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        for (var type : event.getTypes()) {
            event.add(type, CRIT_CHANCE.get());
            event.add(type, CRIT_DAMAGE.get());
            event.add(type, FusedConfig.normalizeRate());
            event.add(type, FusedConfig.normalizeDamage());
        }
    }
}
