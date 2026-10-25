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
 maven {
  url = uri("https://repo.papermc.io/repository/maven-public/")
 }
 maven {
  url = uri("https://libraries.minecraft.net")
 }
}

dependencies {
 compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
 implementation("net.kyori:adventure-api:4.26.1")
 implementation("net.kyori:adventure-text-minimessage:4.26.1")
 implementation("net.kyori:adventure-platform-bukkit:4.4.1")
 implementation("org.incendo:cloud-paper:2.0.1")
 compileOnly("com.mojang:brigadier:1.0.18")
}

tasks.shadowJar {
 mergeServiceFiles()
 relocate("net.kyori", "com.lotusplugins.sheriff.libs.kyori")
 relocate("org.incendo.cloud", "com.lotusplugins.sheriff.libs.cloud")
}
