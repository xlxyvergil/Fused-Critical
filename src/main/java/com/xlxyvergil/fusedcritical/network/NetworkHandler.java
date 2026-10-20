package com.xlxyvergil.fusedcritical.network;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// 本 mod 的网络包注册与发送入口。
// NeoForge 1.21.1 用 payload 体系（CustomPacketPayload + StreamCodec）替代了 Forge 的 SimpleChannel。
public final class NetworkHandler {

    // 协议版本，与 PayloadRegistrar 一起决定客户端/服务端是否兼容
    private static final String PROTOCOL = "1";

    private NetworkHandler() {}

    // 注册所有网络包（由主类挂到 mod 事件总线，替代原先在 FMLCommonSetupEvent 里手动建通道）
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(
                ClientboundDamageNumberPacket.TYPE,
                ClientboundDamageNumberPacket.STREAM_CODEC,
                ClientboundDamageNumberPacket::handle);
    }

    // 服务端：向所有追踪该受击实体的客户端发送伤害数字。是否显示（限制距离）由客户端按攻击者身份判断，只能在服务端调用
    public static void sendDamageNumber(LivingEntity target, float amount, int tier, LivingEntity attacker) {
        boolean attackerIsPlayer = attacker instanceof Player;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new ClientboundDamageNumberPacket(target.getId(), amount, tier, attackerIsPlayer));
    }
}
