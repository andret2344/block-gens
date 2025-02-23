/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

plugins {
	java
	jacoco
	`maven-publish`
	id("org.barfuin.gradle.jacocolog") version "3.1.0"
	id("com.gradleup.shadow") version "8.3.6"
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
	compileOnly(libs.spigot.api)
	compileOnly(libs.jetbrains.annotations)
	implementation(libs.bstats.bukkit)
	implementation(libs.ats.arguments)

	mockitoAgent(libs.mockito.core) { isTransitive = false }
	testCompileOnly(libs.jetbrains.annotations)
	testImplementation(libs.assertj.core)
	testImplementation(libs.mockito.core)
	testImplementation(libs.mockito.testng)
	testImplementation(libs.spigot.api)
	testImplementation(libs.testng)
}

tasks {
	compileJava {
		sourceCompatibility = JavaVersion.VERSION_17.toString()
		targetCompatibility = JavaVersion.VERSION_17.toString()
		options.compilerArgs.addAll(listOf("-parameters", "-g", "-Xlint:deprecation", "-Xlint:unchecked"))
	}

	test {
		useTestNG()
		finalizedBy(jacocoTestCoverageVerification, jacocoAggregatedReport)
		jvmArgs("-javaagent:${mockitoAgent.asPath}")
	}

	jacocoTestReport {
		classDirectories.setFrom(classDirectories.files.map {
			fileTree(it).matching {
				exclude("**/*Plugin.*")
			}
		})
	}

	jacocoTestCoverageVerification {
		violationRules {
			rule {
				classDirectories.setFrom(jacocoTestReport.get().classDirectories)
				limit {
					minimum = "0.8".toBigDecimal()
				}
			}
		}
	}

	build {
		dependsOn(shadowJar)
	}

	shadowJar {
		archiveFileName.set("${project.name}-${project.version}.jar")
		relocate("org.bstats", "${project.group}.blockgenerator.bstats")
	}

	publishing {
		publications {
			create<MavenPublication>("maven") {
				artifact(jar)
				groupId = project.properties["group"].toString()
				version = project.properties["version"].toString()
				artifactId = project.properties["artifact"].toString()
			}
		}
		repositories {
			maven {
				name = "GitLab"

				url = uri("https://gitlab.com/api/v4/projects/6382356/packages/maven")
				credentials(HttpHeaderCredentials::class) {
					name = "Job-Token"
					value = System.getenv("CI_JOB_TOKEN")
				}
				authentication {
					create<HttpHeaderAuthentication>("header")
				}
			}
		}
	}
}
