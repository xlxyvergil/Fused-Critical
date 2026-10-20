package com.xlxyvergil.fusedcritical.combat;

import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.config.FusedConfig;
import com.xlxyvergil.fusedcritical.network.NetworkHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;

// LivingIncomingDamageEvent 结算暴击，LivingDamageEvent.Post 显示伤害数字
// （1.21.1 把 Forge 的 LivingHurtEvent / LivingDamageEvent 拆成了 Incoming + DamageEvent.Pre/Post）。
// 暴击走 Warframe 分级：tier = floor(crit_chance)，小数部分以概率升一级，倍率 = 1 + tier x (crit_damage - 1)。
// 命中配置「不显示伤害数字」实体（默认 dummmmmmy:target_dummy）时只结算不显示，交给该实体自身处理。
@EventBusSubscriber(modid = FusedCritical.MODID)
public final class CritCombatHandler {

    // 攻击者不存在（无来源实体）时使用的默认暴击判定参数
    private static final double DEFAULT_CRIT_CHANCE = 0.2D;
    private static final double DEFAULT_CRIT_DAMAGE = 1.5D;

    // 已 roll 出的暴击等级，hurt 记录、damage 读取后移除，按是否真暴击着色
    private static final Map<Integer, Integer> PENDING_TIER = new HashMap<>();

    private CritCombatHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        double[] params = resolveParams(event);
        if (params == null) {
            return;
        }

        double critChance = params[0];
        double critDamage = params[1];

        // 命中「不显示伤害数字」实体（如假人）：只结算暴击伤害，不记录 tier、不发包
        LivingEntity target = event.getEntity();
        boolean hidden = isDamageNumberHidden(target);
        if (hidden) {
            double multiplier = multiplier(target, critChance, critDamage);
            if (multiplier > 1.0D + 1.0E-6D) {
                event.setAmount(event.getAmount() * (float) multiplier);
            }
            return;
        }

        int tier = tier(target, critChance);
        double multiplier = 1.0D + tier * (critDamage - 1.0D);

        if (multiplier > 1.0D + 1.0E-6D) {
            event.setAmount(event.getAmount() * (float) multiplier);
        }

        // amount 是护甲前的原始值，只在此结算；记录 tier 给伤害数字着色
        if (!target.level().isClientSide) {
            PENDING_TIER.put(target.getId(), tier);
        }
    }

    // 只显示不结算：newDamage 是护甲/吸收结算后的真实扣血，颜色用刚记录的 tier
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        // 服务端才显示；命中「不显示伤害数字」实体时跳过，由其自身显示
        if (target.level().isClientSide || isDamageNumberHidden(target)) {
            return;
        }
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity a ? a : null;
        NetworkHandler.sendDamageNumber(target, event.getNewDamage(), resolveTier(target, event), attacker);
    }

    // 优先用记录的 tier，没记录则按攻击者暴击率兜底
    private static int resolveTier(LivingEntity target, LivingDamageEvent.Post event) {
        Integer tier = PENDING_TIER.remove(target.getId());
        if (tier != null) {
            return tier;
        }
        return critColorTier(event);
    }

    // 兜底：按攻击者暴击率取档位，无攻击者/属性缺失按白
    private static int critColorTier(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            AttributeInstance chanceIns = instanceOf(attacker, FusedConfig.normalizeRate());
            if (chanceIns != null) {
                return (int) Math.floor(chanceIns.getValue());
            }
        }
        return 0;
    }

    // 命中「不显示伤害数字」屏蔽列表则不做本模组显示
    private static boolean isDamageNumberHidden(LivingEntity entity) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return type != null && FusedConfig.isDamageNumberHidden(type.toString());
    }

    // 计算暴击等级：tier = floor(crit_chance)，小数部分以概率升一级
    private static int tier(LivingEntity target, double critChance) {
        int tier = (int) Math.floor(critChance);
        double frac = critChance - tier;
        if (target.getRandom().nextDouble() < frac) {
            tier++;
        }
        return tier;
    }

    // 计算暴击倍率
    private static double multiplier(LivingEntity target, double critChance, double critDamage) {
        return 1.0D + tier(target, critChance) * (critDamage - 1.0D);
    }

    // 读取攻击者的暴击参数；无攻击者用默认值，属性未绑定返回 null
    private static double[] resolveParams(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            AttributeInstance chanceIns = instanceOf(attacker, FusedConfig.normalizeRate());
            AttributeInstance dmgIns = instanceOf(attacker, FusedConfig.normalizeDamage());
            if (chanceIns == null || dmgIns == null) {
                return null;
            }
            return new double[]{chanceIns.getValue(), dmgIns.getValue()};
        }
        return new double[]{DEFAULT_CRIT_CHANCE, DEFAULT_CRIT_DAMAGE};
    }

    // 取实体上已绑定的属性实例；1.21.1 的 getAttribute 内部是 computeIfAbsent，查不存在的属性会凭空建实例而不返回 null，
    // 所以先用 hasAttribute 判断（等价于 1.20.1 里 getAttribute 返回 null 的情况）
    private static AttributeInstance instanceOf(final LivingEntity entity, final Attribute attr) {
        if (attr == null) {
            return null;
        }
        final Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attr);
        return entity.getAttributes().hasAttribute(holder) ? entity.getAttribute(holder) : null;
    }
}
