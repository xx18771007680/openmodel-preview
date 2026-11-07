package com.fin.openmodel.mesh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshParsersTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesAsciiStl() throws IOException {
        Path model = write("triangle.stl", """
            solid triangle
              facet normal 0 0 1
                outer loop
                  vertex 0 0 0
                  vertex 2 0 0
                  vertex 0 3 0
                endloop
              endfacet
            endsolid triangle
            """);

        MeshData mesh = MeshParsers.parse(model);

        assertEquals(1, mesh.triangleCount());
        assertArrayEquals(new float[]{0, 0, 0, 2, 0, 0, 0, 3, 0}, mesh.positions());
        assertArrayEquals(new float[]{0, 0, 0, 2, 3, 0}, mesh.bounds());
    }

    @Test
    void parsesBinaryStlEvenWhenHeaderStartsWithSolid() throws IOException {
        ByteBuffer bytes = ByteBuffer.allocate(84 + 50).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put("solid binary".getBytes(StandardCharsets.US_ASCII));
        bytes.position(80);
        bytes.putInt(1);
        bytes.putFloat(0).putFloat(0).putFloat(1);
        bytes.putFloat(0).putFloat(0).putFloat(0);
        bytes.putFloat(1).putFloat(0).putFloat(0);
        bytes.putFloat(0).putFloat(1).putFloat(0);
        bytes.putShort((short) 0);
        Path model = temporaryDirectory.resolve("binary.stl");
        Files.write(model, bytes.array());

        MeshData mesh = MeshParsers.parse(model);

        assertEquals(1, mesh.triangleCount());
        assertEquals(9, mesh.positions().length);
    }

    @Test
    void triangulatesObjPolygonAndNegativeIndices() throws IOException {
        Path model = write("quad.obj", """
            v 0 0 0
            v 2 0 0
            v 2 2 0
            v 0 2 0
            f -4/-1 -3/-1 -2/-1 -1/-1
            """);

        MeshData mesh = MeshParsers.parse(model);

        assertEquals(2, mesh.triangleCount());
        assertArrayEquals(new float[]{0, 0, 0, 2, 2, 0}, mesh.bounds());
    }

    @Test
    void parsesAsciiPly() throws IOException {
        Path model = write("quad.ply", """
            ply
            format ascii 1.0
            element vertex 4
            property float x
            property float y
            property float z
            element face 1
            property list uchar int vertex_indices
            end_header
            0 0 0
            2 0 0
            2 2 0
            0 2 0
            4 0 1 2 3
            """);

        MeshData mesh = MeshParsers.parse(model);

        assertEquals(2, mesh.triangleCount());
    }

    @Test
    void parsesBinaryLittleEndianPly() throws IOException {
        String header = """
            ply
            format binary_little_endian 1.0
            element vertex 3
            property float x
            property float y
            property float z
            element face 1
            property list uchar int vertex_indices
            end_header
            """;
        byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer bytes = ByteBuffer.allocate(headerBytes.length + 36 + 13).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(headerBytes);
        bytes.putFloat(0).putFloat(0).putFloat(0);
        bytes.putFloat(1).putFloat(0).putFloat(0);
        bytes.putFloat(0).putFloat(1).putFloat(0);
        bytes.put((byte) 3).putInt(0).putInt(1).putInt(2);
        Path model = temporaryDirectory.resolve("binary.ply");
        Files.write(model, bytes.array());

        MeshData mesh = MeshParsers.parse(model);

        assertEquals(1, mesh.triangleCount());
        assertTrue(mesh.toViewerJson("binary.ply").contains("\"triangles\":1"));
    }

    private Path write(String name, String contents) throws IOException {
        Path path = temporaryDirectory.resolve(name);
        Files.writeString(path, contents);
        return path;
    }
}
