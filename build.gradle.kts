import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    // Kotlin support
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "1.1.6"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.3.1"

    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.18.1"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.expediagroup.graphql") version "4.2.0"
    id("org.jetbrains.qodana") version "0.1.12"
}

val javaVersion = properties("javaVersion")
group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

detekt {
    config = files("detekt-config.yml")
    source = files(
        "src/main/kotlin",
    )
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    verbose.set(true)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("src/main/kotlin/**", "src/test/kotlin/**")
    }
}

dependencies {
    implementation(enforcedPlatform("io.ktor:ktor-bom:1.5.4"))
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:1.5.31"))
    implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.5.2"))
    // implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.1.0"))

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")

    implementation("io.ktor:ktor-client")
    implementation("io.ktor:ktor-client-apache")
    implementation("io.ktor:ktor-client-serialization")
    implementation("io.ktor:ktor-client-logging")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("com.github.Ricky12Awesome:json-schema-serialization:0.6.6")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
    implementation("me.xdrop:fuzzywuzzy:1.3.1")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:4.2.0")
    implementation("com.expediagroup:graphql-kotlin-client-serialization:4.2.0")

    testImplementation("org.awaitility:awaitility:4.1.0")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    downloadSources.set(properties("platformDownloadSources").toBoolean())
    updateSinceUntilBuild.set(true)

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(properties("pluginVersion"))
    groups.set(emptyList())
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT").toBoolean())
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    findByName("graphqlGenerateTestClient")?.enabled = false
    val graphqlGenerateClient by getting(GraphQLGenerateClientTask::class) {
        packageName.set("com.github.jensim.megamanipulator.graphql.generated.gitlab")
        queryFiles.from(
            *fileTree("${project.projectDir.absolutePath}/src/test/resources/graphql/gitlab/queries").files.toTypedArray()
        )
        schemaFile.set(file("${project.projectDir.absolutePath}/src/test/resources/graphql/gitlab/gitlab.graphql.schema"))
        serializer.set(GraphQLSerializer.KOTLINX)
    }
    val graphqlGenerateSourcegraphClient by register("graphqlGenerateSourcegraphClient", GraphQLGenerateClientTask::class) {
        packageName.set("com.github.jensim.megamanipulator.graphql.generated.sourcegraph")
        queryFiles.from(
            *fileTree("${project.projectDir.absolutePath}/src/test/resources/graphql/sourcegraph/queries").files.toTypedArray()
        )
        schemaFile.set(file("${project.projectDir.absolutePath}/src/test/resources/graphql/sourcegraph/sourcegraph.graphql.schema"))
        serializer.set(GraphQLSerializer.KOTLINX)
    }
    val graphqlGenerateGithubClient by register("graphqlGenerateGithubClient", GraphQLGenerateClientTask::class) {
        packageName.set("com.github.jensim.megamanipulator.graphql.generated.github")
        queryFiles.from(
            *fileTree("${project.projectDir.absolutePath}/src/test/resources/graphql/github/queries").files.toTypedArray()
        )
        schemaFile.set(file("${project.projectDir.absolutePath}/src/test/resources/graphql/github/github.graphql.schema"))
        serializer.set(GraphQLSerializer.KOTLINX)
    }

    // Set the compatibility jvm versions
    withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    withType<KotlinCompile> {
        dependsOn(graphqlGenerateGithubClient)
        dependsOn(graphqlGenerateSourcegraphClient)
        dependsOn(graphqlGenerateClient)
        kotlinOptions.jvmTarget = javaVersion
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

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(
            provider {
                changelog.run {
                    getOrNull(properties("pluginVersion")) ?: getLatest()
                }.toHTML()
            }
        )
    }

    runPluginVerifier {
        ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}
