package com.fin.openmodel.scad;

import com.fin.openmodel.mesh.MeshData;
import com.fin.openmodel.mesh.MeshParsers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OpenScadCliTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsNativeCommandAndOffersRosettaFallbackOnAppleSilicon() {
        Path executable = Path.of("/Applications/OpenSCAD.app/Contents/MacOS/OpenSCAD");
        Path source = Path.of("model.scad");
        Path output = Path.of("model.stl");

        List<String> nativeCommand = OpenScadCommandPlan.build(executable, source, output);
        List<String> fallbackCommand = OpenScadCommandPlan.buildRosettaFallback(
            executable,
            source,
            output,
            "Mac OS X",
            "aarch64"
        ).orElseThrow();

        assertEquals(executable.toString(), nativeCommand.get(0));
        assertEquals("arch", fallbackCommand.get(0));
        assertEquals("-x86_64", fallbackCommand.get(1));
        assertEquals("-o", fallbackCommand.get(3));
    }

    @Test
    void invokesInstalledOpenScadAndParsesItsOutput() throws IOException {
        assumeTrue(OpenScadLocator.locate().isPresent(), "OpenSCAD is not installed on this machine");
        Path source = temporaryDirectory.resolve("cube.scad");
        Path output = temporaryDirectory.resolve("cube.stl");
        Files.writeString(source, "cube([4, 5, 6], center = true);\n");

        OpenScadCli.renderToStl(source, output, Duration.ofSeconds(90));
        MeshData mesh = MeshParsers.parse(output);

        assertTrue(Files.size(output) > 0);
        assertEquals(12, mesh.triangleCount());
    }
}
