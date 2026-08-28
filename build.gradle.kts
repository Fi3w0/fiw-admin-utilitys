plugins {
	id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
	id("fabric-loom") apply false
	id("net.neoforged.moddev") apply false
}

allprojects {
	group = providers.gradleProperty("maven_group").get()
	version = providers.gradleProperty("mod_version").get()

	repositories {
		mavenCentral()
		maven("https://maven.fabricmc.net/")
		maven("https://maven.neoforged.net/releases")
		maven("https://maven.nucleoid.xyz/")
	}
}
