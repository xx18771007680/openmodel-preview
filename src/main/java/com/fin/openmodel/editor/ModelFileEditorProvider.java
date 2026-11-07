package com.fin.openmodel.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

public final class ModelFileEditorProvider implements FileEditorProvider, DumbAware {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("scad", "stl", "obj", "ply");

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        String extension = file.getExtension();
        return !file.isDirectory() && extension != null &&
            SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        ModelPreviewEditor preview = new ModelPreviewEditor(project, file);
        if ("scad".equalsIgnoreCase(file.getExtension())) {
            TextEditor textEditor = (TextEditor) TextEditorProvider.getInstance().createEditor(project, file);
            return new TextEditorWithPreview(
                textEditor,
                preview,
                "OpenSCAD 3D",
                TextEditorWithPreview.Layout.SHOW_EDITOR_AND_PREVIEW
            );
        }
        return preview;
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "openmodel-3d-preview";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    @Override
    public boolean acceptRequiresReadAction() {
        return false;
    }
}
