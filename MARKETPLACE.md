# JetBrains Marketplace 发布清单

可直接填写的英文上架文案见 [`docs/marketplace/listing.md`](docs/marketplace/listing.md)。真实 IDE 截图在完成验收后保存为 `docs/marketplace/openmodel-preview-1280x800.png`。

## 发布前

1. Marketplace 发布者名称使用 `FIN Tools`，并与 `plugin.xml` 保持一致。
2. 在 Marketplace 创建插件条目，确认插件 ID 为 `com.fin.openmodel.preview`。
3. 将公开源码地址填写为 `https://github.com/xx18771007680/openmodel-preview`。
4. 上传 `docs/marketplace/openmodel-preview-1280x800.png` 真实 IDE 预览截图。
5. 更新 `CHANGELOG.md` 和 `pluginVersion`。
6. 不要把 Marketplace token、签名证书、私钥或私钥密码提交到仓库。

## 本地签名

签名材料只保存在仓库外，密码由 macOS 钥匙串保管：

```bash
./scripts/setup_local_signing.command
./scripts/build_signed_plugin.command
```

第一个脚本只需执行一次；再次运行只检查现有配置，不会覆盖密钥。第二个脚本从钥匙串读取密码，在单次 Gradle 进程内注入 `CERTIFICATE_CHAIN`、`PRIVATE_KEY`、`PRIVATE_KEY_PASSWORD`，结束时立即清除环境变量。

生成的签名包位于 `build/distributions/`，名称以 `-signed.zip` 结尾。脚本会执行 JetBrains 的 `verifyPluginSignature` 并输出安装包 SHA-256。

后续版本可通过环境变量 `PUBLISH_TOKEN` 调用：

```bash
PUBLISH_TOKEN="..." ./gradlew publishPlugin
```

不要把真实 token 写进脚本、`gradle.properties`、Shell 配置或 CI 日志；正式自动发布时应改用 GitHub Actions Secret。

## 质量门

```bash
./gradlew test buildPlugin verifyPluginStructure verifyPluginProjectConfiguration
```

要求测试、结构校验和工程配置校验全部通过。安装包位于 `build/distributions/`。

## 首次提交顺序

1. 使用 JetBrains Account 登录 Marketplace。
2. 由账号所有者本人确认 Developer Agreement，并创建 Vendor Profile。
3. 上传已签名 ZIP、真实截图和英文上架文案。
4. 填写公开源码、问题反馈与许可证链接。
5. 提交审核；审核通过后再配置自动发布 token。

## 免费发布口径

- License: Apache-2.0
- Pricing: Free
- Privacy: no model upload, no preview server, no analytics in version 0.1.0
- Supported IDE baseline: WebStorm 2026.3 / build 263+
