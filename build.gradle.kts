plugins {
    java
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "net.teujaem"

    repositories {
        mavenCentral()

        maven {
            url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        }

    }
}
// JAVA_STUDIO_MASTER_BEGIN
apply(from = "java-studio-master.gradle")
// JAVA_STUDIO_MASTER_END
