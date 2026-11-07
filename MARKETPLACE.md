# JetBrains Marketplace 发布清单

## 发布前

1. Marketplace 发布者名称使用 `FIN Tools`，并与 `plugin.xml` 保持一致。
2. 在 Marketplace 创建插件条目，确认插件 ID 为 `com.fin.openmodel.preview`。
3. 将公开源码地址填写为 `https://github.com/xx18771007680/openmodel-preview`。
4. 准备 1280x800 或更大尺寸的真实 IDE 预览截图。
5. 更新 `CHANGELOG.md` 和 `pluginVersion`。
6. 不要把 Marketplace token、签名证书或私钥提交到仓库。

## 质量门

```bash
./gradlew test buildPlugin verifyPluginStructure verifyPluginProjectConfiguration
```

要求测试、结构校验和工程配置校验全部通过。安装包位于 `build/distributions/`。

## 免费发布口径

- License: Apache-2.0
- Pricing: Free
- Privacy: no model upload, no preview server, no analytics in version 0.1.0
- Supported IDE baseline: WebStorm 2026.3 / build 263+
