package com.khudim.protobufdbviewer;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

final class ProtoRootsDialog extends DialogWrapper {
    private final Project project;
    private final DefaultListModel<String> model = new DefaultListModel<>();

    ProtoRootsDialog(Project project, List<String> initialRoots) {
        super(project);
        this.project = project;
        initialRoots.forEach(model::addElement);
        setTitle("Protobuf Source Directories");
        setOKButtonText("Save");
        init();
    }

    List<String> getRoots() {
        List<String> roots = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) roots.add(model.get(i));
        return roots;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBList<String> list = new JBList<>(model);
        JComponent decorated = ToolbarDecorator.createDecorator(list)
                .setAddAction(button -> addDirectories())
                .setRemoveAction(button -> {
                    for (String value : list.getSelectedValuesList()) model.removeElement(value);
                })
                .disableUpDownActions()
                .createPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(JBUI.size(650, 300));
        panel.add(new JBScrollPane(decorated), BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void doOKAction() {
        if (model.isEmpty()) {
            Messages.showErrorDialog(project, "Add at least one directory containing .proto files.", "Protobuf DB Viewer");
            return;
        }
        super.doOKAction();
    }

    private void addDirectories() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, true)
                .withTitle("Choose Protobuf Source Directories")
                .withDescription("Choose one or more directories that contain .proto files. Imports are resolved across all selected directories.");
        VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
        for (VirtualFile file : files) {
            String path = file.getPath();
            if (!contains(path)) model.addElement(path);
        }
    }

    private boolean contains(String value) {
        for (int i = 0; i < model.size(); i++) if (model.get(i).equals(value)) return true;
        return false;
    }
}
