plugins {
    id("dev.kikugie.stonecutter")
    id("dev.isxander.modstitch.base") version "0.8.4"
    kotlin("jvm") version "2.3.0"
    id("com.google.devtools.ksp") version "2.3.4"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
    id("me.modmuss50.mod-publish-plugin")
}

fun prop(name: String, consumer: (prop: String) -> Unit) {
    (findProperty(name) as? String?)
        ?.let(consumer)
}

val minecraft = property("minecraft_version") as String
val minecraftDep = property("minecraft_dependency") as String
val yaclVersion = property("yacl_version") as String

val isFabric = modstitch.isLoom
val isNeoforge = modstitch.isModDevGradleRegular
val isForge = modstitch.isModDevGradleLegacy
val isForgeLike = modstitch.isModDevGradle
val loader = when {
    isFabric -> "fabric"
    isNeoforge -> "neoforge"
    isForge -> "forge"
    else -> error("Unknown loader")
}

modstitch {
    this.minecraftVersion = minecraft

    val classTweaker = when {
        stonecutter.eval(minecraft, "<1.21") -> "1.20.1.classtweaker"
        stonecutter.eval(minecraft, "<1.21.11") -> "1.21.1.classtweaker"
        stonecutter.eval(minecraft, "<26") -> "1.21.11.classtweaker"
        stonecutter.eval(minecraft, ">26") -> "26.1.classtweaker"
        else -> throw IllegalArgumentException("No access widener specified for $minecraft")
    }

    // If parchment doesn't exist for a version yet, you can safely
    // omit the "deps.parchment" property from your versioned gradle.properties
    parchment {
        prop("deps.parchment") { mappingsVersion = it }
    }

    metadata {
        modId = property("mod_id") as String
        modName = property("mod_name") as String
        modDescription = property("mod_description") as String
        modVersion = property("mod_version") as String
        modGroup = property("mod_group") as String
        modAuthor = property("mod_author") as String
        modLicense = property("mod_license") as String
    }

    // Fabric Loom (Fabric)
    loom {
        // It's not recommended to store the Fabric Loader version in properties.
        // Make sure it's up to date.
        fabricLoaderVersion = property("loader_version") as String

        // Configure loom like normal in this block.
        configureLoom {
            accessWidenerPath = rootProject.file("src/main/resources/classtweakers/${classTweaker}")
        }

        tasks.processResources {
            val props = mapOf (
                "mod_id" to metadata.modId.get(),
                "mod_version" to metadata.modVersion.get(),
                "mod_name" to  metadata.modName.get(),
                "mod_description" to metadata.modDescription.get(),
                "mod_author" to metadata.modAuthor.get(),
                "github" to "ashkitten/minecraft-xiv",
                "mod_license" to metadata.modLicense.get(),
                "classtweaker_file" to classTweaker,
                "loader_version" to fabricLoaderVersion.get(),
                "java_version" to modstitch.javaVersion.get(),
                "minecraft_version" to minecraftDep,
                "yacl_version" to yaclVersion,
            )

            filesMatching(listOf("fabric.mod.json", "minecraftxiv.mixins.json")) {
                expand(props)
            }
        }
    }

    // ModDevGradle (NeoForge, Forge, Forgelike)
    moddevgradle {
        prop("deps.forge") { forgeVersion = it }
        prop("deps.neoform") { neoFormVersion = it }
        prop("deps.neoforge") { neoForgeVersion = it }
        prop("deps.mcp") { mcpVersion = it }

        // Configures client and server runs for MDG, it is not done by default
        defaultRuns()

        // This block configures the `neoforge` extension that MDG exposes by default,
        // you can configure MDG like normal from here
        configureNeoForge {
            runs.all {
                disableIdeRun()
            }
        }
    }
}

stonecutter {
    replacements {
        string(current.parsed >= "1.21") {
            replace("me.jellysquid", "net.caffeinemc")
        }
        string(current.parsed > "1.21.1") {
            replace("ChunkProgressListenerFactory", "LevelLoadListener")
            replace("pushPose", "pushMatrix")
            replace("popPose", "popMatrix")
        }
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
            replace("getTimer", "getDeltaTracker")
            replace("canInteractWithBlock", "isWithinBlockInteractionRange")
        }
        string(current.parsed >= "26.1") {
            replace("playS2C", "clientboundPlay")
            replace("playC2S", "serverboundPlay")
            replace("keybinding", "keymapping")
            replace("KeyBindingHelper", "KeyMappingHelper")
            replace("registerKeyBinding", "registerKeyMapping")
        }
    }
}

fletchingTable {
    mixins.create("main") { // Name should match an existing source set
        // Default matches the default value in the annotation
        mixin("default", "minecraftxiv.mixins.json") {
            // Makes all mixins be registered in the "client" block by default.
            env("CLIENT", "wtf.kity.minecraftxiv.mixin.client")

            // Makes mixins in the provided packages be registered in the "server" block.
            env("SERVER", "wtf.kity.minecraftxiv.mixin.server")
        }
    }
}

// All dependencies should be specified through modstitch's proxy configuration.
// Wondering where the "repositories" block is? Go to "stonecutter.gradle.kts"
// If you want to create proxy configurations for more source sets, such as client source sets,
// use the modstitch.createProxyConfigurations(sourceSets["client"]) function.
dependencies {
    val impl = if (stonecutter.eval(minecraft, "<26")) "modImplementation" else "implementation"

    loom {
        impl("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    }

    // Anything else in the dependencies block will be used for all platforms.

    impl("dev.isxander:yet-another-config-lib:${property("yacl_version")}")
    impl("com.terraformersmc:modmenu:${property("modmenu_version")}")
    findProperty("sodium_version")?.let { impl("maven.modrinth:sodium:$it") }
}

publishMods {
    from(rootProject.publishMods)
    dryRun = rootProject.publishMods.dryRun

    file = modstitch.finalJarTask.flatMap { it.archiveFile }

    displayName = "${modstitch.metadata.modVersion.get()} for $loader $minecraft"
    modLoaders.add(loader)

    changelog = rootProject.file("CHANGELOG.md").readText()

    type = when {
        "alpha" in modstitch.metadata.modVersion.get() -> ALPHA
        "beta" in modstitch.metadata.modVersion.get() -> BETA
        else -> STABLE
    }

    fun versionList(prop: String) = findProperty(prop)?.toString()
        ?.split(',')
        ?.map { it.trim() }
        ?: emptyList()

    // modrinth and curseforge use different formats for snapshots. this can be expressed globally
    val stableCompat = versionList("stable_compat")

    modrinth {
        accessToken = System.getenv("MODRINTH_TOKEN")

        projectId = "fNaJhObx"

        minecraftVersions.addAll(stableCompat)
        minecraftVersions.addAll(versionList("modrinth_compat"))

        announcementTitle = "Download $minecraft for ${loader.replaceFirstChar { it.uppercase() }} from Modrinth"

        requires { slug.set("yacl") }

        if (modstitch.isLoom) {
            requires { slug.set("fabric-api") }
            optional { slug.set("modmenu") }
        }
    }
}