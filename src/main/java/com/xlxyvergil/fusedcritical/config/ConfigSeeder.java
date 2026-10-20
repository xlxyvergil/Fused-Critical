package com.xlxyvergil.fusedcritical.config;

import com.mojang.logging.LogUtils;
import com.xlxyvergil.fusedcritical.detect.CritAttributeDetector;
import org.slf4j.Logger;

// 首次生成配置时把扫描出来的暴击属性写进 critRateIds / critDamageIds，之后永不自动改动。
//
// 判据是配置项 scanSeedVersion 的值（"0" = 还没写过），而不是"配置文件存不存在"：
// 这样从旧版本升级上来的配置文件也会自然补上这两个新键，并写入一次扫描结果。
// 一旦标记变成 "1"，无论配置文件是否被删、是否换了整合包，都不会再动这两个列表 —— 用户说了算。
public final class ConfigSeeder {

    private static final Logger LOGGER = LogUtils.getLogger();

    // scanSeedVersion 的未写入态
    private static final String NOT_SEEDED = "0";

    private ConfigSeeder() {}

    public static void seedIfNeeded() {
        try {
            if (!NOT_SEEDED.equals(FusedConfig.seedVersion())) {
                // 标记存在且已写入过：用户列表是权威，什么都别动
                return;
            }
            final CritAttributeDetector.Result scan = CritAttributeDetector.scan();
            if (scan.rate().isEmpty() && scan.damage().isEmpty()) {
                // 注册表里一个都没扫到（异常情况）：保留配置默认值，别把列表写空
                LOGGER.warn("[fusedcritical] 属性注册表里没扫到任何暴击属性，跳过自动写入，保留配置默认值");
                return;
            }
            FusedConfig.writeScannedIds(scan.rate(), scan.damage());
            LOGGER.info("[fusedcritical] 已把扫描到的暴击属性写入配置：暴击率 {} 项，暴伤 {} 项。之后该列表由你维护，不再自动改动",
                    scan.rate().size(), scan.damage().size());
        } catch (final RuntimeException e) {
            // 配置写入失败不该拖垮启动：保留默认值，把原因打进日志
            LOGGER.warn("[fusedcritical] 写入扫描结果失败，保留配置默认值", e);
        }
    }
}
