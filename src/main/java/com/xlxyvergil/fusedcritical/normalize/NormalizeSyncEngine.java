package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 同步引擎：在装备/效果/入世等时机「登记」待归一化实体，等该实体本 tick 的 tick() 跑完再真正归一化。
//
// 为什么不能在这些事件里直接归一化：1.21.1 中属性修饰符的写入晚于事件本身——
//   LivingEntity#collectEquipmentChanges 在槽位循环内先 post LivingEquipmentChangeEvent，
//   要等整个循环结束才把新装备的修饰符写进 AttributeMap；
//   MobEffectEvent.Added/Remove 也先于 onEffectAdded/onEffectRemoved（真正增删修饰符的地方）；
//   Curios 同样是先 post CurioChangeEvent 再改 attributeMap。
// 事件里立刻归一化只能读到上一状态，表现为「换上武器不生效，等下次装备变动才补上」。
//
// 推迟到本实体本 tick 的 tick() 之后消费，此时属性已是最终值，且仍在同一 tick 内、属性同步给客户端之前。
// 但玩家与非玩家实体的「tick() 结束」发生在不同事件：
//   - 非玩家实体：ServerLevel#tickNonPassenger 先 tick() 再立刻 post EntityTickEvent.Post，时机正确；
//   - 玩家：ServerPlayer#tick() 不会调用 super.tick()，其真正的 LivingEntity#tick()（含
//     detectEquipmentUpdates / refreshDirtyAttributes）在 ServerPlayer#doTick() 里、由
//     ServerGamePacketListenerImpl#tick() 调用；而 EntityTickEvent.Post 只在 ServerLevel 阶段触发，
//     此时玩家的装备/属性写入还没发生，会读到上一状态（表现为「切到主手的当 tick 不统合，
//     等武器被移除后才拿已消失的修饰符补算一次」）。
//     故玩家改在 PlayerTickEvent.Post 消费（Player#tick() 尾部，晚于 super.tick()），
//     并在 EntityTickEvent.Post 中跳过玩家，避免玩家被提前消费掉。
// 依然是纯事件驱动，不做 tick 轮询，开销只发生在真正发生变化的实体上。
@EventBusSubscriber(modid = FusedCritical.MODID)
public final class NormalizeSyncEngine {

    private NormalizeSyncEngine() {}

    // 待归一化实体。集成服下客户端与服务端分属不同线程，故用并发集合；
    // Entity 未重写 equals/hashCode，因此这里的去重语义等同按引用
    private static final Set<LivingEntity> PENDING = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            schedule(living);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        schedule(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        schedule(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        schedule(event.getEntity());
    }

    // 效果给属性修饰符，生效/失效/过期时重算
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        schedule(event.getEntity());
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        schedule(event.getEntity());
    }

    // 归一化凶手，并清理被击杀者的记录
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        CritNormalizer.clear(event.getEntity());
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            schedule(killer);
        }
    }

    // 登记过的非玩家实体在本 tick 的 tick() 结束后归一化，此时装备/效果对属性的写入已经落定。
    // 玩家不走这里：ServerPlayer 的 EntityTickEvent.Post 早于其真正的 LivingEntity#tick()，见类头注释。
    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!PENDING.isEmpty() && event.getEntity() instanceof LivingEntity living
                && !(living instanceof Player) && PENDING.remove(living)) {
            CritNormalizer.normalize(living);
        }
    }

    // 玩家在 Player#tick() 尾部归一化，晚于 super.tick() 里的 detectEquipmentUpdates / refreshDirtyAttributes
    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!PENDING.isEmpty() && PENDING.remove(event.getEntity())) {
            CritNormalizer.normalize(event.getEntity());
        }
    }

    // 离开世界的实体不会再 tick，及时摘掉登记，避免集合一直持有它
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            PENDING.remove(living);
        }
    }

    // 登记待归一化；同一 tick 内多次变化只会归一化一次
    public static void schedule(final LivingEntity entity) {
        PENDING.add(entity);
    }
}
