# Fused Critical

把多个模组各自实现的「暴击率 / 暴击伤害」属性，合并成一套统一的暴击系统。

> 本分支对应 **Minecraft 1.20.1 / Forge**。1.21.1 / NeoForge 见 `1.21.1` 分支。

## 它做什么

1. **归零**：对配置里列出的源属性（别的模组的 `crit_chance` / `crit_damage` 等）加一个 `× -1` 的乘法修饰符，把它们恒定为 0。
2. **迁移**：把这些源属性上的修饰符搬到本模组自己的两个属性上：
   - `fusedcritical:crit_chance` —— 暴击率，`1.0 = 100%`，默认 `0.05`
   - `fusedcritical:crit_damage` —— 暴击伤害倍率，默认 `1.5`

   迁移时按 `rateConversion` / `damageConversion` 折算量纲。例：某个模组的暴伤用整数百分点（`+75` 表示 `+75%`），就需要配 `modid:attr=0.01`，否则会按 `+75 倍` 迁过来。
3. **结算**：Warframe 式分级暴击
   ```
   tier   = floor(crit_chance)，小数部分按概率再升一级
   倍率    = 1 + tier × (crit_damage - 1)
   ```
4. **显示**：受击实体头顶漂出伤害数字，颜色按 tier 分档（0 白 / 1 橙 / ≥2 红）。
   显示总开关 `damageNumberEnabled` **默认关闭**——不发送也不渲染任何伤害数字，交给其它伤害显示模组接管；
   想用本模组自己的数字就把它设成 `true`。开关只拦显示，暴击结算与属性归一化照常。

## 配置（`config/fusedcritical-common.toml`）

| 键 | 说明 |
|---|---|
| `critRateIds` | **需要归一化的暴击率属性 id 列表**（`modid:path`）。归一化只认这个列表。默认**每次启动**按扫描结果覆写。 |
| `critDamageIds` | 同上，暴击伤害。 |
| `disableAutoScan` | **关闭自动扫描**，默认 `false`。`false` = 每次启动扫描属性注册表并覆写上面两个列表；`true` = 完全不扫描、不碰这两个列表，按你手写的内容归一化。 |
| `scanBlacklistWords` | 自动扫描黑名单（子串匹配）：path 含这些词的属性不被自动写入。默认 `reduction` / `resist` / `taken` / `received` / `immun` / `penalty` / `dampen` / `absorb`，清空即不再排除。只影响扫描，不影响你手动列进两个 id 列表的条目。 |
| `rateConversion` / `damageConversion` | 源属性的数值换算（`modid:path=乘数`）。整数百分点型的模组必须在这里配折算系数。 |
| `normalizeRateId` / `normalizeDamageId` | 迁移目标属性，默认本模组的两个属性。 |
| `damageNumberEnabled` | 伤害数字总开关，默认 `false`（关闭）：不发送也不渲染任何伤害数字，交给其它伤害显示模组接管。只拦显示，暴击结算不受影响。 |
| `damageNumberHiddenEntityIds` | 不显示伤害数字的实体 id（默认 Dummmmmmy 靶子，它自己会显示）。总开关打开时才生效。 |
| `l2TabsCritDisplayMode` | 装了 L2 Tabs 时，属性面板里暴击两行怎么显示：`REPLACE` / `APPEND` / `NONE`。 |

### 启动时的自动扫描（`disableAutoScan`）

每次启动（`COMMON_SETUP` 阶段，属性注册表已冻结）都会扫描属性注册表，把识别到的暴击属性按「暴击率 / 暴伤」分类，**覆写** `critRateIds` / `critDamageIds`。

