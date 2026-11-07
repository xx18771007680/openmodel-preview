package com.fin.openmodel.mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ObjParser implements MeshParser {
    @Override
    public MeshData parse(Path path) throws IOException {
        List<float[]> vertices = new ArrayList<>();
        FloatArrayBuilder positions = new FloatArrayBuilder();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                int comment = line.indexOf('#');
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                try {
                    if ("v".equals(parts[0]) && parts.length >= 4) {
                        vertices.add(new float[]{
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3])
                        });
                    } else if ("f".equals(parts[0]) && parts.length >= 4) {
                        int first = parseVertexIndex(parts[1], vertices.size());
                        int previous = parseVertexIndex(parts[2], vertices.size());
                        for (int i = 3; i < parts.length; i++) {
                            int current = parseVertexIndex(parts[i], vertices.size());
                            addVertex(positions, vertices.get(first));
                            addVertex(positions, vertices.get(previous));
                            addVertex(positions, vertices.get(current));
                            previous = current;
                        }
                    }
                } catch (IllegalArgumentException | IndexOutOfBoundsException error) {
                    throw new MeshFormatException("Invalid OBJ data at line " + lineNumber, error);
                }
            }
        }
        if (positions.size() == 0) {
            throw new MeshFormatException("OBJ contains no faces");
        }
        return new MeshData(positions.toArray());
    }

    private static int parseVertexIndex(String token, int vertexCount) {
        String indexPart = token.split("/", -1)[0];
        int raw = Integer.parseInt(indexPart);
        int index = raw > 0 ? raw - 1 : vertexCount + raw;
        if (index < 0 || index >= vertexCount) {
            throw new IllegalArgumentException("Vertex index is outside the vertex list");
        }
        return index;
    }

    private static void addVertex(FloatArrayBuilder output, float[] vertex) {
        output.add3(vertex[0], vertex[1], vertex[2]);
    }
}
