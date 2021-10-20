import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.closure
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    addPlugins()
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
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.detekt}")
    this.dependencies.addDependencies()
}

ktlint {
    verbose.set(true)
    version.set(Versions.ktlint)
    filter {
        exclude("**/graphql/generated/**")
    }
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

    withType<Detekt> {
        jvmTarget = Versions.jvm
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
