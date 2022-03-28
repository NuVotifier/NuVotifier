import org.ajoberstar.grgit.Grgit

plugins {
    id("net.kyori.blossom") version "1.2.0" apply false
}

logger.lifecycle("""
*******************************************
 You are building NuVotifier!
 If you encounter trouble:
 1) Try running 'build' in a separate Gradle run
 2) Use gradlew and not gradle
 3) If you still need help, you should reconsider building NuVotifier!

 Output files will be in [subproject]/build/libs
*******************************************
""")


applyRootArtifactoryConfig()

if (!project.hasProperty("gitCommitHash")) {
    apply(plugin = "org.ajoberstar.grgit")
    ext["gitCommitHash"] = try {
        Grgit.open(mapOf("currentDir" to project.rootDir))?.head()?.abbreviatedId
    } catch (e: Exception) {
        logger.warn("Error getting commit hash", e)

        "no.git.id"
    }
}
/*
subprojects {
    apply plugin: 'java'

    repositories {
        mavenCentral()
        maven {
            url "https://hub.spigotmc.org/nexus/content/repositories/snapshots"
        }
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots"
        }
        maven {
            url "https://repo.spongepowered.org/maven"
        }
        maven {
            url "https://repo.velocitypowered.com/snapshots/"
        }
    }

    dependencies {
        testImplementation "org.junit.jupiter:junit-jupiter-api:5.8.2"
        testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:5.8.2"
        testImplementation "org.junit.jupiter:junit-jupiter-params:5.8.2"
        testImplementation "org.mockito:mockito-core:4.4.0"
    }

    processResources {
        filter(ReplaceTokens, tokens: ["app.version": this.project.version])
    }

    test {
        useJUnitPlatform()
    }
}
*/