# Fused Critical（统合暴击）设计方案

> 目标：把游戏内所有来源的「暴击率 / 暴击伤害」进行**归一化**，统一由本模组（`fusedcritical`）接管，消除不同模组之间暴击属性各自为政、标准不一的问题。

---

## 1. 目标与范围

- 扫描所有模组注册的、命名空间属性路径中包含 `crit_rate`/`crit_chance`（→ 归为暴击率）和 `crit_damage`/`crit_dmg`（→ 归为暴击伤害）的 **Forge 属性**。
- 将上述源属性的数值贡献**归 0**（消除各 mod 自身的暴击结算）。
- 把这些贡献**迁移**到本模组统一的 `fusedcritical:crit_chance` / `fusedcritical:crit_damage`。
- 本模组**接管战斗端的暴击结算**（掷暴击、乘伤害、播粒子），成为唯一暴击来源。

---

## 2. 已确认的事实（代码依据）

### 2.1 各模组暴击属性清单（均为真实 Forge `Attribute`）

| 模组 | 暴击率 ID | 暴击伤害 ID | 说明 |
|---|---|---|---|
| Apothic Attributes | `attributeslib:crit_chance` | `attributeslib:crit_damage` | `PercentBasedAttribute`（继承 `RangedAttribute`），default 0.05 / 1.5，max 10 / 100 |
| Confluence | `confluence:crit_chance` | （无 crit_damage，走 apoth 集成） | `RangedAttribute`，default 0，max 10，ADDITION |
| L2DamageTracker | `l2damagetracker:crit_rate` | `l2damagetracker:crit_damage` | `WrappedAttribute`（继承 `RangedAttribute`），range crit_rate 0~1 |
| L2Artifacts | 无独立属性 | 无独立属性 | 通过 stat 给 `l2damagetracker:crit_rate` / `crit_damage` 生成 **AttributeModifier** |
| Cataclysm | 无 | 无 | 不走暴击属性（用 damage_type） |

### 2.2 关键结论

- **L2Artifacts 的 `crit_rate_add`/`crit_damage_add` 就是标准 Forge `AttributeModifier`**，且作用在真实属性 `l2damagetracker:crit_rate` / `l2damagetracker:crit_damage` 上：
  - `ConfigGen.java`：`regStat(map, CR_ADD, L2DamageTracker.CRIT_RATE.get(), ADDITION, ...)`
  - `StatTypeConfig.getModifier()`：`builder.put(attr, new AttributeModifier(uuid, name, value, op))`
  - 生成的 `stat_types/crit_rate_add.json`：`"attr": "l2damagetracker:crit_rate", "op": "ADDITION"`
- 因此**扫描真实 Forge 属性即可覆盖 L2Artifacts 的暴击贡献**，无需为其单独做 adapter。
- **Confluence 集成注意**：存在 Apothic 时 `ApothicHelper` 会把 `confluence:crit_chance` 映射/桥接到 `attributeslib:crit_chance`，两者可能是同一来源，扫描时应去重。

### 2.3 属性 min/max 盘点

`FC_ZERO`（`MULTIPLY_TOTAL -1`）让结算值归 0，因此需确认各源 `min ≤ 0`。盘点点结果（均在 `ck` 内代码确认）：

| 属性 ID | default | min | max | 可安全归 0 |
|---|---|---|---|---|
| `attributeslib:crit_chance` | 0.05 | 0 | 10 | ✅ |
| `attributeslib:crit_damage` | 1.5 | 1.0 | 100 | ✅ |
| `confluence:crit_chance` | 0.0 | 0 | 10 | ✅ |
| `l2damagetracker:crit_rate` | 0 | 0 | 1 | ✅ |
| `l2damagetracker:crit_damage` | 0.5 | 0 | 1000 | ✅ |

> 结论：全部目标源可被 `FC_ZERO` 无条件安全归 0，**无需任何源级特殊处理**（`crit_damage` 钳到 1.0 即“无加成”，已天然中和）。

### 2.4 本模组自有属性（迁移与消费的落点）

