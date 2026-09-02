package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// capability 门面：定义、挂到实体、提供查询。
@Mod.EventBusSubscriber(modid = FusedCritical.MODID)
public final class CritNormalizeCapability {

    public static final Capability<ICritNormalizeCap> CRIT_NORMALIZE =
            CapabilityManager.get(new CapabilityToken<>() {});

    private CritNormalizeCapability() {}

    // 只对 LivingEntity 附上归一化状态
    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            final ResourceLocation key = ResourceLocation.tryParse(FusedCritical.MODID + ":crit_normalize");
            if (key != null) {
                event.addCapability(key, new Provider());
            }
        }
    }

    // 查询实体归一化状态，无能力时返回 null
    public static ICritNormalizeCap get(final LivingEntity entity) {
        return entity.getCapability(CRIT_NORMALIZE).orElse(null);
    }

    private static final class Provider implements ICapabilityProvider {
        private final ICritNormalizeCap cap = new CritNormalizeCap();
        private final LazyOptional<ICritNormalizeCap> opt = LazyOptional.of(() -> cap);

        @Override
        public <T> LazyOptional<T> getCapability(final Capability<T> capability, final Direction side) {
            return CRIT_NORMALIZE.orEmpty(capability, opt);
        }
    }
}
