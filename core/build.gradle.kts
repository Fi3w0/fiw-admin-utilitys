plugins {
	`java-library`
}

dependencies {
	api("com.google.code.gson:gson:2.10.1")
	// Optional at runtime: used only when the LuckPerms mod is installed on the server.
	compileOnly("net.luckperms:api:5.4")
	testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}

	withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 17
}

tasks.test {
	useJUnitPlatform()
}
