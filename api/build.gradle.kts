plugins {
    `java-library`
}

applyPlatformAndCoreConfiguration()

dependencies {
    "implementation"("com.google.code.gson:gson:${Versions.GSON}")
}
