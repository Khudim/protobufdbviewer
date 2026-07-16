package com.khudim.protobufdbviewer;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicLong;

@Service(Service.Level.PROJECT)
public final class ProtobufPreviewService implements Disposable {
    private final Project project;
    private final AtomicLong refreshGeneration = new AtomicLong();
    private final PropertyChangeListener focusListener;

    private ProtobufPreviewPanel panel;
    private JTable grid;
    private int viewColumn = -1;
    private Descriptors.Descriptor descriptor;
    private ListSelectionListener rowListener;

    public ProtobufPreviewService(Project project) {
        this.project = project;
        focusListener = event -> {
            Object value = event.getNewValue();
            if (value instanceof Component component) {
                JTable focusedGrid = SelectedCellReader.findTable(component);
                if (focusedGrid != null && focusedGrid != grid) {
                    SwingUtilities.invokeLater(() -> activateGrid(focusedGrid));
                }
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("focusOwner", focusListener);
    }

    synchronized void setPanel(ProtobufPreviewPanel panel) {
        this.panel = panel;
        if (grid == null) {
            panel.setText("Select a binary cell and choose View as Protobuf.");
        } else {
            refreshLater();
        }
    }

    public void attach(JTable grid, int viewColumn, Descriptors.Descriptor descriptor) {
        synchronized (this) {
            detachListener();
            this.grid = grid;
            this.viewColumn = viewColumn;
            this.descriptor = descriptor;
            installRowListener(grid);
        }
        refreshLater();
    }

    public void activateGrid(JTable candidate) {
        ProtobufProjectSettings settings = ProtobufProjectSettings.getInstance(project);
        ProtobufProjectSettings.MappingData mapping = settings.findMapping(GridIdentity.signature(candidate));

        if (mapping == null) {
            synchronized (this) {
                detachListener();
                grid = candidate;
                viewColumn = -1;
                descriptor = null;
            }
            setPanelText("This result grid has no protobuf mapping yet.\n\n" +
                    "Select its binary column, choose View as Protobuf, and enter the message name.");
            return;
        }

        int mappedColumn = GridIdentity.findViewColumn(candidate, mapping.columnName);
        if (mappedColumn < 0) {
            setPanelText("The saved protobuf column '" + mapping.columnName + "' is not present in this result grid.\n\n" +
                    "Select the binary column and choose View as Protobuf again.");
            return;
        }

        try {
            DescriptorRegistry registry = project.getService(DescriptorRegistryService.class).getRegistry();
            Descriptors.Descriptor mappedDescriptor = registry.resolveByMessageName(mapping.messageName);
            attach(candidate, mappedColumn, mappedDescriptor);
        } catch (Exception exception) {
            String details = exception.getMessage() == null ? exception.toString() : exception.getMessage();
            setPanelText("Cannot activate the saved protobuf mapping:\n" + details);
        }
    }

    public synchronized void detach() {
        detachListener();
        grid = null;
        descriptor = null;
        viewColumn = -1;
    }

    private void installRowListener(JTable targetGrid) {
        rowListener = event -> {
            if (!event.getValueIsAdjusting()) refreshLater();
        };
        targetGrid.getSelectionModel().addListSelectionListener(rowListener);
    }

    private void detachListener() {
        if (grid != null && rowListener != null) {
            grid.getSelectionModel().removeListSelectionListener(rowListener);
        }
        rowListener = null;
    }

    private void refreshLater() {
        SwingUtilities.invokeLater(this::refresh);
    }

    private void refresh() {
        JTable currentGrid;
        int currentColumn;
        Descriptors.Descriptor currentDescriptor;
        ProtobufPreviewPanel currentPanel;

        synchronized (this) {
            currentGrid = grid;
            currentColumn = viewColumn;
            currentDescriptor = descriptor;
            currentPanel = panel;
        }

        if (currentPanel == null || currentGrid == null || currentDescriptor == null) return;

        long generation = refreshGeneration.incrementAndGet();
        currentPanel.setText("Decoding " + currentDescriptor.getFullName() + "...");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String text;
            try {
                SelectedCell cell = SelectedCellReader.read(currentGrid, currentColumn);
                DynamicMessage message = DynamicMessage.parseFrom(currentDescriptor, cell.bytes());
                text = JsonFormat.printer()
                        .includingDefaultValueFields()
                        .preservingProtoFieldNames()
                        .print(message);
            } catch (Exception exception) {
                String details = exception.getMessage() == null ? exception.toString() : exception.getMessage();
                text = "Cannot decode selected cell:\n" + details;
            }

            if (refreshGeneration.get() == generation && !project.isDisposed()) {
                setPanelText(text);
            }
        });
    }

    private void setPanelText(String text) {
        ProtobufPreviewPanel currentPanel;
        synchronized (this) {
            currentPanel = panel;
        }
        if (currentPanel != null) currentPanel.setText(text);
    }

    @Override
    public void dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removePropertyChangeListener("focusOwner", focusListener);
        detach();
    }
}
