package com.fin.openmodel.mesh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PlyParser implements MeshParser {
    private enum Format {
        ASCII,
        BINARY_LITTLE_ENDIAN,
        BINARY_BIG_ENDIAN
    }

    private record Property(String name, ScalarType scalarType, ScalarType listSizeType, ScalarType listValueType) {
        static Property scalar(String name, ScalarType type) {
            return new Property(name, type, null, null);
        }

        static Property list(String name, ScalarType sizeType, ScalarType valueType) {
            return new Property(name, null, sizeType, valueType);
        }

        boolean isList() {
            return listSizeType != null;
        }
    }

    private record Element(String name, int count, List<Property> properties) {
    }

    private record Header(Format format, List<Element> elements, int dataOffset) {
    }

    private enum ScalarType {
        INT8(1), UINT8(1), INT16(2), UINT16(2), INT32(4), UINT32(4), FLOAT32(4), FLOAT64(8);

        private final int size;

        ScalarType(int size) {
            this.size = size;
        }

        static ScalarType fromName(String name) throws MeshFormatException {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "char", "int8" -> INT8;
                case "uchar", "uint8" -> UINT8;
                case "short", "int16" -> INT16;
                case "ushort", "uint16" -> UINT16;
                case "int", "int32" -> INT32;
                case "uint", "uint32" -> UINT32;
                case "float", "float32" -> FLOAT32;
                case "double", "float64" -> FLOAT64;
                default -> throw new MeshFormatException("Unsupported PLY scalar type: " + name);
            };
        }
    }

    @Override
    public MeshData parse(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        Header header = parseHeader(bytes);
        ParsedBody body = header.format == Format.ASCII
            ? parseAscii(bytes, header)
            : parseBinary(bytes, header);
        return triangulate(body.vertices, body.faces);
    }

    private static Header parseHeader(byte[] bytes) throws MeshFormatException {
        int markerStart = indexOf(bytes, "end_header".getBytes(StandardCharsets.US_ASCII));
        if (markerStart < 0) {
            throw new MeshFormatException("PLY header has no end_header marker");
        }
        int dataOffset = markerStart + "end_header".length();
        while (dataOffset < bytes.length && bytes[dataOffset] != '\n') {
            dataOffset++;
        }
        if (dataOffset < bytes.length) {
            dataOffset++;
        }

        String text = new String(bytes, 0, markerStart, StandardCharsets.US_ASCII);
        String[] lines = text.split("\\R");
        if (lines.length == 0 || !"ply".equals(lines[0].trim())) {
            throw new MeshFormatException("File is not a PLY document");
        }

        Format format = null;
        List<Element> elements = new ArrayList<>();
        String currentName = null;
        int currentCount = 0;
        List<Property> currentProperties = null;
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0) {
                continue;
            }
            if ("format".equals(parts[0]) && parts.length >= 2) {
                format = switch (parts[1]) {
                    case "ascii" -> Format.ASCII;
                    case "binary_little_endian" -> Format.BINARY_LITTLE_ENDIAN;
                    case "binary_big_endian" -> Format.BINARY_BIG_ENDIAN;
                    default -> throw new MeshFormatException("Unsupported PLY format: " + parts[1]);
                };
            } else if ("element".equals(parts[0]) && parts.length >= 3) {
                if (currentName != null) {
                    elements.add(new Element(currentName, currentCount, List.copyOf(currentProperties)));
                }
                currentName = parts[1];
                currentCount = Integer.parseInt(parts[2]);
                currentProperties = new ArrayList<>();
            } else if ("property".equals(parts[0]) && currentProperties != null) {
                if (parts.length >= 5 && "list".equals(parts[1])) {
                    currentProperties.add(Property.list(
                        parts[4],
                        ScalarType.fromName(parts[2]),
                        ScalarType.fromName(parts[3])
                    ));
                } else if (parts.length >= 3) {
                    currentProperties.add(Property.scalar(parts[2], ScalarType.fromName(parts[1])));
                }
            }
        }
        if (currentName != null) {
            elements.add(new Element(currentName, currentCount, List.copyOf(currentProperties)));
        }
        if (format == null) {
            throw new MeshFormatException("PLY header has no format declaration");
        }
        return new Header(format, List.copyOf(elements), dataOffset);
    }

    private static ParsedBody parseAscii(byte[] bytes, Header header) throws MeshFormatException {
        AsciiTokens tokens = new AsciiTokens(new String(
            bytes,
            header.dataOffset,
            bytes.length - header.dataOffset,
            StandardCharsets.US_ASCII
        ));
        BodyCollector collector = new BodyCollector(header.elements);
        for (Element element : header.elements) {
            for (int row = 0; row < element.count; row++) {
                collector.startRow(element);
                for (Property property : element.properties) {
                    if (property.isList()) {
                        int count = toCount(tokens.nextDouble());
                        double[] values = new double[count];
                        for (int i = 0; i < count; i++) {
                            values[i] = tokens.nextDouble();
                        }
                        collector.listValue(element, property, values);
                    } else {
                        collector.scalarValue(element, property, tokens.nextDouble());
                    }
                }
                collector.endRow(element);
            }
        }
        return collector.finish();
    }

    private static ParsedBody parseBinary(byte[] bytes, Header header) throws MeshFormatException {
        ByteOrder order = header.format == Format.BINARY_LITTLE_ENDIAN
            ? ByteOrder.LITTLE_ENDIAN
            : ByteOrder.BIG_ENDIAN;
        ByteBuffer buffer = ByteBuffer.wrap(bytes, header.dataOffset, bytes.length - header.dataOffset).order(order);
        BodyCollector collector = new BodyCollector(header.elements);
        try {
            for (Element element : header.elements) {
                for (int row = 0; row < element.count; row++) {
                    collector.startRow(element);
                    for (Property property : element.properties) {
                        if (property.isList()) {
                            int count = toCount(readScalar(buffer, property.listSizeType));
                            double[] values = new double[count];
                            for (int i = 0; i < count; i++) {
                                values[i] = readScalar(buffer, property.listValueType);
                            }
                            collector.listValue(element, property, values);
                        } else {
                            collector.scalarValue(element, property, readScalar(buffer, property.scalarType));
                        }
                    }
                    collector.endRow(element);
                }
            }
        } catch (java.nio.BufferUnderflowException error) {
            throw new MeshFormatException("Binary PLY ended before all declared elements were read", error);
        }
        return collector.finish();
    }

    private static MeshData triangulate(float[] vertices, List<int[]> faces) throws MeshFormatException {
        FloatArrayBuilder positions = new FloatArrayBuilder(Math.max(faces.size() * 9, 16));
        for (int[] face : faces) {
            if (face.length < 3) {
                continue;
            }
            for (int i = 2; i < face.length; i++) {
                addVertex(positions, vertices, face[0]);
                addVertex(positions, vertices, face[i - 1]);
                addVertex(positions, vertices, face[i]);
            }
        }
        if (positions.size() == 0) {
            throw new MeshFormatException("PLY contains no polygon faces");
        }
        return new MeshData(positions.toArray());
    }

    private static void addVertex(FloatArrayBuilder output, float[] vertices, int index) throws MeshFormatException {
        int offset = index * 3;
        if (index < 0 || offset + 2 >= vertices.length) {
            throw new MeshFormatException("PLY face references a missing vertex: " + index);
        }
        output.add3(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
    }

    private static double readScalar(ByteBuffer buffer, ScalarType type) {
        return switch (type) {
            case INT8 -> buffer.get();
            case UINT8 -> Byte.toUnsignedInt(buffer.get());
            case INT16 -> buffer.getShort();
            case UINT16 -> Short.toUnsignedInt(buffer.getShort());
            case INT32 -> buffer.getInt();
            case UINT32 -> Integer.toUnsignedLong(buffer.getInt());
            case FLOAT32 -> buffer.getFloat();
            case FLOAT64 -> buffer.getDouble();
        };
    }

    private static int toCount(double value) throws MeshFormatException {
        if (!Double.isFinite(value) || value < 0 || value > Integer.MAX_VALUE || value != Math.rint(value)) {
            throw new MeshFormatException("Invalid PLY list length: " + value);
        }
        return (int) value;
    }

    private static int indexOf(byte[] source, byte[] target) {
        outer:
        for (int i = 0; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static final class AsciiTokens {
        private final String source;
        private int offset;

        private AsciiTokens(String source) {
            this.source = source;
        }

        double nextDouble() throws MeshFormatException {
            while (offset < source.length() && Character.isWhitespace(source.charAt(offset))) {
                offset++;
            }
            if (offset >= source.length()) {
                throw new MeshFormatException("ASCII PLY ended before all declared elements were read");
            }
            int start = offset;
            while (offset < source.length() && !Character.isWhitespace(source.charAt(offset))) {
                offset++;
            }
            try {
                return Double.parseDouble(source.substring(start, offset));
            } catch (NumberFormatException error) {
                throw new MeshFormatException("Invalid numeric value in ASCII PLY", error);
            }
        }
    }

    private static final class ParsedBody {
        private final float[] vertices;
        private final List<int[]> faces;

        private ParsedBody(float[] vertices, List<int[]> faces) {
            this.vertices = vertices;
            this.faces = faces;
        }
    }

    private static final class BodyCollector {
        private final float[] vertices;
        private final List<int[]> faces = new ArrayList<>();
        private int vertexOffset;
        private float x;
        private float y;
        private float z;

        private BodyCollector(List<Element> elements) throws MeshFormatException {
            int vertexCount = elements.stream()
                .filter(element -> "vertex".equals(element.name))
                .mapToInt(Element::count)
                .findFirst()
                .orElseThrow(() -> new MeshFormatException("PLY has no vertex element"));
            vertices = new float[vertexCount * 3];
        }

        void startRow(Element element) {
            if ("vertex".equals(element.name)) {
                x = 0;
                y = 0;
                z = 0;
            }
        }

        void scalarValue(Element element, Property property, double value) {
            if (!"vertex".equals(element.name)) {
                return;
            }
            switch (property.name) {
                case "x" -> x = (float) value;
                case "y" -> y = (float) value;
                case "z" -> z = (float) value;
                default -> {
                }
            }
        }

        void listValue(Element element, Property property, double[] values) throws MeshFormatException {
            if (!"face".equals(element.name) ||
                !("vertex_indices".equals(property.name) || "vertex_index".equals(property.name))) {
                return;
            }
            int[] face = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                if (values[i] != Math.rint(values[i])) {
                    throw new MeshFormatException("PLY face index is not an integer");
                }
                face[i] = (int) values[i];
            }
            faces.add(face);
        }

        void endRow(Element element) throws MeshFormatException {
            if (!"vertex".equals(element.name)) {
                return;
            }
            if (vertexOffset + 2 >= vertices.length) {
                throw new MeshFormatException("PLY contains more vertices than declared");
            }
            vertices[vertexOffset++] = x;
            vertices[vertexOffset++] = y;
            vertices[vertexOffset++] = z;
        }

        ParsedBody finish() throws MeshFormatException {
            if (vertexOffset != vertices.length) {
                throw new MeshFormatException("PLY contains fewer vertices than declared");
            }
            return new ParsedBody(vertices, List.copyOf(faces));
        }
    }
}
