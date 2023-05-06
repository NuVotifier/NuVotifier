import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    `java-library`
    id("fabric-loom")
}

applyPlatformAndCoreConfiguration()
applyCommonArtifactoryConfig()
applyShadowConfiguration()

configurations {
    compileClasspath.get().extendsFrom(create("shadeOnly"))
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.4")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.14.19")

    // Make a set of all api modules we wish to use
    setOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1"
    ).forEach {
        // Add each module as a dependency
        modImplementation(fabricApi.module(it, "0.80.0+1.19.4"))
    }

    "implementation"("org.yaml:snakeyaml:2.0")
    "modImplementation"("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")
    "include"("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")


    "api"(project(":nuvotifier-api"))
    "api"(project(":nuvotifier-common"))
}

tasks.named<Copy>("processResources") {
    val internalVersion = project.ext["internalVersion"]
    inputs.property("internalVersion", internalVersion)
    filesMatching("fabric.mod.json") {
        expand("internalVersion" to internalVersion)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations["shadeOnly"], project.configurations["runtimeClasspath"])

    archiveClassifier.set("dist-dev")

    dependencies {
        relocate("org.yaml", "com.vexsoftware.votifier.libs.org.yaml")
        include(dependency(":nuvotifier-api"))
        include(dependency(":nuvotifier-common"))
        include(dependency("org.yaml:snakeyaml:"))
    }
    exclude("mappings/mappings.tiny")
}

tasks.register<RemapJarTask>("remapShadowJar") {
    val shadowJar = tasks.getByName<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    inputFile.set(shadowJar.archiveFile)
    archiveFileName.set(shadowJar.archiveFileName.get().replace(Regex("-dev\\.jar$"), ".jar"))
    addNestedDependencies.set(true)
}

tasks.named("assemble").configure {
    dependsOn("remapShadowJar")
}
