package com.khudim.protobufdbviewer;

import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

final class ProtoCompiler {
    private ProtoCompiler() {}

    static Path descriptorSet(Project project, List<String> configuredRoots, String configuredProtocPath) throws Exception {
        if (configuredRoots.isEmpty()) throw new IOException("No protobuf source directories are configured.");

        List<Path> roots = new ArrayList<>();
        for (String value : configuredRoots) {
            Path root = Path.of(value).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) throw new IOException("Proto directory does not exist: " + root);
            roots.add(root);
        }
        roots = roots.stream().distinct().sorted().toList();

        List<Path> sourceFiles = collectProtoFiles(roots);
        if (sourceFiles.isEmpty()) throw new IOException("No .proto files found in configured directories.");

        Path cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "protobuf-db-viewer", projectHash(project));
        Files.createDirectories(cacheDir);

        List<Path> includeRoots = new ArrayList<>(roots);
        for (Path include : standardIncludePaths()) if (Files.isDirectory(include)) includeRoots.add(include);

        Path externalIncludes = cacheDir.resolve("external-proto-includes");
        extractDependencyProtoFiles(externalIncludes);
        if (Files.isDirectory(externalIncludes)) includeRoots.add(externalIncludes);
        includeRoots = includeRoots.stream().map(Path::toAbsolutePath).distinct().sorted().toList();

        String fingerprint = fingerprint(includeRoots, sourceFiles);
        Path descriptor = cacheDir.resolve("descriptors-" + fingerprint + ".pb");
        if (Files.isRegularFile(descriptor)) return descriptor;

        Path protoc = findProtoc(configuredProtocPath);
        if (protoc == null) {
            throw new IOException("protoc was not found. Install Protocol Buffers or configure the executable in Settings | Tools | Protobuf DB Viewer.");
        }

        List<String> command = new ArrayList<>();
        command.add(protoc.toString());
        command.add("--descriptor_set_out=" + descriptor);
        command.add("--include_imports");
        command.add("--include_source_info");
        for (Path root : includeRoots) command.add("--proto_path=" + root);
        for (Path file : sourceFiles) command.add(file.toString());

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            Files.deleteIfExists(descriptor);
            throw new IOException("protoc failed (exit " + exitCode + "):\n" + output);
        }
        if (!Files.isRegularFile(descriptor)) throw new IOException("protoc did not create the descriptor set.");
        return descriptor;
    }

    private static List<Path> collectProtoFiles(List<Path> roots) throws IOException {
        Set<Path> files = new LinkedHashSet<>();
        for (Path root : roots) {
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".proto"))
                        .map(Path::toAbsolutePath)
                        .forEach(files::add);
            }
        }
        return files.stream().sorted().toList();
    }

    private static Path findProtoc(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            Path configured = Path.of(configuredPath).toAbsolutePath();
            if (Files.isExecutable(configured)) return configured;
        }
        String path = System.getenv("PATH");
        if (path != null) {
            String executable = System.getProperty("os.name", "").toLowerCase().contains("win") ? "protoc.exe" : "protoc";
            for (String directory : path.split(java.io.File.pathSeparator)) {
                Path candidate = Path.of(directory, executable);
                if (Files.isExecutable(candidate)) return candidate;
            }
        }
        for (String value : new String[]{"/opt/homebrew/bin/protoc", "/usr/local/bin/protoc", "/usr/bin/protoc"}) {
            Path candidate = Path.of(value);
            if (Files.isExecutable(candidate)) return candidate;
        }
        return null;
    }

    private static List<Path> standardIncludePaths() {
        return List.of(Path.of("/opt/homebrew/include"), Path.of("/usr/local/include"), Path.of("/usr/include"));
    }

    private static void extractDependencyProtoFiles(Path destination) throws IOException {
        Files.createDirectories(destination);
        Set<Path> repositories = new LinkedHashSet<>();
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            repositories.add(Path.of(home, ".gradle", "caches", "modules-2", "files-2.1"));
            repositories.add(Path.of(home, ".m2", "repository"));
        }
        for (Path repository : repositories) {
            if (!Files.isDirectory(repository)) continue;
            try (Stream<Path> stream = Files.find(repository, 12,
                    (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".jar"))) {
                stream.forEach(jar -> extractProtoEntries(jar, destination));
            }
        }
    }

    private static void extractProtoEntries(Path jarPath, Path destination) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            List<JarEntry> protoEntries = jar.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".proto"))
                    .toList();
            if (protoEntries.isEmpty()) return;
            for (JarEntry entry : protoEntries) copyJarEntry(jar, entry, destination);
        } catch (Exception ignored) {}
    }

    private static void copyJarEntry(JarFile jar, JarEntry entry, Path destination) {
        try {
            Path target = destination.resolve(entry.getName()).normalize();
            if (!target.startsWith(destination)) return;
            Files.createDirectories(target.getParent());
            if (Files.isRegularFile(target) && Files.size(target) == entry.getSize()) return;
            try (InputStream input = jar.getInputStream(entry)) {
                Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {}
    }

    private static String fingerprint(List<Path> roots, List<Path> files) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (Path root : roots) digest.update(root.toString().getBytes(StandardCharsets.UTF_8));
        for (Path file : files) {
            digest.update(file.toString().getBytes(StandardCharsets.UTF_8));
            digest.update(Long.toString(Files.size(file)).getBytes(StandardCharsets.UTF_8));
            digest.update(Long.toString(Files.getLastModifiedTime(file).toMillis()).getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
    }

    private static String projectHash(Project project) throws Exception {
        String identity = project.getLocationHash();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(identity.getBytes(StandardCharsets.UTF_8))).substring(0, 12);
    }
}
