package com.xlxyvergil.fusedcritical.attribute;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 本模组自有暴击属性：承接外部 mod 归 0 后迁移来的暴击修饰符并统一结算。
// 属性属 fusedcritical 命名空间，归 0 扫描时必须跳过。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FusedAttributes {

    private FusedAttributes() {}

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, FusedCritical.MODID);

    // 统一暴击率：1.0 = 100%，默认 0.05，最大 10240（分级暴击 tier = floor(值)）
    public static final RegistryObject<Attribute> CRIT_CHANCE = ATTRIBUTES.register(
            "crit_chance",
            () -> new RangedAttribute("attribute.fusedcritical.crit_chance", 0.05D, 0.0D, 10240)
                    .setSyncable(true));

    // 统一暴击伤害（倍率）：1.0 = 无加成，默认 1.5，最大 10240
    public static final RegistryObject<Attribute> CRIT_DAMAGE = ATTRIBUTES.register(
            "crit_damage",
            () -> new RangedAttribute("attribute.fusedcritical.crit_damage", 1.5D, 1.0D, 10240)
                    .setSyncable(true));

    // 把自有暴击属性挂到所有实体上，保证宠物/怪/假人也读得到（归一接收方若为外部 mod 属性则由该 mod 自行挂载）
    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        for (var type : event.getTypes()) {
            event.add(type, CRIT_CHANCE.get());
            event.add(type, CRIT_DAMAGE.get());
        }
    }
}
