package com.khudim.protobufdbviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public final class ProtobufPreviewToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ProtobufPreviewPanel panel = new ProtobufPreviewPanel(project);
        Content content = ContentFactory.getInstance().createContent(panel.component(), "", false);
        content.setDisposer(panel);
        toolWindow.getContentManager().addContent(content);
        project.getService(ProtobufPreviewService.class).setPanel(panel);
    }
}
