package com.khudim.protobufdbviewer;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.nio.file.Path;

@Service(Service.Level.PROJECT)
public final class DescriptorRegistryService {
    private final Project project;
    private DescriptorRegistry registry;
    private long settingsRevision = -1;

    public DescriptorRegistryService(Project project) {
        this.project = project;
    }

    synchronized DescriptorRegistry getRegistry() throws Exception {
        ProtobufProjectSettings settings = ProtobufProjectSettings.getInstance(project);
        if (registry != null && settingsRevision == settings.getRevision()) return registry;

        Path descriptor = ProtoCompiler.descriptorSet(project, settings.getProtoRoots(), settings.getProtocPath());
        registry = DescriptorRegistry.load(descriptor);
        settingsRevision = settings.getRevision();
        return registry;
    }

    public synchronized void invalidate() {
        registry = null;
        settingsRevision = -1;
    }
}
