plugins {
	id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
	id("net.fabricmc.fabric-loom-remap") apply false
	id("net.fabricmc.fabric-loom") apply false
	id("net.neoforged.moddev.legacyforge") apply false
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

tasks.register<Exec>("smokeTest") {
	val module = providers.gradleProperty("smokeTarget").getOrElse("fabric-26.1.2")
	dependsOn("$module:build")
	val artifactModule = module
	commandLine("python3", ".github/scripts/smoke-test.py", module,
		"$module/build/libs/fiw-admin-tools-$artifactModule-${project.version}.jar")
	providers.gradleProperty("smokeSuiteFile").orNull?.let { args("--suite-file", it) }
	providers.gradleProperty("smokeExtraJars").orNull?.split("|")?.forEach { args("--extra-jar", it) }
	providers.gradleProperty("smokeCommands").orNull?.split("|")?.forEach { args("--command", it) }
	providers.gradleProperty("smokeExpected").orNull?.split("|")?.forEach { args("--expect", it) }
}


tasks.register<Exec>("installPlayerTestTools") {
    inputs.files(".github/player-tests/package.json", ".github/player-tests/package-lock.json")
    outputs.dir(".github/player-tests/node_modules")
    commandLine("npm", "ci", "--ignore-scripts", "--no-audit", "--no-fund",
        "--cache", "build/npm-cache", "--prefix", ".github/player-tests")
}


tasks.register<Exec>("playerTest") {
    val module = providers.gradleProperty("smokeTarget").getOrElse("fabric-26.1.2")
    dependsOn("$module:build", "installPlayerTestTools")
    val artifactModule = module
    commandLine("python3", ".github/scripts/smoke-test.py", module,
        "$module/build/libs/fiw-admin-tools-$artifactModule-${project.version}.jar",
        "--player-script", ".github/player-tests/gameplay.cjs", "--player-phases", "first,restart")
}
