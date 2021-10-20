import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.jacoco
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.version
import org.gradle.plugin.use.PluginDependenciesSpec

object Versions {

    const val jvm = "11"
    const val junit = "5.8.1"
    const val kotlinxCoroutines = "1.5.2"
    const val ktor = "1.5.4"
    const val kotlin = "1.5.31"
    const val hamcrest = "2.2"

    const val changelog = "1.3.1"
    const val intellij = "1.1.6"
    const val qodana = "0.1.12"
    const val benManesVersions = "0.39.0"

    const val kotlinxSerialization = "1.1.0"
    const val jsonSchemaSerialization = "0.6.6"
    const val jGit = "5.11.1.202105131744-r"
    const val log4j = "2.14.1"
    const val fuzzyWuzzy = "1.3.1"
    const val mockk = "1.11.0"
    const val graphql = "4.1.1"
}

object Dependencies {

    val implementation = setOf(
            "org.jetbrains.kotlin:kotlin-bom:${Versions.kotlin}",//:pom",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}",
            "org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Versions.kotlinxCoroutines}",
            "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.kotlinxCoroutines}",
            "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}",

            "io.ktor:ktor-client:${Versions.ktor}",
            "io.ktor:ktor-client-apache:${Versions.ktor}",
            "io.ktor:ktor-client-serialization:${Versions.ktor}",
            "io.ktor:ktor-client-logging:${Versions.ktor}",
            "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}",
            "com.github.Ricky12Awesome:json-schema-serialization:${Versions.jsonSchemaSerialization}",

            "org.eclipse.jgit:org.eclipse.jgit:${Versions.jGit}",
            "org.apache.logging.log4j:log4j-slf4j-impl:${Versions.log4j}",
            "me.xdrop:fuzzywuzzy:${Versions.fuzzyWuzzy}",
            "com.expediagroup:graphql-kotlin-ktor-client:${Versions.graphql}",
            "com.expediagroup:graphql-kotlin-client-serialization:${Versions.graphql}",
    )
    val testImplementation = setOf(
            "org.awaitility:awaitility:4.1.0",
            "io.mockk:mockk:${Versions.mockk}",
            "org.hamcrest:hamcrest:${Versions.hamcrest}",
            "org.hamcrest:hamcrest-library:${Versions.hamcrest}",
            "org.junit.jupiter:junit-jupiter-api:${Versions.junit}",
            "org.junit.jupiter:junit-jupiter-params:${Versions.junit}",
    )
    val testRuntime = setOf(
            "org.junit.jupiter:junit-jupiter-engine:${Versions.junit}",
    )
}

fun PluginDependenciesSpec.addPlugins() {
    id("java")
    // Kotlin support
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version Versions.intellij
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version Versions.changelog

    id("com.github.ben-manes.versions") version Versions.benManesVersions
    id("com.expediagroup.graphql") version Versions.graphql
    id("org.jetbrains.qodana") version Versions.qodana
}

fun DependencyHandler.addDependencies() {
    //detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.detekt}")
    Dependencies.implementation.forEach { this.add("implementation", it) }
    Dependencies.testImplementation.forEach { this.add("testImplementation", it) }
    Dependencies.testRuntime.forEach { this.add("testRuntimeOnly", it) }
}
