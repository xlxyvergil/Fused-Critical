package com.xlxyvergil.fusedcritical.combat;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.compat.dummmmmmy.DummmmmmyCompat;
import com.xlxyvergil.fusedcritical.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

// LivingHurtEvent 结算暴击，LivingDamageEvent 显示伤害数字。
// 暴击走 Warframe 分级：tier = floor(crit_chance)，小数部分以概率升一级，倍率 = 1 + tier x (crit_damage - 1)。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID)
public final class CritCombatHandler {

    // 攻击者不存在（无来源实体）时使用的默认暴击判定参数
    private static final double DEFAULT_CRIT_CHANCE = 0.2D;
    private static final double DEFAULT_CRIT_DAMAGE = 1.5D;

    // 本次实际 roll 出的暴击等级，hurt 记录、damage 读取后移除，按是否真暴击着色
    private static final Map<Integer, Integer> PENDING_TIER = new HashMap<>();

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

        if (multiplier > 1.0D + 1.0E-6D) {
            event.setAmount(event.getAmount() * (float) multiplier);
        }

        // 这里的 amount 是护甲前的原始值，只在此结算；把 tier 记下来给 DamageEvent 着色
        LivingEntity target = event.getEntity();
        if (!target.level().isClientSide && !isDuMmmMmmyDummy(target)) {
            PENDING_TIER.put(target.getId(), tier);
        }

        // 假人只喂倍率，数字由它自己显示
        if (!target.level().isClientSide
                && isDuMmmMmmyDummy(target)
                && multiplier > 1.0D + 1.0E-6D
                && event.getSource().getEntity() instanceof LivingEntity attacker) {
            DummmmmmyCompat.tryFeedCrit(target, attacker, (float) multiplier);
        }
    }

    // 只显示不结算：DamageEvent 的 amount 是护甲结算后的真实扣血，颜色用刚记录的 tier
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        // 服务端才显示；假人自行显示，跳过避免重复
        if (target.level().isClientSide || isDuMmmMmmyDummy(target)) {
            return;
        }
        NetworkHandler.sendDamageNumber(target, event.getAmount(), resolveTier(target, event));
    }

    // 优先用记录的 tier，没记录则按攻击者暴击率兜底
    private static int resolveTier(LivingEntity target, LivingDamageEvent event) {
        Integer tier = PENDING_TIER.remove(target.getId());
        if (tier != null) {
            return tier;
        }
        return critColorTier(event);
    }

    // 兜底：按攻击者暴击率取档位，无攻击者/属性缺失按白
    private static int critColorTier(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            AttributeInstance chanceIns = attacker.getAttribute(FusedConfig.normalizeRate());
            if (chanceIns != null) {
                return (int) Math.floor(chanceIns.getValue());
            }
        }
        return 0;
    }

    // 按注册名判断假人，不依赖编译期引用
    private static boolean isDuMmmMmmyDummy(LivingEntity entity) {
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return type != null && "dummmmmmy".equals(type.getNamespace()) && "target_dummy".equals(type.getPath());
    }

    // 读取攻击者的暴击参数；无攻击者用默认值，属性未绑定返回 null
    private static double[] resolveParams(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            AttributeInstance chanceIns = attacker.getAttribute(FusedConfig.normalizeRate());
            AttributeInstance dmgIns = attacker.getAttribute(FusedConfig.normalizeDamage());
            if (chanceIns == null || dmgIns == null) {
                return null;
            }
            return new double[]{chanceIns.getValue(), dmgIns.getValue()};
        }
        return new double[]{DEFAULT_CRIT_CHANCE, DEFAULT_CRIT_DAMAGE};
    }
}
