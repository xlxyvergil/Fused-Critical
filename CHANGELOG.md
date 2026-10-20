# 更新记录

## 1.0.6

### 破坏性变更：暴击属性改为完全由配置驱动

- 归一化不再在运行期扫描属性注册表，只读取配置里的 `critRateIds` / `critDamageIds`
- 移除 `zeroRateIds`、`zeroDamageIds`、`blacklistRateIds`、`blacklistDamageIds` 四个配置键，
  由 `critRateIds` / `critDamageIds` 取代（列表自身即白名单，不再需要黑名单）
- 新增 `scanSeedVersion`：首次生成配置（或从旧版本升级、文件里还没有这个键）时扫描注册表，
  把结果写入两个列表一次，之后永不自动改动

**迁移**：升级后若某些属性不再被归一化，手动补进 `critRateIds` / `critDamageIds` 即可。
旧文件里的 4 个旧键已不再被读取，可以直接删除。

### 其他

- 新增 `KnownCritIds` 内置已知表：作为两个列表的默认值，并在扫描时并入结果（命名规则漏判时兜底），
  保证升级用户的原有属性不丢
- 扫描规则扩展：支持 `crit` / `cri_`（如 `myosotis:cri_chance`）/ `luck_hit`（幸运一击当暴击）等多种命名
- 扫描新增反向语义词排除（`reduction` / `taken` / `resist` / `immun` 等），
  避免把"受到的暴击伤害""暴击抗性"这类属性当成加成归零并搬走
- 一个属性只会被归入「暴击率」或「暴伤」其中一个分类，修掉旧逻辑里同一属性可能同时进两个集合、
  被归零两次、修饰符被搬两份的隐患
- 配置里没有任何源属性时，归一化整体短路：不查属性实例、不为实体建迁移账本
- 配置写入与 Curios 监听注册移到 `FMLCommonSetupEvent` 的同步队列（`enqueueWork`），
  不再在并行加载线程上做这两件事
- 配置里的属性 id 解析失败 / 未注册时记 DEBUG 日志（列了但没装对应模组的情况）
- 版本号统一为 1.0.6

## 1.0.4 及更早

此版本之前没有维护更新记录。
