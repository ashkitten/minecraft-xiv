plugins {
    id("dev.isxander.modstitch.base") version "0.5.12"
    kotlin("jvm") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
}

fun prop(name: String, consumer: (prop: String) -> Unit) {
    (findProperty(name) as? String?)
        ?.let(consumer)
}

// can't shadow modstitch.minecraftVersion
val _minecraftVersion = property("minecraft_version") as String
val yaclVersion = property("yacl_version") as String
val classTweaker = when {
    stonecutter.eval(_minecraftVersion, ">=1.20.1") -> "1.20.1.classtweaker"
    else -> throw IllegalArgumentException("No access widener specified for $_minecraftVersion")
}

modstitch {
    minecraftVersion = _minecraftVersion

    javaTarget = when {
        stonecutter.eval(_minecraftVersion, "<=1.20.4") -> 17
        stonecutter.eval(_minecraftVersion, "<=1.21.11") -> 21
        else -> 25
    }

    // If parchment doesn't exist for a version yet, you can safely
    // omit the "deps.parchment" property from your versioned gradle.properties
    parchment {
        prop("deps.parchment") { mappingsVersion = it }
    }

    // This metadata is used to fill out the information inside
    // the metadata files found in the templates folder.
    metadata {
        modId = "minecraft-xiv"
        modName = "Minecraft XIV"
        modDescription = "Changes Minecraft's third-person controls to be more like FFXIV."
        modVersion = "1.4.0"
        modGroup = "wtf.kity"
        modAuthor = "ashkitten"
        modLicense = "MIT"
    }

    // Fabric Loom (Fabric)
    loom {
        // It's not recommended to store the Fabric Loader version in properties.
        // Make sure it's up to date.
        fabricLoaderVersion = property("loader_version") as String

        // Configure loom like normal in this block.
        configureLoom {
            accessWidenerPath = rootProject.file("src/main/resources/classtweakers/$classTweaker")
        }

        tasks.processResources {
            val props = mapOf (
                "mod_id" to metadata.modId.get(),
                "mod_version" to metadata.modVersion.get(),
                "mod_name" to  metadata.modName.get(),
                "mod_description" to metadata.modDescription.get(),
                "mod_author" to metadata.modAuthor.get(),
                "github" to "ashkitten/minecraft-xiv",
                "mod_license" to "LGPL-3.0",
                "classtweaker_file" to classTweaker,
                "loader_version" to fabricLoaderVersion.get(),
                "minecraft_version" to _minecraftVersion,
                "yacl_version" to yaclVersion,
            )

            filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
                expand(props)
            }
        }
    }

    // ModDevGradle (NeoForge, Forge, Forgelike)
    moddevgradle {
        enable {
            prop("deps.forge") { forgeVersion = it }
            prop("deps.neoform") { neoFormVersion = it }
            prop("deps.neoforge") { neoForgeVersion = it }
            prop("deps.mcp") { mcpVersion = it }
        }

        // Configures client and server runs for MDG, it is not done by default
        defaultRuns()

        // This block configures the `neoforge` extension that MDG exposes by default,
        // you can configure MDG like normal from here
        configureNeoforge {
            runs.all {
                disableIdeRun()
            }
        }
    }
}

// Stonecutter constants for mod loaders.
// See https://stonecutter.kikugie.dev/stonecutter/guide/comments#condition-constants
var constraint: String = name.split("-")[1]
stonecutter {
    consts(
        "fabric" to constraint.equals("fabric"),
        "neoforge" to constraint.equals("neoforge"),
        "forge" to constraint.equals("forge"),
        "vanilla" to constraint.equals("vanilla")
    )
}

fletchingTable {
    mixins.create("main") { // Name should match an existing source set
        // Default matches the default value in the annotation
        mixin("default", "minecraftxiv.mixins.json")
    }
}

// All dependencies should be specified through modstitch's proxy configuration.
// Wondering where the "repositories" block is? Go to "stonecutter.gradle.kts"
// If you want to create proxy configurations for more source sets, such as client source sets,
// use the modstitch.createProxyConfigurations(sourceSets["client"]) function.
dependencies {
    loom {
        modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    }

    // Anything else in the dependencies block will be used for all platforms.

    modImplementation("dev.isxander:yet-another-config-lib:${property("yacl_version")}")
    modImplementation("com.terraformersmc:modmenu:${property("modmenu_version")}")
}