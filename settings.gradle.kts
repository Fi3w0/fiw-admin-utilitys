pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		maven {
			name = "NeoForged"
			url = uri("https://maven.neoforged.net/releases")
		}
		maven {
			name = "MinecraftForge"
			url = uri("https://maven.minecraftforge.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}

	plugins {
		id("fabric-loom") version providers.gradleProperty("loom_version")
		id("net.neoforged.moddev") version providers.gradleProperty("moddev_version")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// Should match your modid
rootProject.name = "fiw-admin-tools"

include("core")
include("fabric-1.21.11")
include("neoforge-1.21.11")
include("fabric-1.21.8")
include("neoforge-1.21.8")
include("fabric-1.21.1")
include("neoforge-1.21.1")
include("fabric-1.20.1")
include("forge-1.20.1")
