plugins {
	java
	jacoco
	alias(libs.plugins.jacocolog)
	alias(libs.plugins.changelog)
	alias(libs.plugins.shadow)
	alias(libs.plugins.hangar)
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
	}
}

jacoco {
	toolVersion = libs.versions.jacoco.get()
}

val artifact = providers.gradleProperty("artifact").get()

configurations {
	// Tests run against the same server API that the plugin compiles against
	testImplementation {
		extendsFrom(configurations.compileOnly.get())
	}
}

dependencies {
	compileOnly(libs.paper.api)
	compileOnly(libs.jetbrains.annotations)
	implementation(libs.bstats.bukkit)
	implementation(libs.lamp.common)
	implementation(libs.lamp.bukkit)

	testImplementation(libs.assertj.core)
	testImplementation(libs.mockbukkit)
	testImplementation(libs.testng)
}

tasks {
	withType<JavaCompile> {
		// Lamp reads the parameter names to build the command usage
		options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation", "-Xlint:unchecked"))
	}

	processResources {
		val version = project.version.toString()
		inputs.property("version", version)
		filesMatching("plugin.yml") {
			expand("version" to version)
		}
	}

	test {
		useTestNG()
		// bStats refuses to start unless relocated, which only happens in the shadow jar
		systemProperty("bstats.relocatecheck", "false")
		finalizedBy(jacocoTestCoverageVerification, jacocoLogTestCoverage)
	}

	jacocoTestCoverageVerification {
		violationRules {
			rule {
				limit {
					minimum = "0.8".toBigDecimal()
				}
			}
		}
	}

	// The shadow jar is the only jar - the plain one would miss the shaded libraries
	jar {
		enabled = false
	}

	build {
		dependsOn(shadowJar)
	}

	withType<Jar> {
		// The suffix stops the LICENSE and NOTICE files of the shaded libraries from replacing ours
		metaInf {
			from("LICENSE", "NOTICE")
			rename { "$it-$artifact" }
		}
	}

	shadowJar {
		archiveFileName.set("${project.name}-${project.version}.jar")
		relocate("org.bstats", "${project.group}.blockgens.bstats")
		relocate("revxrsal.commands", "${project.group}.blockgens.lamp")
	}
}

changelog {
	groups.empty()
}

// Run by the release workflow, see .github/workflows/release.yml
hangarPublish {
	publications.register("plugin") {
		version = project.version.toString()
		// https://hangar.papermc.io/andret2344/BlockGens
		id = "BlockGens"
		channel = providers.gradleProperty("hangarChannel").orElse("Release")
		// Written by `getChangelog --output-file` in the release workflow
		changelog = providers.fileContents(layout.buildDirectory.file("release-notes.md")).asText.orElse("")
		apiKey = providers.environmentVariable("HANGAR_API_TOKEN")
		platforms {
			paper {
				jar = tasks.shadowJar.flatMap { it.archiveFile }
				platformVersions = providers.gradleProperty("minecraftVersions").get().split(",").map { it.trim() }
			}
		}
	}
}
