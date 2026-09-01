package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 同步引擎：在属性/装备可能变化的时机触发归一化。只做事件驱动，不做 tick 轮询（开销大）。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID)
public final class NormalizeSyncEngine {

    private NormalizeSyncEngine() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            CritNormalizer.normalize(living);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        CritNormalizer.normalize(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        CritNormalizer.normalize(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        CritNormalizer.normalize(event.getEntity());
    }

    // 药水/效果生效、失效、过期时，效果给的属性修饰符会挂上或卸下
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        CritNormalizer.normalize(event.getEntity());
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        CritNormalizer.normalize(event.getEntity());
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        CritNormalizer.normalize(event.getEntity());
    }

    // 击杀后动态提升属性的行为，影响的是凶手（攻击者），不是被击杀者
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            CritNormalizer.normalize(killer);
        }
    }
}
