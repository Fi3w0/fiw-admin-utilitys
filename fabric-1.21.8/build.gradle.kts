import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
	id("fabric-loom")
	id("org.jetbrains.kotlin.jvm")
}

val targetProperties = Properties().apply {
	file("gradle.properties").inputStream().use { load(it) }
}
fun targetProperty(name: String): String = targetProperties.getProperty(name)

base {
	archivesName = "${project.property("archives_base_name")}-fabric-${targetProperty("minecraft_version")}"
}

loom {
	mods {
		register("fiw-admin-tools") {
			sourceSet(sourceSets.main.get())
		}
	}
}

sourceSets {
	main {
		java.srcDir(rootProject.file("common-1.21.8/src/main/java"))
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${targetProperty("minecraft_version")}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${targetProperty("loader_version")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${targetProperty("fabric_api_version")}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${targetProperty("fabric_kotlin_version")}")
	implementation(project(":core"))
	include(project(":core"))
}

tasks.processResources {
	val properties = mapOf(
		"version" to project.version,
		"mod_id" to providers.gradleProperty("mod_id").get(),
		"mod_name" to providers.gradleProperty("mod_name").get(),
		"mod_description" to providers.gradleProperty("mod_description").get(),
		"mod_license" to providers.gradleProperty("mod_license").get(),
		"mod_author" to providers.gradleProperty("mod_author").get(),
		"minecraft_version" to targetProperty("minecraft_version"),
		"loader_version" to targetProperty("loader_version"),
		"java_version" to targetProperty("java_version")
	)

	inputs.properties(properties)

	filesMatching("fabric.mod.json") {
		expand(properties)
	}
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(targetProperty("java_version").toInt())
	}

	withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
	options.release = targetProperty("java_version").toInt()
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.fromTarget(targetProperty("java_version"))
	}
}
