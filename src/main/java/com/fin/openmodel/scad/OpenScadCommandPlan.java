package com.fin.openmodel.scad;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class OpenScadCommandPlan {
    private OpenScadCommandPlan() {
    }

    public static List<String> build(Path executable, Path source, Path output) {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.add("-o");
        command.add(output.toString());
        command.add(source.toString());
        return List.copyOf(command);
    }

    public static Optional<List<String>> buildRosettaFallback(
        Path executable,
        Path source,
        Path output,
        String osName,
        String architecture
    ) {
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = architecture.toLowerCase(Locale.ROOT);
        if (!os.contains("mac") || (!arch.contains("aarch64") && !arch.contains("arm64"))) {
            return Optional.empty();
        }

        List<String> command = new ArrayList<>();
        command.add("arch");
        command.add("-x86_64");
        command.add(executable.toString());
        command.add("-o");
        command.add(output.toString());
        command.add(source.toString());
        return Optional.of(List.copyOf(command));
    }
}
