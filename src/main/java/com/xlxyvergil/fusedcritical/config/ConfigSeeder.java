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

// 首次生成配置时把扫描出来的暴击属性并入 critRateIds / critDamageIds，之后永不自动改动。
//
// 判据：配置文件不存在，或存在但缺少当前版本应有的配置项（= 首次安装 / 从旧版本升级上来）。
// 只判断"键在不在"，不看值 —— 所以你把 critRateIds 里的内容清空，键还在，不会被重新扫描填回去。
// 一旦配置项齐全，之后永不自动改动：删掉的条目不会被加回来，手写的条目也不会被覆盖。
// 想让扫描再跑一次：删掉 config/fusedcritical-common.toml 再启动（其余配置项一并回到默认值）。
//
// 采样放在 mod 构造阶段、配置文件被读取之前：Forge 会在加载时把缺的键按默认值补进去并立刻存盘，
// 采晚了就永远看到"键都齐了"。补键只补缺的项，不会改你调过的值。
public final class ConfigSeeder {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Forge 给 COMMON 配置生成的文件名：<modid>-common.toml
    private static final String CONFIG_FILE_NAME = FusedCritical.MODID + "-common.toml";

    // 这一版配置"应该有的"顶层键。少任何一个 = 不是这一版生成的配置 → 补扫一次。
    // 以后新增配置键时记得加进来，老配置升级时才会补扫。
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

    // 构造函数里、配置被读取之前采样。null = 没采到（时序异常）→ 本趟不扫描，宁可不写也不乱写
    private static Boolean needsSeeding;

    private ConfigSeeder() {}

    // 必须在 registerConfig 之前调用
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

    // 逐个检查顶层键是否存在。只读原始文件：Forge 还没往里补键
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

    // 顶层键在 TOML 里写成行首的 `key =`；注释行（# 开头）里的同名文字不算
    private static boolean containsTopLevelKey(final String text, final String key) {
        return Pattern.compile("^[ \\t]*" + Pattern.quote(key) + "[ \\t]*=", Pattern.MULTILINE)
                .matcher(text)
                .find();
    }

    public static void seedIfNeeded() {
        if (needsSeeding == null) {
            // 没采样成功：不动配置
            return;
        }
        if (!needsSeeding) {
            // 配置项齐全：两个列表完全归用户
            LOGGER.debug("[fusedcritical] 配置项齐全，跳过自动扫描");
            return;
        }
        try {
            final CritAttributeDetector.Result scan = CritAttributeDetector.scan();
            if (scan.rate().isEmpty() && scan.damage().isEmpty()) {
                // 一个都没扫到（整合包里没有其它暴击模组）：什么都不写，列表保持为空
                LOGGER.info("[fusedcritical] 首次生成或升级配置：属性注册表里没扫到暴击属性，critRateIds / critDamageIds 保持为空");
                return;
            }
            FusedConfig.writeScannedIds(scan.rate(), scan.damage());
            LOGGER.info("[fusedcritical] 首次生成或升级配置：已并入扫描到的暴击属性 暴击率 {} 项 {}，暴伤 {} 项 {}。之后该列表由你维护，不再自动改动",
                    scan.rate().size(), scan.rate(), scan.damage().size(), scan.damage());
        } catch (final RuntimeException e) {
            // 配置写入失败不该拖垮启动：保留现有配置，把原因打进日志
            LOGGER.warn("[fusedcritical] 并入扫描结果失败，保留现有配置", e);
        }
    }
}
