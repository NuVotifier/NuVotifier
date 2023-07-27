import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
}

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

configurations {
    compileClasspath.get().extendsFrom(create("shadeOnly"))
}

dependencies {
    "implementation"(project(":nuvotifier-api"))
    "implementation"(project(":nuvotifier-common"))
    "implementation"(project(":nuvotifier-bukkit"))
    "implementation"(project(":nuvotifier-bungeecord"))
    "implementation"(project(":nuvotifier-sponge"))
    "implementation"(project(":nuvotifier-sponge8"))
    "implementation"(project(":nuvotifier-fabric"))
    "implementation"(project(":nuvotifier-velocity"))
}

tasks.named<Jar>("jar") {
    val projectVersion = project.version
    inputs.property("projectVersion", projectVersion)
    manifest {
        attributes("Implementation-Version" to projectVersion)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations["shadeOnly"], project.configurations["runtimeClasspath"])

    dependencies {
        include(dependency(":nuvotifier-api"))
        include(dependency(":nuvotifier-common"))
        include(dependency(":nuvotifier-bukkit"))
        include(dependency(":nuvotifier-bungeecord"))
        include(dependency(":nuvotifier-sponge"))
        include(dependency(":nuvotifier-sponge8"))
        include(dependency(":nuvotifier-fabric"))
        include(dependency(":nuvotifier-velocity"))
    }

    exclude("GradleStart**")
    exclude(".cache");
    exclude("LICENSE*")
    exclude("META-INF/services/**")
    exclude("META-INF/maven/**")
    exclude("META-INF/versions/**")
    exclude("org/intellij/**")
    exclude("org/jetbrains/**")
    exclude("**/module-info.class")
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}
