package com.xlxyvergil.fusedcritical.config;

import com.mojang.logging.LogUtils;
import com.xlxyvergil.fusedcritical.detect.CritAttributeDetector;
import org.slf4j.Logger;

// 每次启动按属性注册表扫描结果覆写 critRateIds / critDamageIds。
// 不再判断「配置文件是不是刚生成」——Forge 在读配置时必然先把文件写出来，
// 那个判据在真实整合包里永远为假，扫描从来不会发生。
// 想长期手写维护两个列表：configure 里把 disableAutoScan 设为 true。
public final class ConfigSeeder {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ConfigSeeder() {}

    // 在 FMLCommonSetupEvent 的 enqueueWork 里调用一次（此时注册表已冻结、配置已加载）
    public static void seedIfNeeded() {
        if (FusedConfig.disableAutoScan) {
            LOGGER.debug("[fusedcritical] disableAutoScan = true，跳过自动扫描，保留配置里的 critRateIds / critDamageIds");
            return;
        }
        try {
            final CritAttributeDetector.Result scan = CritAttributeDetector.scan();
            if (scan.rate().isEmpty() && scan.damage().isEmpty()) {
                LOGGER.info("[fusedcritical] 属性注册表里没扫到暴击属性，critRateIds / critDamageIds 保持原样");
                return;
            }
            FusedConfig.writeScannedIds(scan.rate(), scan.damage());
            LOGGER.info("[fusedcritical] 已按扫描结果覆写暴击属性列表：暴击率 {} 项 {}，暴伤 {} 项 {}",
                    scan.rate().size(), scan.rate(), scan.damage().size(), scan.damage());
        } catch (final RuntimeException e) {
            LOGGER.warn("[fusedcritical] 写入扫描结果失败，保留现有配置", e);
        }
    }
}
