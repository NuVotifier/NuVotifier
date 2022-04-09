plugins {
    `java-library`
}

applyPlatformAndCoreConfiguration()
applyCommonArtifactoryConfig()

dependencies {
    "implementation"("com.google.code.gson:gson:${Versions.GSON}")
}