- 覆写是**整体替换**，不是并集：你手写的条目会被扫描结果冲掉，删掉的条目会被扫回来
- 想让某个属性不进名单：把它加进 `scanBlacklistWords`；想让整张名单完全手写：把 `disableAutoScan` 设成 `true`
- `disableAutoScan = true` 时**不扫描、不写文件**，两个列表完全按你的内容生效
- 扫不到任何暴击属性时（整合包里没有其它暴击模组）列表保持原样，不写入空列表
- 以前那套「判断配置项齐不齐 / 文件在不在」的首次生成判据已经删掉：Forge 读配置时必然先把文件写出来，那个判据在真实整合包里永远为假，扫描根本不会发生——这正是 1.0.8 及以前"列表一直为空"的原因

扫描会跳过 path 含 `scanBlacklistWords` 里任一关键词的属性（默认是 `reduction` / `taken` / `resist` 这类防御向词），避免把"受到的暴击伤害""暴击抗性"当成加成归零后反向搬走。

两个列表没有内置已知表：名单只有「扫描结果」和「你自己手写」两个来源。

如果一个源属性都没扫到（整合包里没有其它暴击模组），两个列表保持为空，归一化链路整体短路，不产生任何开销。

### 归一化什么时候跑

纯事件驱动，不轮询：`EntityJoinLevelEvent`（实体进世界）、装备变更、重生、跨维度、效果增删、以及生物死亡时各触发一次。

装备和物品给的属性修饰符**不是随实体一起到位的**：`LivingEntity#tick` → `detectEquipmentUpdates` 在实体出生后的第一个 tick 才把它们挂到属性实例上，而且这条路径不发 `LivingEquipmentChangeEvent`（那个只在 `setItemSlot` 里发）。所以登录后如果要让新装备参与迁移，**切换一次主手或动一下装备**即可，那次事件会把当前全部修饰符准确搬过去。归 0（`× -1`）与顺序无关，不受这个时序影响。

## 升级到 1.0.6（破坏性变更）

旧版本用 4 个键配合运行期自动扫描：`zeroRateIds` / `zeroDamageIds` / `blacklistRateIds` / `blacklistDamageIds`。1.0.6 起：

- 归一化**只读配置**，运行期不再扫描属性注册表
- 4 个旧键被 `critRateIds` / `critDamageIds` 取代；旧键不再被读取（留在文件里无害，可删）
- 黑名单概念取消——列表本身就是白名单，不想归一化的属性不写进去即可

升级后如果发现某些属性不再被归一化，把它们手动补进 `critRateIds` / `critDamageIds`。完整记录见 [CHANGELOG.md](CHANGELOG.md)。

## 可选兼容

| 模组 | 效果 | 未安装时 |
|---|---|---|
| Curios | 饰品穿脱后重新归一化 | 自动跳过 |
| Apothic Attributes (`attributeslib`) | 本模组的两个属性按百分比显示（`IFormattableAttribute`） | 退回普通 `RangedAttribute` |
| L2 Tabs | 属性面板里的暴击两行替换 / 追加成归一后的属性 | 自动跳过 |

全部是软依赖：编译期 `compileOnly`，运行时用 `ModList.isLoaded(...)` 守卫，引用第三方类型监听器的类都做了隔离，缺依赖时不会抛 `NoClassDefFoundError`。

## 构建

需要 **JDK 17**：

```bash
./gradlew build          # 产物 build/libs/fusedcritical-1.0.6.jar
./gradlew runClient      # 开发客户端
./gradlew runServer      # 开发服务端（首次需改 run/eula.txt）
```

可选依赖的 jar 放在 `libs/`（该目录不进仓库，见 `libs` 相关说明）：

- `l2tabs-0.3.3.jar`、`l2library-2.4.23-pre1.jar`、`l2serial-1.2.2.jar`（L2 Tabs 及其编译期依赖）
- `ApothicAttributes-1.20.1-1.3.7.jar`（百分比显示）
- Curios 走 CurseForge maven 坐标（见 `build.gradle`），不需要本地 jar

## 分支

| 分支 | 平台 | 构建 |
|---|---|---|
| `main` | Minecraft 1.20.1 / Forge 47.x | JDK 17 |
| `1.21.1` | Minecraft 1.21.1 / NeoForge 21.1.x | JDK 21 |
