package com.xlxyvergil.fusedcritical.normalize;

import com.xlxyvergil.fusedcritical.FusedCritical;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Data Attachment 门面：声明、提供查询。
// 1.21.1 已没有 Forge capability / AttachCapabilitiesEvent，实体附加数据一律走 NeoForge Data Attachment。
public final class CritNormalizeAttachments {

    private CritNormalizeAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FusedCritical.MODID);

    // 只对需要归一化的 LivingEntity 惰性创建，无需序列化 / copyOnDeath / 网络同步
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CritNormalizeData>> CRIT_NORMALIZE =
            ATTACHMENTS.register("crit_normalize",
                    () -> AttachmentType.builder(CritNormalizeData::new).build());

    // 查询实体归一化状态（不存在时按默认构造创建）
    public static CritNormalizeData get(final LivingEntity entity) {
        return entity.getData(CRIT_NORMALIZE.get());
    }
}
