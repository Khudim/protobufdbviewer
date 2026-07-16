package com.khudim.protobufdbviewer;

import com.google.protobuf.Descriptors;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;

public final class ViewAsProtobufAction extends AnAction {
    private static final String TOOL_WINDOW_ID = "Protobuf Preview";

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        try {
            ensureConfigured(project);

            Component contextComponent = event.getData(PlatformDataKeys.CONTEXT_COMPONENT);
            SelectedCell cell = SelectedCellReader.read(contextComponent);
            if (cell.bytes().length == 0) {
                throw new IllegalArgumentException("The selected cell contains no binary data.");
            }

            ProtobufProjectSettings settings = ProtobufProjectSettings.getInstance(project);
            String gridSignature = GridIdentity.signature(cell.grid());
            ProtobufProjectSettings.MappingData saved = settings.findMapping(gridSignature);

            String initialValue = saved == null ? "" : saved.messageName;
            String messageName = Messages.showInputDialog(
                    project,
                    "Enter the protobuf message name, for example Transaction or com.example.Transaction:",
                    "View as Protobuf",
                    Messages.getQuestionIcon(),
                    initialValue,
                    null
            );
            if (messageName == null || messageName.isBlank()) return;
            messageName = messageName.trim();

            DescriptorRegistry registry = project.getService(DescriptorRegistryService.class).getRegistry();
            Descriptors.Descriptor descriptor = registry.resolveByMessageName(messageName);

            String columnName = GridIdentity.columnName(cell.grid(), cell.viewColumn());
            settings.saveMapping(gridSignature, columnName, descriptor.getFullName());

            ProtobufPreviewService previewService = project.getService(ProtobufPreviewService.class);
            previewService.attach(cell.grid(), cell.viewColumn(), descriptor);

            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
            if (toolWindow == null) {
                throw new IllegalStateException("The Protobuf Preview tool window is not available.");
            }
            toolWindow.show();
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.toString() : exception.getMessage();
            Messages.showErrorDialog(project, message, "Cannot Decode Protobuf");
        }
    }

    private static void ensureConfigured(Project project) {
        ProtobufProjectSettings settings = ProtobufProjectSettings.getInstance(project);
        if (!settings.getProtoRoots().isEmpty()) return;

        ProtoRootsDialog dialog = new ProtoRootsDialog(project, settings.getProtoRoots());
        if (!dialog.showAndGet()) {
            throw new IllegalStateException("No protobuf source directories are configured.");
        }
        settings.update(dialog.getRoots(), settings.getProtocPath());
        project.getService(DescriptorRegistryService.class).invalidate();
    }
}
