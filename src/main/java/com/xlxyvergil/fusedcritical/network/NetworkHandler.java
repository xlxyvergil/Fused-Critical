package com.xlxyvergil.fusedcritical.network;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

// 本 mod 的网络通道与发送入口。
// CHANNEL 用静态字段初始化，register() 在 FMLCommonSetupEvent 里调用，确保加载阶段就建好通道。
public final class NetworkHandler {

    private static final String PROTOCOL = "1";
    private static int nextId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(FusedCritical.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private NetworkHandler() {}

    // 注册所有网络包
    public static void register() {
        CHANNEL.messageBuilder(ClientboundDamageNumberPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundDamageNumberPacket::new)
                .encoder(ClientboundDamageNumberPacket::write)
                .consumerMainThread(ClientboundDamageNumberPacket::handle)
                .add();
    }

    // 服务端：向所有追踪该受击实体的客户端发送伤害数字，只能在服务端调用
    public static void sendDamageNumber(LivingEntity target, float amount, int tier) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new ClientboundDamageNumberPacket(target.getId(), amount, tier));
    }
}
