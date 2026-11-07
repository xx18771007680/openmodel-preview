package com.fin.openmodel.file;

import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public final class MeshFileType implements FileType {
    public static final MeshFileType INSTANCE = new MeshFileType();

    private MeshFileType() {
    }

    @Override
    public @NotNull String getName() {
        return "3D Mesh";
    }

    @Override
    public @NotNull String getDescription() {
        return "3D mesh model";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "stl";
    }

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
