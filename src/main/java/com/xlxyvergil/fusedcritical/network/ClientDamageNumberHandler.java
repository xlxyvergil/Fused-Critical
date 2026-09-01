package com.xlxyvergil.fusedcritical.network;

import com.xlxyvergil.fusedcritical.client.DamageNumberParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// 纯客户端处理器：在受击实体头顶生成伤害数字粒子。
// 标注 @OnlyIn(Dist.CLIENT)，且只通过 ClientboundDamageNumberPacket#handle 里的
// DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...) 桥接调用，保证服务端永远不会加载/执行本类。
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
