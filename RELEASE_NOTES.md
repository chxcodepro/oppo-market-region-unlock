# v1.0.0

LSPosed 模块：解除 OPPO 软件商店 `com.heytap.market` 的「服务国家/地区」切换限制。

## 支持版本

- OPPO 软件商店 `26.7.0_CN`（versionCode 260700）
- OPPO 软件商店 `26.10.2_CN`（versionCode 261002）

## 安装

1. 设备需要 root + LSPosed。
2. 安装 `OppoMarketRegionUnlock-1.0.apk`。
3. 打开 LSPosed 管理器 → 模块 → 启用「OPPO 商店地区解锁」。
4. 作用域勾选 **软件商店**（`com.heytap.market`），保存。
5. 强制停止软件商店，或重启手机。
6. 打开软件商店 → **我的 → 设置 → 服务与诊断 → 服务国家/地区**。

## 文件

- `OppoMarketRegionUnlock-1.0.apk`
- SHA-256：

```text
3fdcaab301eb9492b7eca00719323947bf796a2686cd9ed7f98e56b3f0439731
```

## 说明

- 不修改商店本体，不需要重签名。
- 需要 root + LSPosed；未 root 设备无法使用。
- 可选地区仍由商店服务端配置决定，本模块不注入额外国家。
- 仅用于个人设备上的研究与测试；可能违反 OPPO 服务条款，请自行承担风险；不提供任何担保。
- 源码见仓库 `module/`，构建方式见 `README.md`。
- 从源码自行构建时，如果使用不同的签名 key，需要先卸载旧版本再安装。
