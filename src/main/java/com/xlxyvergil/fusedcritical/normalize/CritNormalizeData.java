package com.xlxyvergil.fusedcritical.normalize;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashMap;
import java.util.Map;

// 实体级归一化状态（Data Attachment）：记录本模组迁到各接收方属性上的修饰符，下次只撤销这些。
// 纯运行期状态，不需要序列化与同步（与原 Forge capability 行为一致）。
public final class CritNormalizeData {

    private final Map<Attribute, Map<Attribute, Map<ResourceLocation, ResourceLocation>>> migration = new HashMap<>();

    // migration() 返回可变 map，供 CritNormalizer 读写迁移记录
    public Map<Attribute, Map<Attribute, Map<ResourceLocation, ResourceLocation>>> migration() {
        return migration;
    }
}
