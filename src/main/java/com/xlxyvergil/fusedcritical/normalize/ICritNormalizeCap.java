package com.xlxyvergil.fusedcritical.normalize;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.Map;
import java.util.UUID;

// 实体级归一化状态（capability）：记录本模组迁到各接收方属性上的修饰符，下次只撤销这些。
@AutoRegisterCapability
public interface ICritNormalizeCap {

    // 接收方属性 -> (源属性 -> (源修饰符 UUID -> 接收方新 UUID))
    Map<Attribute, Map<Attribute, Map<UUID, UUID>>> migration();
}
