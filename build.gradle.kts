plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.khudim.protobufdbviewer"
version = "0.5.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:4.33.2")
    implementation("com.google.protobuf:protobuf-java-util:4.33.2")

    intellijPlatform {
        intellijIdeaUltimate("2025.3.3")
        bundledPlugin("com.intellij.database")
    }
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion { sinceBuild = "253" }
        changeNotes = """
            <p>Live preview tool window.</p>
            <ul>
              <li>Decode selected binary database cells as protobuf JSON.</li>
              <li>Live preview updates automatically when the selected database row changes.</li>
              <li>Project-level configuration with multiple proto roots.</li>
              <li>Automatic descriptor generation and per-grid message selection cache.</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    buildSearchableOptions { enabled = false }
}
