plugins {
    java
}

val id = project.property("id") as String
val extensionName = project.property("name") as String
val author = project.property("author") as String
val version = project.version as String
val geyserApiVersion = "2.9.2"

repositories {
    // Repo for the Geyser API artifacts
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")

    // Add other repositories here
    mavenCentral()
}

dependencies {
    // Geyser API - needed for all extensions
    compileOnly("org.geysermc.geyser:api:$geyserApiVersion-SNAPSHOT")

    // Internal access for packet interception and entity data
    compileOnly("org.geysermc.geyser:core:$geyserApiVersion-SNAPSHOT")

    // Bedrock + Java protocol access
    compileOnly("org.cloudburstmc.protocol:bedrock-codec:3.0.0.Beta11-SNAPSHOT")
    compileOnly("org.cloudburstmc.protocol:bedrock-connection:3.0.0.Beta11-SNAPSHOT")
    compileOnly("org.geysermc.mcprotocollib:protocol:1.21.11-SNAPSHOT")

    // Utilities
    compileOnly("it.unimi.dsi:fastutil:8.5.13")
    compileOnly("org.yaml:snakeyaml:2.2")
    compileOnly("org.bstats:bstats-base:3.1.0")
}

// Java currently requires Java 17 or higher, so extensions should also target it
java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

afterEvaluate {
    val idRegex = Regex("[a-z][a-z0-9-_]{0,63}")
    if (idRegex.matches(id).not()) {
        throw IllegalArgumentException("Invalid extension id $id! Must only contain lowercase letters, " +
            "and cannot start with a number.")
    }

    val nameRegex = Regex("^[A-Za-z0-9 _.-]+$")
    if (nameRegex.matches(extensionName).not()) {
        throw IllegalArgumentException("Invalid extension name $extensionName! Must fit regex: ${nameRegex.pattern})")
    }
}

tasks {
    // This automatically fills in the extension.yml file.
    processResources {
        filesMatching("extension.yml") {
            expand(
                "id" to id,
                "name" to extensionName,
                "api" to geyserApiVersion,
                "version" to version,
                "author" to author
            )
        }
    }
}

