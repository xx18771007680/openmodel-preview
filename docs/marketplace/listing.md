# LingdongAI 3D Model Preview Marketplace Listing

## Basic information

- **Name:** LingdongAI 3D Model Preview
- **Plugin ID:** `com.fin.openmodel.preview`
- **Vendor:** LingdongAI
- **Vendor ID:** `lingdongai`
- **License:** Apache License 2.0
- **Pricing:** Free
- **Source code:** https://github.com/xx18771007680/openmodel-preview
- **Issue tracker:** https://github.com/xx18771007680/openmodel-preview/issues
- **Release notes:** https://github.com/xx18771007680/openmodel-preview/releases
- **Category:** Tools Integration
- **Suggested tags:** 3D, OpenSCAD, STL, OBJ, PLY, model viewer

## Short description

Preview OpenSCAD, STL, OBJ, and PLY models directly in JetBrains IDEs without uploading files.

## Full description

LingdongAI 3D Model Preview adds a private, local 3D model viewer to JetBrains IDEs.

Open `.scad`, `.stl`, `.obj`, or `.ply` files directly in the editor and inspect the model with orbit, zoom, fit-to-view, and grid controls. OpenSCAD files use a split source and preview editor, so code and geometry stay visible together.

All rendering happens on your computer. The plugin does not upload models, source code, or file paths; it does not start a preview server; and it contains no analytics or telemetry.

### Supported formats

- OpenSCAD: local rendering through an installed OpenSCAD CLI
- STL: ASCII and binary
- OBJ: vertices, positive and negative indices, and polygon triangulation
- PLY: ASCII, binary little-endian, and binary big-endian vertex and face data

### Requirements

OpenSCAD must be installed locally to preview `.scad` files. STL, OBJ, and PLY previews do not require OpenSCAD.

### Current limitations

OBJ materials and textures are not rendered in version 0.1.1. The plugin currently targets JetBrains IDEs based on build 263 or later.

## Release notes 0.1.1

- Rename the public plugin to LingdongAI 3D Model Preview.
- Keep the stable plugin ID for normal upgrades from 0.1.0.
- Keep all model processing local with no upload, server, analytics, or telemetry.

## Release notes 0.1.0

- Add local OpenSCAD preview with a split source and 3D editor.
- Add offline STL, OBJ, and PLY parsing and rendering.
- Add orbit, zoom, fit-to-view, and grid controls.
- Keep models and paths local with no upload, server, analytics, or telemetry.

## Privacy declaration

LingdongAI 3D Model Preview processes model data locally. Version 0.1.1 does not collect, transmit, sell, or share personal data, model contents, source code, file paths, analytics, or telemetry.

## Screenshot

Upload `openmodel-preview-1280x800.png` from this directory. The image must show a real IDE window with the OpenSCAD split editor and rendered 3D model visible.
