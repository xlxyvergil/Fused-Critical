package com.xlxyvergil.fusedcritical.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 服务端 -> 客户端伤害数字包：受击实体 id、真实扣血、颜色档位（0 白 / 1 橙 / >=2 红）。
// 仅打包用，渲染逻辑在 @OnlyIn 的 ClientDamageNumberHandler 里。
public final class ClientboundDamageNumberPacket {

    private final int entityId;
    // 护甲/吸收结算后的真实扣血量
    private final float amount;
    // 颜色档位（0 白 / 1 橙 / >=2 红）
    private final int tier;

    public ClientboundDamageNumberPacket(int entityId, float amount, int tier) {
        this.entityId = entityId;
        this.amount = amount;
        this.tier = tier;
    }

    public ClientboundDamageNumberPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.amount = buf.readFloat();
        this.tier = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeFloat(this.amount);
        buf.writeInt(this.tier);
    }

    public int entityId() {
        return this.entityId;
    }

    public float amount() {
        return this.amount;
    }

    public int tier() {
        return this.tier;
    }

    // 桥接到 @OnlyIn 的客户端处理，避免服务端加载客户端代码
    public static void handle(ClientboundDamageNumberPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDamageNumberHandler.handle(msg))
        );
        ctx.get().setPacketHandled(true);
    }
}
