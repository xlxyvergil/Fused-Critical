package com.xlxyvergil.fusedcritical.network;

import com.xlxyvergil.fusedcritical.client.DamageNumberParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// 纯客户端：受击实体头顶生成伤害数字粒子，只经网络包桥接调用
@OnlyIn(Dist.CLIENT)
public final class ClientDamageNumberHandler {

    private ClientDamageNumberHandler() {}

    static void handle(ClientboundDamageNumberPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(packet.entityId());
        if (entity != null) {
            mc.particleEngine.add(new DamageNumberParticle(mc.level,
                    entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                    packet.amount(), packet.tier()));
        }
    }
}
