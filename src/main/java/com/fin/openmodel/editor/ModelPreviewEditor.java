package com.fin.openmodel.editor;

import com.fin.openmodel.mesh.MeshData;
import com.fin.openmodel.mesh.MeshParsers;
import com.fin.openmodel.scad.OpenScadCli;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ModelPreviewEditor extends UserDataHolderBase implements FileEditor {
    private static final Duration OPENSCAD_TIMEOUT = Duration.ofMinutes(2);
    private static final String PAGE_URL = "https://openmodel.local/viewer";

    private final VirtualFile file;
    private final JPanel component = new JPanel(new BorderLayout());
    private final PropertyChangeSupport propertyChanges = new PropertyChangeSupport(this);
    private final AtomicBoolean disposed = new AtomicBoolean();
    private final AtomicLong renderGeneration = new AtomicLong();
    private final Alarm refreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    private final JBCefBrowser browser;
    private volatile boolean pageLoaded;
    private volatile String pendingScript;

    public ModelPreviewEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.file = file;
        if (!JBCefApp.isSupported()) {
            browser = null;
            JLabel message = new JLabel(
                "3D preview requires the JetBrains Runtime with JCEF enabled.",
                SwingConstants.CENTER
            );
            message.setForeground(JBColor.foreground());
            component.add(message, BorderLayout.CENTER);
            return;
        }

        browser = new JBCefBrowser();
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                if (!frame.isMain()) {
                    return;
                }
                pageLoaded = true;
                String script = pendingScript;
                if (script != null) {
                    pendingScript = null;
                    executeScript(script);
                }
            }
        }, browser.getCefBrowser());
        component.add(browser.getComponent(), BorderLayout.CENTER);
        browser.loadHTML(loadViewerHtml(), PAGE_URL);
        attachLiveRefresh();
        refresh();
    }

    private void attachLiveRefresh() {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                refreshAlarm.cancelAllRequests();
                refreshAlarm.addRequest(ModelPreviewEditor.this::refresh, 600);
            }
        }, this);
    }

    private void refresh() {
        long generation = renderGeneration.incrementAndGet();
        runScript("window.OpenModel && window.OpenModel.setLoading(true);");
        if ("scad".equalsIgnoreCase(file.getExtension())) {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null && FileDocumentManager.getInstance().isDocumentUnsaved(document)) {
                FileDocumentManager.getInstance().saveDocument(document);
            }
        }

        CompletableFuture
            .supplyAsync(() -> loadMesh(generation), AppExecutorUtil.getAppExecutorService())
            .whenComplete((result, failure) -> ApplicationManager.getApplication().invokeLater(() -> {
                if (disposed.get() || generation != renderGeneration.get()) {
                    return;
                }
                if (failure != null) {
                    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                    showError(cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
                } else {
                    runScript("window.OpenModel.loadMesh(" + result.toViewerJson(file.getName()) + ");");
                }
            }));
    }

    private MeshData loadMesh(long generation) {
        Path source = file.toNioPath();
        Path rendered = null;
        try {
            if ("scad".equalsIgnoreCase(file.getExtension())) {
                Path cacheDirectory = Path.of(PathManager.getSystemPath(), "openmodel-preview");
                String cacheName = Integer.toUnsignedString(source.toAbsolutePath().toString().hashCode(), 36) +
                    "-" + generation + ".stl";
                rendered = cacheDirectory.resolve(cacheName);
                OpenScadCli.renderToStl(source, rendered, OPENSCAD_TIMEOUT);
                return MeshParsers.parse(rendered);
            }
            return MeshParsers.parse(source);
        } catch (IOException error) {
            throw new ModelPreviewException(error.getMessage(), error);
        } finally {
            if (rendered != null) {
                try {
                    Files.deleteIfExists(rendered);
                } catch (IOException ignored) {
                    // The IDE system cache can safely remove a stale render later.
                }
            }
        }
    }

    private static String loadViewerHtml() {
        try (InputStream stream = ModelPreviewEditor.class.getResourceAsStream("/viewer/index.html")) {
            if (stream == null) {
                throw new IOException("Bundled viewer page is missing");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            return "<html><body>Unable to load the bundled 3D viewer.</body></html>";
        }
    }

    private void showError(String message) {
        runScript("window.OpenModel.showError(\"" + escapeJavaScript(message) + "\");");
    }

    private void runScript(String script) {
        if (browser == null || disposed.get()) {
            return;
        }
        if (!pageLoaded) {
            pendingScript = script;
            return;
        }
        executeScript(script);
    }

    private void executeScript(String script) {
        if (browser != null && !disposed.get()) {
            browser.getCefBrowser().executeJavaScript(script, PAGE_URL, 0);
        }
    }

    private static String escapeJavaScript(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return browser == null ? component : browser.getComponent();
    }

    @Override
    public @NotNull String getName() {
        return "3D Preview";
    }

    @Override
    public @NotNull FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return file.isValid() && !disposed.get();
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        propertyChanges.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        propertyChanges.removePropertyChangeListener(listener);
    }

    @Override
    public void dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return;
        }
        renderGeneration.incrementAndGet();
        if (browser != null) {
            browser.dispose();
        }
    }

    private static final class ModelPreviewException extends RuntimeException {
        private ModelPreviewException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