- 机制：**把别的 mod 的暴击率/暴击伤害归 0**，再**把它们上面的暴击 AttributeModifier 桥接迁移到本模组自有属性**；最后由本模组自有属性统一结算。
- 属性定义（参考 `L2DamageTracker`，用普通 `RangedAttribute` + `setSyncable(true)`，无需 `WrappedAttribute`）：

| 属性 ID | 默认值 | min | max | 说明 |
|---|---|---|---|---|
| `fusedcritical:crit_chance` | 0.05 | 0 | 1 | 暴击率，1.0 = 100%（可配置溢出上限） |
| `fusedcritical:crit_damage` | 1.5 | 1.0 | 上限 | 暴击伤害，1.0 = 无加成，1.5 = 150% |

- 需通过 `EntityAttributeModificationEvent` 绑定到**所有实体类型**（遍历 `event.getTypes()`，与 L2 相同），使任意攻击者都拥有该属性。
- **归 0 时必须排除本模组自有属性**：检测源属性时按命名空间跳过 `fusedcritical:*`，绝不给自己加 `FC_ZERO`。只有外部 mod 的暴击属性才需要归 0。

### 2.5 归 0 对象与迁移分拣（问答澄清）

**哪些视为需要归 0** —— 扫描 `ForgeRegistries.ATTRIBUTES`，按属性路径命中且命名空间非 `fusedcritical`：

| 类别 | 路径关键词 | 具体命中 |
|---|---|---|
| 暴击率 | `crit_rate` / `crit_chance` | `attributeslib:crit_chance`、`confluence:crit_chance`、`l2damagetracker:crit_rate` |
| 暴击伤害 | `crit_damage` / `crit_dmg` | `attributeslib:crit_damage`、`l2damagetracker:crit_damage` |

**迁移如何分类** —— 枚举外部源属性的 modifier（剔除 FC_ZERO/FC_MIGRATE）后，按来源类别分拣到不同落点：

```text
来源是「暴击率类」    → 折算成 ADDITION 加到 fusedcritical:crit_chance
来源是「暴击伤害类」  → 折算成 ADDITION 加到 fusedcritical:crit_damage
```

（暴击率只进暴击率，暴击伤害只进暴击伤害，不混。）

---

## 3. 完整闭环

```text
[发现] 扫描 ForgeRegistries.ATTRIBUTES，按路径命中：
          crit_rate / crit_chance      → 暴击率
          crit_damage / crit_dmg       → 暴击伤害
   ↓（对每个玩家实体）
[捕获]   读取源属性「剔除我方归 0 标记 FC_ZERO 后」的数值   ← 必须在归 0 之前（或排除标记后）读取
[归 0]   给源属性添加 持久 AttributeModifier：operation=MULTIPLY_TOTAL, amount=-1.0, UUID=FC_ZERO
          → 源属性最终值恒为 0
[迁移]   把捕获值（×每源缩放）作为 ADDITION modifier 添加到 fusedcritical:crit_chance / crit_damage
[消费]   ★必须★ 本模组监听攻击事件，用自己的 crit_chance 掷暴击、用 crit_damage 乘伤害、播粒子
   ↑（换装/属性变化时重跑「捕获 → 迁移」）
```

> **重点：第 4 步「消费」不可省。** 各 mod 掷暴击读的是它们自己的属性，归 0 后它们永远不会触发暴击；如果本模组的属性无人读取，暴击就凭空消失。因此本模组必须自己成为唯一的暴击结算者。

---

## 4. 关键技术难点与对策

### 4.1 归 0 后读不到原值 → 枚举修饰符 + 屏蔽自家 UUID
`MULTIPLY_TOTAL = -1.0` 使最终值恒为 0，但 **modifier 列表仍在**。「捕获值」不能取 `AttributeInstance.getValue()`（那是 0），需**枚举各条修饰符、屏蔽我们加的 UUID、按 op 折叠重算**。无需 hack MC 内部。

#### (a) 两套硬编码 UUID（必须分开）
| 用途 | 加在 | UUID | operation | amount |
|---|---|---|---|---|
| 归 0 | 源属性上 | `FC_ZERO` | `MULTIPLY_TOTAL` | `-1.0` |
| 迁移 | 我们自己的属性上 | `FC_MIGRATE` | `ADDITION` | 换算后的贡献值 |

