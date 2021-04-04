import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
}

applyPlatformAndCoreConfiguration()
applyCommonArtifactoryConfig()
applyShadowConfiguration()

dependencies {
    compileOnly("org.spongepowered:spongeapi:8.0.0")
    implementation(project(":nuvotifier-api"))
    implementation(project(":nuvotifier-common"))
}

tasks {
    processResources {
        from(sourceSets.main.get().resources.srcDirs) {
            include("META-INF/sponge_plugins.json")
            expand(mapOf("version" to project.version))
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

tasks.named<Jar>("jar") {
    val projectVersion = project.version
    inputs.property("projectVersion", projectVersion)
    manifest {
        attributes("Implementation-Version" to projectVersion)
    }
}

// tasks.named<ShadowJar>("shadowJar") {
//     configurations = listOf(project.configurations["shadeOnly"], project.configurations["runtimeClasspath"])
// 
//     dependencies {
//         include(dependency(":nuvotifier-api"))
//         include(dependency(":nuvotifier-common"))
//     }
// 
//     exclude("GradleStart**")
//     exclude(".cache");
//     exclude("LICENSE*")
//     exclude("META-INF/services/**")
//     exclude("META-INF/maven/**")
//     exclude("META-INF/versions/**")
//     exclude("org/intellij/**")
//     exclude("org/jetbrains/**")
//     exclude("**/module-info.class")
//     exclude("*.yml")
// }

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}
