# OPPO 软件商店 · 服务国家/地区 解锁（LSPosed 模块）

用于解除 OPPO 软件商店 `com.heytap.market` 中
「我的 → 设置 / 服务与诊断 → 服务国家/地区」的切换限制。

> 仅用于自己设备上的研究与测试。请遵守 OPPO 的服务条款和当地法律法规。

## 支持版本

| 版本 | versionCode | 状态 |
| --- | --- | --- |
| `26.7.0_CN` | 260700 | 静态分析 |
| `26.10.2_CN` | 261002 | 静态分析 + 设备实测版本 |

目标 APK 经过 R8 混淆，不同版本的类名/方法名会变化。模块用“候选名称 + 按返回类型动态解析”
的方式同时兼容上述版本；商店再升级后如果日志只有 `skip`，需要按新版本更新 hook 点。

## 解锁了哪些限制

| 作用 | 26.7.0_CN | 26.10.2_CN |
| --- | --- | --- |
| 设置项是否可用 | `a.a.a.otc/y45.isSupportRegionSwitch()` | `a.a.a.pqd/jr5.isSupportRegionSwitch()` |
| 同上（底层） | `RegionSwitchRepo.ލ()Z` | `RegionSwitchRepo.ދ()Z` |
| App 侧地区支持校验 | `a.a.a.ytc.ދ(String)Z` | `a.a.a.atd.އ/ވ(String)Z` |
| SDK 目标地区资格校验 | `mspsdk.regionv3.impl.g.ՠ(String)` | 同左（未变） |
| 风控结果解析 | `RegionRiskRepo.ؠ(RiskProcessResp) -> u3d` | 同左，返回 `l3e` |
| 异步风控入口 | `RegionRiskRepo.֏(List, callback)` | 同左（未变） |
| 海外 CTA 标志 | `a.a.a.asc.ԯ()Z` | `a.a.a.nod.ԯ()Z` |
| 空实现转发 | `a.a.a.y45.openRegionSelectionPage` | `a.a.a.jr5.openRegionSelectionPage` |

最终落盘仍由商店自身完成：
`RegionSwitchRepo.ޠ/ޣ` → `߿` → `߾` 写入当前地区、通知 SDK、更新相关状态。

## 目录结构

```text
.
├── module/
│   ├── AndroidManifest.xml
│   ├── assets/xposed_init
│   └── src/com/regionunlock/oppomarket/
│       ├── HookEntry.java
│       └── SettingsActivity.java
├── build.sh
└── README.md
```

仓库只包含模块源码，不包含：

- OPPO 软件商店本体 APK
- 反编译产物 / 分析数据
- 签名 keystore
- 构建输出 APK（APK 发布在 GitHub Releases）

## 构建

依赖：

- JDK 17+（推荐 17/21）
- Android SDK：build-tools + `platforms/android-36`（或 34/35）
- Python 3
- curl（首次下载 Xposed API 时使用）

```bash
bash build.sh
```

输出：

```text
dist/OppoMarketRegionUnlock-1.0.apk
```

可选环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `ANDROID_HOME` | 自动探测 | Android SDK 路径 |
| `XPOSED_API_JAR` | `libs/api-82.jar` | 不存在时自动下载 |
| `KEYSTORE` | `build/regionunlock.jks` | 不存在时自动生成 |
| `KEYSTORE_PASS` | `regionunlock123` | keystore 口令 |
| `KEY_PASS` | 同 `KEYSTORE_PASS` | key 口令 |
| `KEY_ALIAS` | `regionunlock` | key alias |

> 构建脚本生成的是开发用自签名证书。如果要覆盖安装已发布的 APK，必须使用同一个签名 key；
> 否则需要先卸载旧版本。

## 安装与使用

1. 手机已安装 **LSPosed** 并激活（需要 root）。
2. 安装 `dist/OppoMarketRegionUnlock-1.0.apk`（或 Releases 里的 APK）。
3. LSPosed 管理器 → 模块 → 启用「OPPO 商店地区解锁」。
4. 作用域勾选 **软件商店**（`com.heytap.market`），保存。
5. 强制停止软件商店，或重启手机。
6. 打开软件商店 → **我的 → 设置 → 服务与诊断 → 服务国家/地区**，即可切换。

### 验证

```bash
adb logcat -s Xposed | grep OppoRegionUnlock
```

26.10.2 上应看到类似：

```text
OppoRegionUnlock: hooked a.a.a.pqd#isSupportRegionSwitch
OppoRegionUnlock: hooked com.heytap.market.mine.cn.overseastravel.RegionSwitchRepo#...
OppoRegionUnlock: hooked a.a.a.atd#...
OppoRegionUnlock: hooked a.a.a.nod#...
OppoRegionUnlock: hooked SDK target-region check
OppoRegionUnlock: hooked risk parse (a.a.a.l3e)
OppoRegionUnlock: hooked async risk check
```

另一版本的候选会打印 `skip ...`，属于正常现象。

## 说明与限制

- 需要 root + LSPosed；未 root 设备无法使用。
- 不修改商店本体、不需要重签名，商店升级后只需更新模块的 hook 点。
- 可选国家仍来自商店服务端下发的地区配置；本模块不注入额外国家。
- 跳过地区风控/CTA 校验可能违反 OPPO 服务条款，请自行承担风险。
- 本项目不提供任何明示或暗示的担保。
