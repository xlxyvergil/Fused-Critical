package com.xlxyvergil.fusedcritical.config;

import com.mojang.logging.LogUtils;
import com.xlxyvergil.fusedcritical.FusedCritical;
import com.xlxyvergil.fusedcritical.detect.CritAttributeDetector;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

// 首次生成配置时把扫描结果并入 critRateIds / critDamageIds，之后不再自动改动。
// 判据是配置文件缺不缺项：文件不存在，或缺 EXPECTED_KEYS 里的键（首次安装 / 旧版本升级）。
// 只判断键在不在，不看值，所以清空列表内容不会触发重新扫描。
public final class ConfigSeeder {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG_FILE_NAME = FusedCritical.MODID + "-common.toml";

    // 这一版配置应有的顶层键，缺任何一个就补扫一次。新增配置键时加进来
    private static final String[] EXPECTED_KEYS = {
            "critRateIds",
            "critDamageIds",
            "scanBlacklistWords",
            "rateConversion",
            "damageConversion",
            "normalizeRateId",
            "normalizeDamageId",
            "damageNumberHiddenEntityIds",
            "l2TabsCritDisplayMode"};

    // null = 采样失败，本次不扫描
    private static Boolean needsSeeding;

    private ConfigSeeder() {}

    // 必须在 registerConfig 之前调用：Forge 读取配置时会把缺的键补上并存盘
    public static void captureConfigStateBeforeLoad() {
        try {
            final Path file = configFile();
            needsSeeding = !Files.exists(file) || !hasAllExpectedKeys(file);
        } catch (final RuntimeException e) {
            needsSeeding = null;
            LOGGER.warn("[fusedcritical] 无法读取配置文件状态，本次跳过自动扫描", e);
        }
    }

    private static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
    }

    private static boolean hasAllExpectedKeys(final Path file) throws RuntimeException {
        final String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (final java.io.IOException e) {
            throw new IllegalStateException("读取配置文件失败: " + file, e);
        }
        for (final String key : EXPECTED_KEYS) {
            if (!containsTopLevelKey(text, key)) {
                return false;
            }
        }
        return true;
    }

    // 只认行首的 key =，注释里的同名文字不算
    private static boolean containsTopLevelKey(final String text, final String key) {
        return Pattern.compile("^[ \\t]*" + Pattern.quote(key) + "[ \\t]*=", Pattern.MULTILINE)
                .matcher(text)
                .find();
    }

    public static void seedIfNeeded() {
        if (needsSeeding == null) {
            return;
        }
        if (!needsSeeding) {
            LOGGER.debug("[fusedcritical] 配置项齐全，跳过自动扫描");
            return;
        }
        try {
            final CritAttributeDetector.Result scan = CritAttributeDetector.scan();
            if (scan.rate().isEmpty() && scan.damage().isEmpty()) {
                LOGGER.info("[fusedcritical] 首次生成或升级配置：属性注册表里没扫到暴击属性，critRateIds / critDamageIds 保持为空");
                return;
            }
            FusedConfig.writeScannedIds(scan.rate(), scan.damage());
            LOGGER.info("[fusedcritical] 首次生成或升级配置：已并入扫描到的暴击属性 暴击率 {} 项 {}，暴伤 {} 项 {}",
                    scan.rate().size(), scan.rate(), scan.damage().size(), scan.damage());
        } catch (final RuntimeException e) {
            LOGGER.warn("[fusedcritical] 并入扫描结果失败，保留现有配置", e);
        }
    }
}
