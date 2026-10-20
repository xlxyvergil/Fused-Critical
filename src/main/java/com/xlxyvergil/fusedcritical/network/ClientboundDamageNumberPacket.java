package com.xlxyvergil.fusedcritical.network;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 服务端 -> 客户端伤害数字包：受击实体 id、真实扣血、颜色档位（0 白 / 1 橙 / >=2 红）。渲染逻辑在 @OnlyIn 的 ClientDamageNumberHandler 里。
public record ClientboundDamageNumberPacket(int entityId, float amount, int tier, boolean attackerIsPlayer)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundDamageNumberPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FusedCritical.MODID, "damage_number"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDamageNumberPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ClientboundDamageNumberPacket::entityId,
                    ByteBufCodecs.FLOAT, ClientboundDamageNumberPacket::amount,
                    ByteBufCodecs.VAR_INT, ClientboundDamageNumberPacket::tier,
                    ByteBufCodecs.BOOL, ClientboundDamageNumberPacket::attackerIsPlayer,
                    ClientboundDamageNumberPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 桥接到 @OnlyIn 的客户端处理：lambda 体在服务端不会被解析执行，避免服务端加载客户端代码
    public static void handle(ClientboundDamageNumberPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDamageNumberHandler.handle(payload));
    }
}
