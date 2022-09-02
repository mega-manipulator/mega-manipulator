import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    // Kotlin support
    kotlin("jvm") version "1.7.10"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "1.9.0"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.3.1"

    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    id("com.github.ben-manes.versions") version "0.42.0"
    id("com.expediagroup.graphql") version "6.2.2"
    id("org.jetbrains.qodana") version "0.1.13"
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
        include("**/kotlin/**")
    }
}

dependencies {
    val kotlinVersion = "1.7.10"
    val jacksonDatabindVersion = "2.13.3"

    val ktorVersion = "2.1.0"
    val kotlinCoroutinesVersion = "1.6.4"
    val graphqlKotlinKtorVersion = "6.2.2"

    implementation(enforcedPlatform(kotlin("bom", kotlinVersion)))
    implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinCoroutinesVersion"))
    implementation(enforcedPlatform("io.ktor:ktor-bom:$ktorVersion"))

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")

    implementation("io.ktor:ktor-client")
    implementation("commons-codec:commons-codec:1.15") // Fix transitive vulnerability in apache client
    implementation("io.ktor:ktor-client-apache")
    // implementation("io.ktor:ktor-client-jackson:$ktor_version") // Old
    implementation("io.ktor:ktor-serialization-jackson") // new
    implementation("io.ktor:ktor-client-content-negotiation") // new
    implementation("io.ktor:ktor-client-logging")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonDatabindVersion")

    implementation("org.eclipse.jgit:org.eclipse.jgit:6.2.0.202206071550-r")
    implementation("ch.qos.logback:logback-classic:1.4.0")
    implementation("me.xdrop:fuzzywuzzy:1.4.0")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphqlKotlinKtorVersion") {
        exclude("com.expediagroup:graphql-kotlin-client-serialization")
        exclude("org.jetbrains.kotlinx:kotlinx-serialization-json")
    }
    implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphqlKotlinKtorVersion")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")

    // Test deps
    val junitVersion = "5.9.0"
    val hamcrestVersion = "2.2"
    testImplementation(enforcedPlatform("org.junit:junit-bom:$junitVersion"))

    testImplementation("com.fasterxml.jackson.module:jackson-module-jsonSchema:$jacksonDatabindVersion")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("io.mockk:mockk:1.12.7")

    testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
    testImplementation("org.hamcrest:hamcrest-library:$hamcrestVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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
        serializer.set(GraphQLSerializer.JACKSON)
    }
    val graphqlGenerateSourcegraphClient by register("graphqlGenerateSourcegraphClient", GraphQLGenerateClientTask::class) {
        packageName.set("com.github.jensim.megamanipulator.graphql.generated.sourcegraph")
        queryFiles.from(
            *fileTree("${project.projectDir.absolutePath}/src/test/resources/graphql/sourcegraph/queries").files.toTypedArray()
        )
        schemaFile.set(file("${project.projectDir.absolutePath}/src/test/resources/graphql/sourcegraph/sourcegraph.graphql.schema"))
        serializer.set(GraphQLSerializer.JACKSON)
    }
    val graphqlGenerateGithubClient by register("graphqlGenerateGithubClient", GraphQLGenerateClientTask::class) {
        packageName.set("com.github.jensim.megamanipulator.graphql.generated.github")
        queryFiles.from(
            *fileTree("${project.projectDir.absolutePath}/src/test/resources/graphql/github/queries").files.toTypedArray()
        )
        schemaFile.set(file("${project.projectDir.absolutePath}/src/test/resources/graphql/github/github.graphql.schema"))
        serializer.set(GraphQLSerializer.JACKSON)
    }
    withType(ProcessResources::class.java) {
        // Generate a file on the classpath to be able to know your own version of the plugin and compare that against the latest version available
        listOf(File(projectDir,"src/main/resources/version"), File(buildDir,"resources/main/version")).forEach { versionFile ->
            if (!versionFile.parentFile.exists()) versionFile.parentFile.mkdirs()
            versionFile.writeText(project.version.toString())
        }
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
        kotlinOptions {
            jvmTarget = javaVersion
            freeCompilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.serialization.ImplicitReflectionSerializer",
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
                "-opt-in=kotlinx.serialization.ExperimentalCoroutinesApi",
            )
        }
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

    test {
        testLogging {
            showStandardStreams = true
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
