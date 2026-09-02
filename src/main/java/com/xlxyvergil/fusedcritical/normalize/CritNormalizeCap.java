package com.xlxyvergil.fusedcritical.normalize;

import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// migration() 返回可变 map，供 CritNormalizer 读写迁移记录
public final class CritNormalizeCap implements ICritNormalizeCap {

    private final Map<Attribute, Map<Attribute, Map<UUID, UUID>>> migration = new HashMap<>();

    @Override
    public Map<Attribute, Map<Attribute, Map<UUID, UUID>>> migration() {
        return migration;
    }
}
