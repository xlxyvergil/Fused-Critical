package com.xlxyvergil.fusedcritical.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 服务端 -> 客户端伤害数字包：受击实体 id、真实扣血、颜色档位（0 白 / 1 橙 / >=2 红）。渲染逻辑在 @OnlyIn 的 ClientDamageNumberHandler 里。
public final class ClientboundDamageNumberPacket {

    private final int entityId;
    // 护甲/吸收结算后的真实扣血量
    private final float amount;
    // 颜色档位（0 白 / 1 橙 / >=2 红）
    private final int tier;
    // 造成伤害的实体是否为玩家：true 客户端不限距离显示；false 仅玩家距受击目标 64 格内显示
    private final boolean attackerIsPlayer;

    public ClientboundDamageNumberPacket(int entityId, float amount, int tier, boolean attackerIsPlayer) {
        this.entityId = entityId;
        this.amount = amount;
        this.tier = tier;
        this.attackerIsPlayer = attackerIsPlayer;
    }

    public ClientboundDamageNumberPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.amount = buf.readFloat();
        this.tier = buf.readInt();
        this.attackerIsPlayer = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeFloat(this.amount);
        buf.writeInt(this.tier);
        buf.writeBoolean(this.attackerIsPlayer);
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

    public boolean attackerIsPlayer() {
        return this.attackerIsPlayer;
    }

    // 桥接到 @OnlyIn 的客户端处理，避免服务端加载客户端代码
    public static void handle(ClientboundDamageNumberPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDamageNumberHandler.handle(msg))
        );
        ctx.get().setPacketHandled(true);
    }
}
