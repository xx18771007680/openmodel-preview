package com.fin.openmodel.mesh;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public final class MeshParsers {
    private MeshParsers() {
    }

    public static MeshData parse(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String extension = dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        MeshParser parser = switch (extension) {
            case "stl" -> new StlParser();
            case "obj" -> new ObjParser();
            case "ply" -> new PlyParser();
            default -> throw new MeshFormatException("Unsupported model format: " + extension);
        };
        return parser.parse(path);
    }
}
