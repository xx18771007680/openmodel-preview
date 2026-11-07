# OpenModel Preview

[![Build](https://github.com/xx18771007680/openmodel-preview/actions/workflows/build.yml/badge.svg)](https://github.com/xx18771007680/openmodel-preview/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

OpenModel Preview is a local 3D model preview plugin for JetBrains IDEs. It opens OpenSCAD, STL, OBJ, and PLY files directly in the editor with orbit, zoom, fit-to-view, and grid controls.

Models, source code, and file paths stay on your computer. The plugin does not upload files, start a preview server, or use analytics.

## Features

| Format | Support |
| --- | --- |
| OpenSCAD `.scad` | Split source/preview editor; renders through the locally installed OpenSCAD CLI |
| STL `.stl` | ASCII and binary files |
| OBJ `.obj` | Vertices, positive/negative indices, and polygon triangulation; materials and textures are not rendered yet |
| PLY `.ply` | ASCII, binary little-endian, and binary big-endian vertex/face data |

Rendering is handled offline by the JCEF/WebGL runtime bundled with the IDE. OpenSCAD output is converted to a temporary STL in the IDE system cache and deleted immediately after parsing.

## Requirements

- WebStorm 2026.3 or a compatible JetBrains IDE based on build 263+
- [OpenSCAD](https://openscad.org/) for `.scad` previews only

The plugin locates OpenSCAD in this order:

1. JVM property `openmodel.openscad.path`
2. Environment variable `OPENSCAD_PATH`
3. Standard macOS application path `/Applications/OpenSCAD.app/Contents/MacOS/OpenSCAD`
4. System `PATH`

On Apple silicon, OpenModel Preview first runs OpenSCAD natively. If an Intel/Qt build fails, it retries through Rosetta with `arch -x86_64`.

## Install From Disk

1. Download the plugin ZIP from [GitHub Releases](https://github.com/xx18771007680/openmodel-preview/releases).
2. In the IDE, open `Settings > Plugins`.
3. From the gear menu, choose `Install Plugin from Disk...`.
4. Select the downloaded ZIP and restart or enable the plugin when prompted.

Disable other plugins that claim the `.scad` file type before enabling OpenModel Preview.

## Build From Source

The project requires JDK 25. If WebStorm is installed locally, its bundled JBR can be reused:

```bash
export JAVA_HOME="/Applications/WebStorm.app/Contents/jbr/Contents/Home"
./gradlew test buildPlugin -PlocalIdePath="/Applications/WebStorm.app"
```

Without `localIdePath`, Gradle downloads the target WebStorm SDK configured in `gradle.properties`:

```bash
./gradlew test buildPlugin verifyPluginStructure verifyPluginProjectConfiguration
```

The installable ZIP is written to `build/distributions/`.

## Privacy and Security

- No model, source code, or local path is uploaded.
- No local HTTP server or listening port is created.
- No analytics or telemetry is included in version 0.1.0.
- Gradle uses the network only while developers download build dependencies.

See [SECURITY.md](SECURITY.md) for vulnerability reports.

## Contributing

Bug reports and focused pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a change.

## License

Apache License 2.0. See [LICENSE](LICENSE).

## 中文说明

OpenModel Preview 是一个面向 JetBrains IDE 的本地 3D 模型预览插件。首版支持 `.scad`、`.stl`、`.obj`、`.ply`，可以在编辑器内旋转、缩放、复位视角和切换网格。

插件不会把模型、源代码或文件路径上传到服务器，也不会启动本地预览服务。SCAD 预览调用本机 OpenSCAD，其他格式直接在插件进程中解析，画面由 IDE 自带的 JCEF/WebGL 离线绘制。

发布到 JetBrains Marketplace 的维护清单见 [MARKETPLACE.md](MARKETPLACE.md)。
