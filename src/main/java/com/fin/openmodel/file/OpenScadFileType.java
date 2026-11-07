package com.fin.openmodel.file;

import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public final class OpenScadFileType implements FileType {
    public static final OpenScadFileType INSTANCE = new OpenScadFileType();

    private OpenScadFileType() {
    }

    @Override
    public @NotNull String getName() {
        return "OpenSCAD";
    }

    @Override
    public @NotNull String getDescription() {
        return "OpenSCAD model";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "scad";
    }

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
