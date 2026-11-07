package com.fin.openmodel.mesh;

import java.io.IOException;
import java.nio.file.Path;

interface MeshParser {
    MeshData parse(Path path) throws IOException;
}
