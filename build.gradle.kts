import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    addPlugins()
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    this.dependencies.addDependencies()
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
        sourceCompatibility = Versions.jvm
        targetCompatibility = Versions.jvm
    }
    withType<KotlinCompile> {
        dependsOn(graphqlGenerateGithubClient)
        dependsOn(graphqlGenerateSourcegraphClient)
        dependsOn(graphqlGenerateClient)
        kotlinOptions.jvmTarget = Versions.jvm
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
        changeNotes.set(provider {
            changelog.run {
                getOrNull(properties("pluginVersion")) ?: getLatest()
            }.toHTML()
        })
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
