package com.khudim.protobufdbviewer;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

public final class ProtobufSettingsConfigurable implements Configurable {
    private final Project project;
    private final DefaultListModel<String> rootsModel = new DefaultListModel<>();
    private final TextFieldWithBrowseButton protocField = new TextFieldWithBrowseButton();
    private JPanel panel;

    public ProtobufSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @Nls String getDisplayName() {
        return "Protobuf DB Viewer";
    }

    @Override
    public @Nullable JComponent createComponent() {
        JBList<String> rootsList = new JBList<>(rootsModel);
        JComponent rootsPanel = ToolbarDecorator.createDecorator(rootsList)
                .setAddAction(button -> {
                    ProtoRootsDialog dialog = new ProtoRootsDialog(project, getRoots());
                    if (dialog.showAndGet()) setRoots(dialog.getRoots());
                })
                .setRemoveAction(button -> rootsList.getSelectedValuesList().forEach(rootsModel::removeElement))
                .disableUpDownActions()
                .createPanel();

        FileChooserDescriptor descriptor =
            new FileChooserDescriptor(
                true,
                false,
                false,
                false,
                false,
                false
            )
                .withTitle("Select Protoc")
                .withDescription("Select the Protocol Buffers compiler executable");

        protocField.addBrowseFolderListener(project, descriptor);

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Proto source directories:"), rootsPanel, 1, false)
                .addLabeledComponent(new JBLabel("Protoc executable (optional):"), protocField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        ProtobufProjectSettings settings = ProtobufProjectSettings.getInstance(project);
        return !settings.getProtoRoots().equals(getRoots()) || !settings.getProtocPath().equals(protocField.getText().trim());
    }

    @Override
    public void apply() {
        ProtobufProjectSettings.getInstance(project).update(getRoots(), protocField.getText());
        project.getService(DescriptorRegistryService.class).invalidate();
    }

    @Override
    public void reset() {
        ProtobufProjectSettings settings = ProtobufProjectSettings.getInstance(project);
        setRoots(settings.getProtoRoots());
        protocField.setText(settings.getProtocPath());
    }

    private List<String> getRoots() {
        List<String> roots = new ArrayList<>();
        for (int i = 0; i < rootsModel.size(); i++) roots.add(rootsModel.get(i));
        return roots;
    }

    private void setRoots(List<String> roots) {
        rootsModel.clear();
        roots.forEach(rootsModel::addElement);
    }
}
