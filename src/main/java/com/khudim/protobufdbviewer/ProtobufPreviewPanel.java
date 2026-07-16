package com.khudim.protobufdbviewer;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

final class ProtobufPreviewPanel implements Disposable {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final Document document;
    private final Editor editor;

    ProtobufPreviewPanel(Project project) {
        document = EditorFactory.getInstance().createDocument(
                "Select a binary cell and choose View as Protobuf."
        );
        editor = EditorFactory.getInstance().createEditor(
                document,
                project,
                JsonFileType.INSTANCE,
                true
        );
        if (editor instanceof EditorEx editorEx) {
            editorEx.setHorizontalScrollbarVisible(true);
            editorEx.setVerticalScrollbarVisible(true);
            editorEx.getSettings().setLineNumbersShown(true);
            editorEx.getSettings().setFoldingOutlineShown(true);
        }
        panel.add(editor.getComponent(), BorderLayout.CENTER);
    }

    JComponent component() {
        return panel;
    }

    void setText(String text) {
        ApplicationManager.getApplication().invokeLater(() ->
                WriteAction.run(() -> document.setText(text == null ? "" : text))
        );
    }

    @Override
    public void dispose() {
        EditorFactory.getInstance().releaseEditor(editor);
    }
}
