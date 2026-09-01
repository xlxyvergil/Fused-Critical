package com.xlxyvergil.fusedcritical.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 服务端 -> 客户端「伤害数字」包：受击方实体 id、结算后的伤害值、本次命中的暴击等级。
// 客户端收到后在受击实体头顶生成一个文本粒子，按暴击等级着色。
// 本包在服务端与客户端都会被反序列化（服务端仅用于 setPacketHandled，不做实际渲染）；
// 真正的客户端渲染逻辑交给 @OnlyIn(Dist.CLIENT) 的 ClientDamageNumberHandler，
// 通过 DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...) 桥接，保证服务端永远不会加载客户端渲染代码。
public final class ClientboundDamageNumberPacket {

    private final int entityId;
    private final float amount;
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

    // 客户端处理：仅在客户端物理端通过 DistExecutor 桥接到 @OnlyIn 的 ClientDamageNumberHandler，
    // 避免服务端加载/执行任何客户端渲染代码。
    public static void handle(ClientboundDamageNumberPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDamageNumberHandler.handle(msg))
        );
        ctx.get().setPacketHandled(true);
    }
}
