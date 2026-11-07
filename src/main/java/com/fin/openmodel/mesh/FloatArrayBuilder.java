package com.fin.openmodel.mesh;

import java.util.Arrays;

final class FloatArrayBuilder {
    private float[] values;
    private int size;

    FloatArrayBuilder() {
        this(1024);
    }

    FloatArrayBuilder(int initialCapacity) {
        values = new float[Math.max(initialCapacity, 16)];
    }

    void add(float value) {
        ensureCapacity(size + 1);
        values[size++] = value;
    }

    void add3(float x, float y, float z) {
        ensureCapacity(size + 3);
        values[size++] = x;
        values[size++] = y;
        values[size++] = z;
    }

    int size() {
        return size;
    }

    float[] toArray() {
        return Arrays.copyOf(values, size);
    }

    private void ensureCapacity(int required) {
        if (required <= values.length) {
            return;
        }
        int next = Math.max(required, values.length + values.length / 2);
        values = Arrays.copyOf(values, next);
    }
}
