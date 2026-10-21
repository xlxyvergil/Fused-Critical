# Fused Critical

把多个模组各自实现的「暴击率 / 暴击伤害」属性，合并成一套统一的暴击系统。

> 本分支对应 **Minecraft 1.21.1 / NeoForge 21.1.x**。1.20.1 / Forge 见 `main` 分支。

## 它做什么

1. **归零**：对配置里列出的源属性（别的模组的 `crit_chance` / `crit_damage` 等）加一个 `× -1` 的乘法修饰符（`ADD_MULTIPLIED_TOTAL`），把它们恒定为 0。
2. **迁移**：把这些源属性上的修饰符搬到本模组自己的两个属性上：
   - `fusedcritical:crit_chance` —— 暴击率，`1.0 = 100%`，默认 `0.05`
   - `fusedcritical:crit_damage` —— 暴击伤害倍率，默认 `1.5`

   两个属性用 NeoForge 的 `PercentageAttribute`，tooltip 与属性面板按百分比显示。
   迁移时按 `rateConversion` / `damageConversion` 折算量纲。例：某个模组的暴伤用整数百分点（`+75` 表示 `+75%`），就需要配 `modid:attr=0.01`，否则会按 `+75 倍` 迁过来。
3. **结算**：Warframe 式分级暴击
   ```
   tier   = floor(crit_chance)，小数部分按概率再升一级
   倍率    = 1 + tier × (crit_damage - 1)
   ```
4. **显示**：受击实体头顶漂出伤害数字，颜色按 tier 分档（0 白 / 1 橙 / ≥2 红）。

## 配置（`config/fusedcritical-common.toml`）

| 键 | 说明 |
|---|---|
| `critRateIds` | **需要归一化的暴击率属性 id 列表**（`modid:path`）。归一化只认这个列表。 |
| `critDamageIds` | 同上，暴击伤害。 |
| `scanSeedVersion` | 内部标记：`"0"` = 还没写入过扫描结果。**请勿手改**。 |
| `scanBlacklistWords` | 自动扫描黑名单（子串匹配）：path 含这些词的属性不被自动写入。默认 `reduction` / `resist` / `taken` / `received` / `immun` / `penalty` / `dampen` / `absorb`，清空即不再排除。只影响扫描，不影响你手动列进两个 id 列表的条目。 |
| `rateConversion` / `damageConversion` | 源属性的数值换算（`modid:path=乘数`）。整数百分点型的模组必须在这里配折算系数。 |
| `normalizeRateId` / `normalizeDamageId` | 迁移目标属性，默认本模组的两个属性。 |
| `damageNumberHiddenEntityIds` | 不显示伤害数字的实体 id（默认 Dummmmmmy 靶子，它自己会显示）。 |
| `l2TabsCritDisplayMode` | 装了 L2 Tabs 时，属性面板里暴击两行怎么显示：`REPLACE` / `APPEND` / `NONE`。 |

### 首次生成时的自动扫描

配置文件**第一次生成**时（或从旧版本升级、文件里还没有 `scanSeedVersion` 这个键时），模组会扫描属性注册表，把识别到的暴击属性按「暴击率 / 暴伤」分类**并入** `critRateIds` / `critDamageIds`，再把标记置为 `"1"`。

扫描会跳过 path 含 `scanBlacklistWords` 里任一关键词的属性（默认是 `reduction` / `taken` / `resist` 这类防御向词），避免把"受到的暴击伤害""暴击抗性"当成加成归零后反向搬走。

两个列表的默认值都是空的：名单只有「扫描结果」和「你自己的编辑」两个来源，没有内置已知表——扫不到就不产生任何条目。

之后**永不自动改动**这两个列表：

- 你删掉的条目不会被加回来
- 你手写的条目不会被扫描覆盖（并入只做并集）
- 新装了带暴击属性的模组，想纳入就手动加一行
- 确实想让扫描再跑一次，把 `scanSeedVersion` 改回 `"0"` 再重启（只增不删）

如果一个源属性都没扫到（整合包里没有其它暴击模组），两个列表保持为空、标记保持 `"0"`，归一化链路整体短路，不产生任何开销；下次启动会再扫一次，装了带暴击属性的模组就会自动纳入。

## 升级到 1.0.6（破坏性变更）

旧版本用 4 个键配合运行期自动扫描：`zeroRateIds` / `zeroDamageIds` / `blacklistRateIds` / `blacklistDamageIds`。1.0.6 起：

- 归一化**只读配置**，运行期不再扫描属性注册表
- 4 个旧键被 `critRateIds` / `critDamageIds` 取代；旧键不再被读取（留在文件里无害，可删）
- 黑名单概念取消——列表本身就是白名单，不想归一化的属性不写进去即可

升级后如果发现某些属性不再被归一化，把它们手动补进 `critRateIds` / `critDamageIds`。完整记录见 [CHANGELOG.md](CHANGELOG.md)。

## 可选兼容

| 模组 | 效果 | 未安装时 |
|---|---|---|
| Curios | 饰品穿脱后重新归一化（`CurioChangeEvent`） | 自动跳过 |
| L2 Tabs | 属性面板里的暴击两行替换 / 追加成归一后的属性 | 自动跳过 |

全部是软依赖：编译期 `compileOnly`，运行时用 `ModList.isLoaded(...)` 守卫，引用第三方类型的监听器做了类隔离，缺依赖时不会抛 `NoClassDefFoundError`。

> 1.21.1 线不需要 Apothic Attributes 兼容层：`PercentageAttribute` 是 NeoForge 自带的。

## 构建

需要 **JDK 21**：

```bash
./gradlew build          # 产物 build/libs/fusedcritical-1.0.6-1.21.1.jar
./gradlew runClient      # 开发客户端
./gradlew runServer      # 开发服务端（首次需改 run/eula.txt）
```

依赖来源：

| 依赖 | 来源 |
|---|---|
| Curios | 官方 maven `top.theillusivec4.curios:curios-neoforge`（版本见 `gradle.properties` 的 `curios_version`） |
| L2 Tabs / L2 Core | 随仓库提交的 `libs/*.jar`（这两个库没有可编程访问的公开坐标） |

所以 clone 下来直接 `./gradlew build` 即可，不需要额外准备 jar。CI 也在跑同一套流程（见 `.github/workflows/build.yml`）。

## 分支

| 分支 | 平台 | 构建 |
|---|---|---|
| `main` | Minecraft 1.20.1 / Forge 47.x | JDK 17 |
| `1.21.1` | Minecraft 1.21.1 / NeoForge 21.1.x | JDK 21 |