> 两者都是同 mod 硬编码 UUID，天然幂等（重复添加会替换旧值）。

#### (b) 桥接读取的屏蔽逻辑
```text
for mod in player.getAttribute(srcAttr).getModifiers():
    if mod.getId() in {FC_ZERO, FC_MIGRATE}:   # 屏蔽我们自己加的
        continue
    把 (op, amount) 喂进贡献计算
```
这样正好排除“我们增加的”算法，不会自干扰。

#### (c) 贡献换算公式（相对源默认值折叠乘法）
```text
srcBase = srcAttr.getDefaultValue()

contribution = (srcBase + Σ ADDITION)
              * (1 + Σ MULTIPLY_BASE)
              * (1 + Σ MULTIPLY_TOTAL[排除 FC_ZERO])   # 屏蔽后剩余
              - srcBase                                # 仅 BONUS 模式减去基础值

our modifier amount = contribution × scale             # scale 为每源缩放
```
> 关键：`MULTIPLY_BASE` / `MULTIPLY_TOTAL` 是**相对 base 的乘法**，折算成一条 `ADDITION` 时必须以**源属性默认值**为基准，不可原样当作加法搬移。这就是 `ContributionCalc` 的职责：枚举 + 屏蔽 UUID + 按 op 折叠 + 相对 base 换算。
>
> **BONUS 基准**：BONUS 模式统一以**源属性默认值**为减项（`value - srcBase`），只留下修饰符/装备带来的增益，不把源自身固有默认值（如 `crit_damage` 默认 1.5）一并迁走。这样装备无修饰符时贡献恒为 0。
>
> **钳制备注**：公式得到的是「未钳制的纯值」，不受 `sanitizeValue` 影响（取值用 `getBaseValue()` 重算而非 `getValue()`）。

### 4.2 各单位/刻度不一致
- Apothic `crit_chance` default 0.05（1.0 = 100%），`crit_damage` default 1.5（150%）。
- L2 `crit_rate` 范围 0~1（1.0 = 100%），`crit_damage` default 0.5。
- Confluence `crit_chance` 0~10。

需定义**本模组统一标度**，并给每个源提供**可选缩放系数**（配置项）。

### 4.3 换装/增益变化后迁移值过期
Forge 没有「属性变化后」的通用事件，需组合：
- 立即重算：`LivingEquipmentChangeEvent`、`PlayerChangedDimensionEvent`、`PlayerRespawnEvent`。
- 兜底：`LivingTickEvent` 节流（每 10~20 tick 校验源属性「签名」，变化才重算）。

### 4.4 反馈回环防护
- 源属性检测时**跳过 `fusedcritical:*`**。
- 本模组的迁移 modifier 只作用于自己的属性，绝不写回源属性。
- 重算源值时**永远排除 FC_ZERO**，避免 -1 抵消捕获值。
- 源属性 `Living.getAttribute(attr)` 可能为 `null`（属性未挂到该实体），必须判空。

### 4.5 源属性不一定挂在玩家身上
很多自定义属性需靠 `EntityAttributeModificationEvent.add(EntityType.PLAYER, attr)` 才存在（L2 即是）。需确保：
- 判空处理；
- 本模组自己的 `crit_chance` / `crit_damage` 正确注册到玩家。

---

## 5. 代码分层

