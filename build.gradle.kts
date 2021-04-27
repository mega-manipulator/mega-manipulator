import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.closure
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitVersion: String = "5.7.1"
val kotlinxCoroutinesVersion: String = "1.4.3"
val ktorVersion: String = "1.5.3"
val kotlinVersion: String = "1.4.32"
val jvmVersion: String = "11"
val hamcrestVersion: String = "2.2"

plugins {
    // Java support
    id("java")
    // Kotlin support
    kotlin("jvm") version "1.5.0"
    kotlin("plugin.serialization") version "1.4.32"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "0.7.2"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.1.2"
    // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
    id("io.gitlab.arturbosch.detekt") version "1.16.0"
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    jacoco
    id("com.github.ben-manes.versions") version "0.38.0"
}

// Import variables from gradle.properties file
val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project

val platformType: String by project
val platformVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    jcenter()
}

dependencies {
    detektPlugins(group = "io.gitlab.arturbosch.detekt", name = "detekt-formatting", version = "1.16.0")
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = kotlinVersion)
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-jdk8", version = kotlinxCoroutinesVersion)
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-reflect", version = kotlinVersion)
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-bom", version = kotlinVersion, ext = "pom")

    implementation(group = "io.ktor", name = "ktor-client", version = ktorVersion)
    implementation(group = "io.ktor", name = "ktor-client-apache", version = ktorVersion)
    implementation(group = "io.ktor", name = "ktor-client-serialization", version = ktorVersion)
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.1.0")
    implementation(group = "com.github.Ricky12Awesome", name = "json-schema-serialization", version = "0.6.6")

    implementation(group = "org.eclipse.jgit", name = "org.eclipse.jgit", version = "5.11.0.202103091610-r")
    implementation(group = "org.apache.logging.log4j", name = "log4j-slf4j-impl", version = "2.14.1")
    implementation(group = "me.xdrop", name = "fuzzywuzzy", version = "1.3.1")

    // TEST
    testImplementation(group = "io.mockk", name = "mockk", version = "1.11.0")
    testImplementation(group = "org.hamcrest", name = "hamcrest", version = hamcrestVersion)
    testImplementation(group = "org.hamcrest", name = "hamcrest-library", version = hamcrestVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
}

ktlint {
    verbose.set(true)
    version.set("0.41.0")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName = pluginName_
    version = platformVersion
    type = platformType
    downloadSources = platformDownloadSources.toBoolean()
    updateSinceUntilBuild = true

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    setPlugins(*platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray())
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version = pluginVersion
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    // Set the compatibility jvm versions
    withType<JavaCompile> {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = jvmVersion
    }

    withType<Detekt> {
        jvmTarget = jvmVersion
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    named<DependencyUpdatesTask>("dependencyUpdates").configure {
        gradleReleaseChannel = "current"
        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }
    }

    jacocoTestReport {
        reports {
            xml.isEnabled = true
            csv.isEnabled = false
            html.destination = file("$buildDir/jacocoHtml")
        }
    }

    patchPluginXml {
        version(pluginVersion)
        sinceBuild(pluginSinceBuild)
        untilBuild(pluginUntilBuild)

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription(
            closure {
                File(project.projectDir, "README.md").readText().lines().run {
                    val start = "<!-- Plugin description -->"
                    val end = "<!-- Plugin description end -->"

                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end))
                }.joinToString("\n").run { markdownToHTML(this) }
            }
        )

        // Get the latest available change notes from the changelog file
        changeNotes(
            closure {
                changelog.getLatest().toHTML()
            }
        )
    }

    runPluginVerifier {
        ideVersions(pluginVerifierIdeVersions)
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first())
    }
}
