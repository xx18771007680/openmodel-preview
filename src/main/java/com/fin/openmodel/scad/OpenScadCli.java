package com.fin.openmodel.scad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class OpenScadCli {
    private OpenScadCli() {
    }

    public static Path renderToStl(Path source, Path output, Duration timeout) throws IOException {
        Path executable = OpenScadLocator.locate().orElseThrow(() -> new IOException(
            "OpenSCAD was not found. Install OpenSCAD or set OPENSCAD_PATH."
        ));
        Path absoluteSource = source.toAbsolutePath();
        Path absoluteOutput = output.toAbsolutePath();
        Files.createDirectories(absoluteOutput.getParent());

        List<List<String>> attempts = new ArrayList<>();
        attempts.add(OpenScadCommandPlan.build(executable, absoluteSource, absoluteOutput));
        OpenScadCommandPlan.buildRosettaFallback(
            executable,
            absoluteSource,
            absoluteOutput,
            System.getProperty("os.name", ""),
            System.getProperty("os.arch", "")
        ).ifPresent(attempts::add);

        IOException firstFailure = null;
        for (List<String> command : attempts) {
            Files.deleteIfExists(absoluteOutput);
            try {
                return renderOnce(command, absoluteOutput, timeout);
            } catch (IOException error) {
                if (firstFailure == null) {
                    firstFailure = error;
                } else {
                    IOException combined = new IOException(
                        "OpenSCAD failed natively and through Rosetta: " + error.getMessage(),
                        error
                    );
                    combined.addSuppressed(firstFailure);
                    throw combined;
                }
            }
        }

        throw firstFailure == null ? new IOException("OpenSCAD rendering failed") : firstFailure;
    }

    private static Path renderOnce(List<String> command, Path output, Duration timeout) throws IOException {
        Path diagnosticLog = Files.createTempFile(output.getParent(), ".openmodel-openscad-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(diagnosticLog.toFile())
                .start();
            boolean finished;
            try {
                finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
                throw new IOException("OpenSCAD rendering was interrupted", error);
            }
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("OpenSCAD rendering timed out after " + timeout.toSeconds() + " seconds");
            }
            String outputText = Files.readString(diagnosticLog, StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IOException("OpenSCAD failed: " + shorten(outputText));
            }
            if (!Files.isRegularFile(output) || Files.size(output) == 0) {
                throw new IOException("OpenSCAD did not create an STL file: " + shorten(outputText));
            }
            return output;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            Files.deleteIfExists(diagnosticLog);
        }
    }

    private static String shorten(String value) {
        if (value.isBlank()) {
            return "no diagnostic output";
        }
        return value.length() <= 1200 ? value : value.substring(0, 1200) + "...";
    }
}
