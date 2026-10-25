plugins {
 java
 id("com.gradleup.shadow") version "9.6.1"
}

repositories {
 mavenCentral()
 maven {
  url = uri("https://jitpack.io")
 }
 maven {
  url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
 }
}

dependencies {
 compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
 implementation("net.kyori:adventure-api:4.26.1")
 implementation("net.kyori:adventure-text-minimessage:4.26.1")
 implementation("net.kyori:adventure-platform-bukkit:4.4.1")
}
