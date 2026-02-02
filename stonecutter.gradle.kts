plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.0-fabric"

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.isxander.dev/releases")
        maven("https://maven.terraformersmc.com/")
    }
}