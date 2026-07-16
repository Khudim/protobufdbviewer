package com.khudim.protobufdbviewer;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class DescriptorRegistry {
    private final Map<String, Descriptors.Descriptor> messages;

    private DescriptorRegistry(Map<String, Descriptors.Descriptor> messages) {
        this.messages = messages;
    }

    static DescriptorRegistry load(Path path) throws Exception {
        DescriptorProtos.FileDescriptorSet set = DescriptorProtos.FileDescriptorSet.parseFrom(Files.readAllBytes(path));
        Map<String, DescriptorProtos.FileDescriptorProto> protos = new LinkedHashMap<>();
        for (DescriptorProtos.FileDescriptorProto proto : set.getFileList()) protos.put(proto.getName(), proto);

        Map<String, Descriptors.FileDescriptor> built = new HashMap<>();
        for (String name : protos.keySet()) buildFile(name, protos, built, new ArrayList<>());

        Map<String, Descriptors.Descriptor> messages = new LinkedHashMap<>();
        for (Descriptors.FileDescriptor file : built.values()) {
            for (Descriptors.Descriptor descriptor : file.getMessageTypes()) collect(descriptor, messages);
        }
        return new DescriptorRegistry(messages);
    }

    Descriptors.Descriptor resolveByMessageName(String messageName) {
        String requested = messageName == null ? "" : messageName.trim();
        if (requested.isBlank()) throw new IllegalArgumentException("Protobuf message name is empty.");

        Descriptors.Descriptor fullNameMatch = messages.get(requested);
        if (fullNameMatch != null) return fullNameMatch;

        List<Descriptors.Descriptor> simpleNameMatches = messages.values().stream()
                .filter(message -> message.getName().equalsIgnoreCase(requested))
                .toList();
        if (simpleNameMatches.size() == 1) return simpleNameMatches.get(0);
        if (simpleNameMatches.isEmpty()) {
            throw new IllegalArgumentException("Protobuf message not found: " + requested);
        }

        String matches = simpleNameMatches.stream()
                .map(Descriptors.Descriptor::getFullName)
                .sorted()
                .collect(Collectors.joining("\n"));
        throw new IllegalArgumentException("More than one message is named '" + requested + "'. Enter a full name:\n" + matches);
    }

    private static Descriptors.FileDescriptor buildFile(
            String name,
            Map<String, DescriptorProtos.FileDescriptorProto> protos,
            Map<String, Descriptors.FileDescriptor> built,
            List<String> stack
    ) throws Exception {
        Descriptors.FileDescriptor existing = built.get(name);
        if (existing != null) return existing;
        if (stack.contains(name)) throw new IOException("Cyclic descriptor dependency: " + stack + " -> " + name);
        DescriptorProtos.FileDescriptorProto proto = protos.get(name);
        if (proto == null) throw new IOException("Missing imported descriptor: " + name);

        stack.add(name);
        List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
        for (String dependency : proto.getDependencyList()) {
            dependencies.add(buildFile(dependency, protos, built, stack));
        }
        stack.remove(stack.size() - 1);

        Descriptors.FileDescriptor result = Descriptors.FileDescriptor.buildFrom(
                proto,
                dependencies.toArray(Descriptors.FileDescriptor[]::new)
        );
        built.put(name, result);
        return result;
    }

    private static void collect(Descriptors.Descriptor descriptor, Map<String, Descriptors.Descriptor> messages) {
        messages.put(descriptor.getFullName(), descriptor);
        for (Descriptors.Descriptor nested : descriptor.getNestedTypes()) collect(nested, messages);
    }
}
