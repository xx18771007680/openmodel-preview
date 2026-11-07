# Contributing

Thanks for helping improve OpenModel Preview.

## Before You Start

- Search existing issues before opening a new one.
- Keep changes focused on local model parsing, preview rendering, IDE integration, or documentation.
- Do not add model uploads, analytics, or a preview server without an explicit design discussion.
- Do not commit model files that you are not licensed to redistribute.

## Development

Use JDK 25 and run the quality gate before opening a pull request:

```bash
./gradlew test buildPlugin verifyPluginStructure verifyPluginProjectConfiguration
```

If WebStorm is already installed, you can avoid downloading an IDE SDK:

```bash
./gradlew test buildPlugin -PlocalIdePath="/Applications/WebStorm.app"
```

Add or update tests for parser and OpenSCAD command-planning changes. For viewer changes, describe the IDE and model used for manual verification.

## Pull Requests

- Explain the user-visible behavior and why the change is needed.
- Keep generated output, IDE metadata, caches, signing material, and Marketplace tokens out of the commit.
- Update `CHANGELOG.md` when behavior changes.
- Confirm that model data remains local unless the project has explicitly approved a different architecture.