```
com.xlxyvergil.fusedcritical
├── attribute/FusedAttributes.java         # 注册 crit_chance(0.05) / crit_damage(1.5)，RangedAttribute + setSyncable，EntityAttributeModificationEvent 绑所有实体类型；不设最大值（分级暴击需溢出）
├── detect/CritAttributeDetector.java      # 扫描注册表 → 源属性集合（跳过 fusedcritical:*）；读配置的额外源 id
├── normalize/ContributionCalc.java        # 读取「剔除 FC_ZERO 后的源 modifier」数据
├── normalize/CritNormalizer.java          # 单实体：读取源修饰符数据 → 新建「临时」修饰符挂到自有属性 → 给源加 FC_ZERO 归 0（transient，不写档，死亡自动清，事件驱动重建）
├── normalize/NormalizeSyncEngine.java     # 事件驱动的同步引擎（加入世界/换装/重生/换维度/药水增减/击杀）
├── normalize/FusedModifierIds.java        # 硬编码 UUID：FC_ZERO（归 0）、FC_MIGRATE
├── config/FusedConfig.java                # 配置项：critRateIds / critDamageIds（手动补漏）、normalizeRateId / normalizeDamageId（归一接收方，默认本模组自有）
├── combat/CritCombatHandler.java          # ★LivingHurtEvent 统一结算：Warframe 分级暴击（tier=floor(rate)，frac 概率升级，倍率=1+tier×(dmg-1)），覆盖近战/投射物/法术；结算后服务端发包显示伤害数字并喂 DuMmmMmmy 假人★
├── network/NetworkHandler.java            # 建通道 + 注册消息；server 侧向追踪者发包
├── network/ClientboundDamageNumberPacket.java  # 服务端→客户端伤害数字包（实体 id、结算值、暴击等级）
├── client/DamageNumberParticle.java       # 客户端文本粒子：受击方头顶显示数字，按等级着色（0 白 / 1 橙 / >=2 红）
├── compat/curios/CuriosCompat.java        # Curios 可选依赖守卫门面（ModList.isLoaded）
├── compat/curios/CuriosCompatEvents.java  # 隔离的 CurioChangeEvent 监听（确认 Curios 存在才注册）
└── compat/dummmmmmy/DummmmmmyCompat.java  # DuMmmMmmy 可选依赖守卫门面；经反射喂其假人 moist(Entity,float)，让假人按本 mod 暴击规则显示
```

---

## 6. 配置项设计（FusedConfig）

| 配置 | 说明 | 默认 |
|---|---|---|
| `critRateIds` | 自动扫描漏掉、需额外当作暴击率的完整属性 id（`modid:path`），绝对特殊才填 | 空 |
| `critDamageIds` | 自动扫描漏掉、需额外当作暴击伤害的完整属性 id | 空 |
| `normalizeRateId` | 归一接收方（暴击率）：收集到的暴击率修饰符迁移到该属性；可改为其他 mod 的属性 id | `fusedcritical:crit_chance` |
| `normalizeDamageId` | 归一接收方（暴击伤害）：收集到的暴击伤害修饰符迁移到该属性；可改为其他 mod 的属性 id | `fusedcritical:crit_damage` |

> 常规识别全靠 `CritAttributeDetector` 按属性路径关键字自动扫描，配置项只做「特殊情况手动补漏」，默认保持空即可。
> 归一接收方会被 `CritAttributeDetector` 排除出源集合（不归 0、不收集、避免自迁移），因此「想排除某个暴击属性」等价于「把它设为归一接收方」，无需额外黑名单。

---

## 7. 分阶段落地

- **P1（MVP）**：仅覆盖纯 Forge 属性（Apothic + Confluence + L2DamageTracker，含 L2Artifacts 经由 L2 属性产生的贡献）；仅玩家实体；单位定为「1.0 = 100%」；实现 发现 → 捕获 → 归 0 → 迁移 → 消费 全链路。
- **P2**：非 Forge 属性体系（如 L2Artifacts 自身的 stat 层若独立计算）经配置登记或专用 adapter 接入，验证是否需要额外处理。
- **P3**：迁移模式（TOTAL / BONUS）与每源缩放调优；加黑名单防误伤（避免归 0 路径恰含 crit 的非暴击属性）；异常兜底与日志。

---

## 8. 设计决策（已敲定）

1. **统一标度**：暴击率 `1.0 = 100%`（默认 0.05，溢出可配置）；暴击伤害为「倍率」`1.0 = 无加成`（默认 1.5）。✅
2. **迁移内容**：只迁 `BONUS`（装备/增益带来的修饰符加成，**不含**源属性默认值）。✅
3. **v1 覆盖范围**：先只支持纯 Forge 属性（Apothic + Confluence + L2DamageTracker，含 L2Artifacts 经由 L2 属性产生的贡献），其余后续再加。✅
4. **消费端范围**：接管所有攻击类型（近战 + 投射物 + 魔法），挂 `LivingHurtEvent`。✅
