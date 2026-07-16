# Protobuf DB Viewer

An IntelliJ IDEA Ultimate / DataGrip plugin that decodes a selected binary database cell as protobuf and displays formatted JSON.

## Features

- Works from database result grids.
- Supports binary values exposed as `byte[]`, JDBC `Blob`, `ByteBuffer`, and common IDE wrapper objects.
- Configures one or more directories containing `.proto` files per project.
- Prompts for proto directories on first use and stores them in `.idea/protobuf-db-viewer.xml`.
- Resolves imports across all configured directories.
- Finds additional `.proto` resources in local Gradle and Maven dependency caches.
- Uses a locally installed `protoc` to generate a descriptor set.
- Caches compiled descriptors and the in-memory descriptor registry.
- Accepts a simple message name such as `Transaction`, or a fully qualified name such as `com.example.Transaction`.
- Remembers the selected message for each open result grid.
- Does not infer schemas from table or column names.

## Requirements

- IntelliJ IDEA Ultimate or DataGrip with Database Tools.
- Java 21 for building the plugin.
- `protoc` installed locally.

On macOS:

```bash
brew install protobuf
```

The `protoc` executable can also be configured in:

`Settings | Tools | Protobuf DB Viewer`

## Usage

1. Open a database table or query result.
2. Select a binary cell.
3. Right-click and choose **View as Protobuf**.
4. On first use, select one or more directories containing `.proto` files.
5. Enter a protobuf message name, for example `Transaction`.
6. The decoded message is displayed as formatted JSON.

The message name is remembered for the current result grid. Open another result grid to choose a different message.

## Settings

Open:

`Settings | Tools | Protobuf DB Viewer`

You can:

- add or remove multiple proto source directories;
- optionally configure an explicit `protoc` executable;
- trigger descriptor rebuilding by applying changed settings.

## Development

```bash
gradle runIde
```

Build an installable ZIP:

```bash
gradle buildPlugin
```

The artifact is created under:

```text
build/distributions/
```

## Marketplace publication

Before publishing, replace the placeholder vendor email and URL in `src/main/resources/META-INF/plugin.xml`, add a real plugin icon, and verify the plugin against the IDE versions you plan to support.
