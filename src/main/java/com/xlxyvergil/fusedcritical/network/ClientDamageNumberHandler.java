package com.xlxyvergil.fusedcritical.network;

import com.xlxyvergil.fusedcritical.client.DamageNumberParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// 纯客户端：受击实体头顶生成伤害数字粒子，只经网络包桥接调用
// 攻击者为玩家则不限距离；否则仅当玩家距受击实体 64 格内才显示
@OnlyIn(Dist.CLIENT)
public final class ClientDamageNumberHandler {

    // 非玩家攻击时，玩家距受击实体超过该距离则不显示伤害数字
    private static final double NON_PLAYER_RANGE = 64.0D;

    private ClientDamageNumberHandler() {}

    static void handle(ClientboundDamageNumberPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        // 攻击者为玩家→不限距离；否则玩家距受击实体超过 64 格则忽略
        if (!packet.attackerIsPlayer()) {
            Player player = mc.player;
            if (player == null || player.distanceToSqr(entity) > NON_PLAYER_RANGE * NON_PLAYER_RANGE) {
                return;
            }
        }
        mc.particleEngine.add(new DamageNumberParticle(mc.level,
                entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                packet.amount(), packet.tier()));
    }
}
