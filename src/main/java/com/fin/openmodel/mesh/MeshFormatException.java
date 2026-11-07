package com.fin.openmodel.mesh;

import java.io.IOException;

public final class MeshFormatException extends IOException {
    public MeshFormatException(String message) {
        super(message);
    }

    public MeshFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
