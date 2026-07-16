package com.khudim.protobufdbviewer;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
@State(name = "ProtobufDbViewerSettings", storages = @Storage("protobuf-db-viewer.xml"))
public final class ProtobufProjectSettings implements PersistentStateComponent<ProtobufProjectSettings.StateData> {
    public static final class MappingData {
        public String gridSignature = "";
        public String columnName = "";
        public String messageName = "";

        public MappingData() {}

        MappingData(String gridSignature, String columnName, String messageName) {
            this.gridSignature = gridSignature;
            this.columnName = columnName;
            this.messageName = messageName;
        }
    }

    public static final class StateData {
        public List<String> protoRoots = new ArrayList<>();
        public String protocPath = "";
        public List<MappingData> mappings = new ArrayList<>();
        public long revision = 0;
    }

    private StateData state = new StateData();

    public static ProtobufProjectSettings getInstance(Project project) {
        return project.getService(ProtobufProjectSettings.class);
    }

    @Override
    public @Nullable StateData getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateData state) {
        this.state = state;
        if (this.state.protoRoots == null) this.state.protoRoots = new ArrayList<>();
        if (this.state.mappings == null) this.state.mappings = new ArrayList<>();
    }

    public List<String> getProtoRoots() {
        return List.copyOf(state.protoRoots);
    }

    public String getProtocPath() {
        return state.protocPath == null ? "" : state.protocPath;
    }

    public long getRevision() {
        return state.revision;
    }

    public synchronized MappingData findMapping(String gridSignature) {
        return state.mappings.stream()
                .filter(mapping -> gridSignature.equals(mapping.gridSignature))
                .findFirst()
                .orElse(null);
    }

    public synchronized void saveMapping(String gridSignature, String columnName, String messageName) {
        MappingData existing = findMapping(gridSignature);
        if (existing == null) {
            state.mappings.add(new MappingData(gridSignature, columnName, messageName));
        } else {
            existing.columnName = columnName;
            existing.messageName = messageName;
        }
    }

    public void update(List<String> roots, String protocPath) {
        state.protoRoots = new ArrayList<>(roots);
        state.protocPath = protocPath == null ? "" : protocPath.trim();
        state.revision++;
    }
}
