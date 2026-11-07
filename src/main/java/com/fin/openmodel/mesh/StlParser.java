package com.fin.openmodel.mesh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StlParser implements MeshParser {
    private static final String NUMBER = "[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?";
    private static final Pattern VERTEX = Pattern.compile(
        "(?im)^\\s*vertex\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")\\s*$"
    );

    @Override
    public MeshData parse(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (looksBinary(bytes)) {
            return parseBinary(bytes);
        }
        return parseAscii(bytes);
    }

    private static boolean looksBinary(byte[] bytes) {
        if (bytes.length < 84) {
            return false;
        }
        long triangles = Integer.toUnsignedLong(ByteBuffer.wrap(bytes, 80, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        long required = 84L + triangles * 50L;
        return triangles > 0 && required <= bytes.length;
    }

    private static MeshData parseBinary(byte[] bytes) throws MeshFormatException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(80);
        long triangleCount = Integer.toUnsignedLong(buffer.getInt());
        if (triangleCount > Integer.MAX_VALUE / 9) {
            throw new MeshFormatException("STL triangle count is too large");
        }
        float[] positions = new float[(int) triangleCount * 9];
        int offset = 0;
        for (int triangle = 0; triangle < triangleCount; triangle++) {
            buffer.position(buffer.position() + 12);
            for (int coordinate = 0; coordinate < 9; coordinate++) {
                positions[offset++] = buffer.getFloat();
            }
            buffer.position(buffer.position() + 2);
        }
        return new MeshData(positions);
    }

    private static MeshData parseAscii(byte[] bytes) throws MeshFormatException {
        String source = new String(bytes, StandardCharsets.US_ASCII);
        Matcher matcher = VERTEX.matcher(source);
        FloatArrayBuilder positions = new FloatArrayBuilder();
        while (matcher.find()) {
            positions.add3(
                Float.parseFloat(matcher.group(1)),
                Float.parseFloat(matcher.group(2)),
                Float.parseFloat(matcher.group(3))
            );
        }
        if (positions.size() == 0 || positions.size() % 9 != 0) {
            throw new MeshFormatException("ASCII STL contains incomplete triangles");
        }
        return new MeshData(positions.toArray());
    }
}
