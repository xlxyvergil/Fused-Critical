package com.xlxyvergil.fusedcritical.combat;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.attribute.FusedAttributes;
import com.xlxyvergil.fusedcritical.compat.dummmmmmy.DummmmmmyCompat;
import com.xlxyvergil.fusedcritical.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

// 消费端：监听 LivingHurtEvent，用本模组自有的暴击率/暴击伤害统一结算暴击。
// LivingHurtEvent 对任意伤害类型都会触发，所以近战/投射物/法术都能覆盖。
// 暴击模型用 Warframe 分级暴击：暴击率 1.0 = 100% 且允许溢出；暴击等级 tier = floor(crit_chance)，
// 小数部分以 frac 的概率升一级；该次倍率 = 1 + tier x (crit_damage - 1)。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID)
public final class CritCombatHandler {

    // 攻击者不存在（无来源实体）时使用的默认暴击判定参数
    private static final double DEFAULT_CRIT_CHANCE = 0.2D;
    private static final double DEFAULT_CRIT_DAMAGE = 1.5D;

    private CritCombatHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        double[] params = resolveParams(event);
        if (params == null) {
            return;
        }

        double critChance = params[0];
        double critDamage = params[1];

        // Warframe 分级暴击：算本次命中的暴击等级
        int tier = (int) Math.floor(critChance);
        double frac = critChance - tier;
        if (event.getEntity().getRandom().nextDouble() < frac) {
            tier++;
        }

        // 实际倍率 = 1 + tier x (crit_damage - 1)
        double multiplier = 1.0D + tier * (critDamage - 1.0D);

        // 有暴击才改伤害，没有就把等级归 0（消息里只用于区分普通/一级橙/二级及以上红）
        if (multiplier > 1.0D + 1.0E-6D) {
            event.setAmount(event.getAmount() * (float) multiplier);
        } else {
            tier = 0;
        }

        // 服务端才向所有追踪者发包显示伤害数字（在单机里集成服务器也满足 !isClientSide）。
        boolean serverSide = !event.getEntity().level().isClientSide && event.getAmount() != 0.0F;
        if (isDuMmmMmmyDummy(event.getEntity())) {
            // DuMmmMmmy 假人：伤害数字由它自己显示（我们不发自己的数字包，避免双重数字/颜色冲突）。
            // 但只要本次命中了我们的统合暴击，就把暴击倍率喂给 DuMmmMmmy，
            // 让它在显示数字时按暴击规则着色/显示倍率。
            if (serverSide && multiplier > 1.0D + 1.0E-6D
                    && event.getSource().getEntity() instanceof LivingEntity attacker) {
                DummmmmmyCompat.tryFeedCrit(event.getEntity(), attacker, (float) multiplier);
            }
        } else if (serverSide) {
            NetworkHandler.sendDamageNumber(event.getEntity(), event.getAmount(), tier);
        }
    }

    // 判断受击目标是否为 DuMmmMmmy 假人。直接用实体类型的注册名判断，不依赖对 DuMmmMmmy 的编译期依赖。
    private static boolean isDuMmmMmmyDummy(LivingEntity entity) {
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return type != null && "dummmmmmy".equals(type.getNamespace()) && "target_dummy".equals(type.getPath());
    }

    // 解析本次伤害的暴击参数 [critChance, critDamage]。
    // 攻击者存在就读其自有属性；属性没挂到实体（未绑定类型）则返回 null（无暴击）。
    // 攻击者不存在（无来源实体）就用默认 0.2 / 1.5。
    private static double[] resolveParams(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            AttributeInstance chanceIns = attacker.getAttribute(FusedAttributes.CRIT_CHANCE.get());
            AttributeInstance dmgIns = attacker.getAttribute(FusedAttributes.CRIT_DAMAGE.get());
            if (chanceIns == null || dmgIns == null) {
                return null;
            }
            return new double[]{chanceIns.getValue(), dmgIns.getValue()};
        }
        return new double[]{DEFAULT_CRIT_CHANCE, DEFAULT_CRIT_DAMAGE};
    }
}
