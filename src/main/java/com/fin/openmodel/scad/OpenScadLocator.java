package com.fin.openmodel.scad;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OpenScadLocator {
    private static final Path MAC_APP_EXECUTABLE = Path.of(
        "/Applications/OpenSCAD.app/Contents/MacOS/OpenSCAD"
    );

    private OpenScadLocator() {
    }

    public static Optional<Path> locate() {
        List<Path> candidates = new ArrayList<>();
        addConfigured(candidates, System.getProperty("openmodel.openscad.path"));
        addConfigured(candidates, System.getenv("OPENSCAD_PATH"));
        candidates.add(MAC_APP_EXECUTABLE);

        String pathValue = System.getenv("PATH");
        if (pathValue != null) {
            for (String directory : pathValue.split(java.io.File.pathSeparator)) {
                if (!directory.isBlank()) {
                    candidates.add(Path.of(directory, isWindows() ? "openscad.exe" : "openscad"));
                }
            }
        }

        return candidates.stream()
            .map(Path::toAbsolutePath)
            .filter(Files::isRegularFile)
            .filter(Files::isExecutable)
            .findFirst();
    }

    private static void addConfigured(List<Path> candidates, String value) {
        if (value != null && !value.isBlank()) {
            candidates.add(Path.of(value));
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
