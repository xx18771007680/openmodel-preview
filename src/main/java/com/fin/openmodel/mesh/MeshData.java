package com.fin.openmodel.mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

public final class MeshData {
    private final float[] positions;
    private final float[] normals;
    private final float[] bounds;

    public MeshData(float[] positions) throws MeshFormatException {
        if (positions.length == 0 || positions.length % 9 != 0) {
            throw new MeshFormatException("Mesh must contain complete triangles");
        }
        this.positions = positions.clone();
        this.normals = calculateFlatNormals(this.positions);
        this.bounds = calculateBounds(this.positions);
    }

    public int triangleCount() {
        return positions.length / 9;
    }

    public float[] positions() {
        return positions.clone();
    }

    public float[] normals() {
        return normals.clone();
    }

    public float[] bounds() {
        return bounds.clone();
    }

    public String toViewerJson(String sourceName) {
        return "{" +
            "\"name\":\"" + escapeJson(sourceName) + "\"," +
            "\"triangles\":" + triangleCount() + "," +
            "\"positions\":\"" + encodeFloats(positions) + "\"," +
            "\"normals\":\"" + encodeFloats(normals) + "\"," +
            "\"bounds\":[" + join(bounds) + "]" +
            "}";
    }

    private static float[] calculateFlatNormals(float[] vertices) {
        float[] result = new float[vertices.length];
        for (int i = 0; i < vertices.length; i += 9) {
            float ax = vertices[i + 3] - vertices[i];
            float ay = vertices[i + 4] - vertices[i + 1];
            float az = vertices[i + 5] - vertices[i + 2];
            float bx = vertices[i + 6] - vertices[i];
            float by = vertices[i + 7] - vertices[i + 1];
            float bz = vertices[i + 8] - vertices[i + 2];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 1.0e-12f) {
                nx /= length;
                ny /= length;
                nz /= length;
            } else {
                nx = 0.0f;
                ny = 0.0f;
                nz = 1.0f;
            }
            for (int vertex = 0; vertex < 3; vertex++) {
                int offset = i + vertex * 3;
                result[offset] = nx;
                result[offset + 1] = ny;
                result[offset + 2] = nz;
            }
        }
        return result;
    }

    private static float[] calculateBounds(float[] vertices) throws MeshFormatException {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vertices.length; i += 3) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new MeshFormatException("Mesh contains a non-finite coordinate");
            }
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    private static String encodeFloats(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : values) {
            buffer.putFloat(value);
        }
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    private static String join(float[] values) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                output.append(',');
            }
            output.append(Float.toString(values[i]));
        }
        return output.toString();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
