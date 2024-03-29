package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.module.jsonSchema.JsonSchema
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.KeepLocalRepos
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.BitBucketSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.codehost.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.codehost.GitLabSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.GithubSearchSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.searchhost.SourceGraphSettings
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import java.io.File

class SettingsFileOperatorTest {

    private val testData = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            "github" to SearchHostSettingsGroup(
                gitHub = GithubSearchSettings(
                    username = "jensim",
                    keepLocalRepos = KeepLocalRepos("/tmp/vcs"),
                ),
            ),
            "sourcegraph_com" to SearchHostSettingsGroup(
                sourceGraph = SourceGraphSettings(
                    username = "jensim",
                    baseUrl = "https://sourcegraph.com",
                    codeHostSettings = mapOf(
                        "github.com" to CodeHostSettingsGroup(
                            gitHub = GitHubSettings(
                                username = "jensim",
                                keepLocalRepos = KeepLocalRepos("/tmp/vcs"),
                            )
                        )
                    )
                )
            ),
            "private_sourcegraph" to SearchHostSettingsGroup(
                sourceGraph = SourceGraphSettings(
                    username = "jensim",
                    baseUrl = "http://localhost:7080",
                    httpsOverride = HttpsOverride.ALLOW_ANYTHING,
                    codeHostSettings = mapOf(
                        "github.com" to CodeHostSettingsGroup(
                            gitHub = GitHubSettings(
                                username = "jensim",
                                keepLocalRepos = KeepLocalRepos("/tmp/vcs"),
                            )
                        ),
                        "bitbucket" to CodeHostSettingsGroup(
                            bitBucket = BitBucketSettings(
                                baseUrl = "http://localhost:7081",
                                httpsOverride = HttpsOverride.ALLOW_ANYTHING,
                                username = "jensim",
                                keepLocalRepos = KeepLocalRepos("/tmp/vcs"),
                            )
                        ),
                        "gitlab.com" to CodeHostSettingsGroup(
                            gitLab = GitLabSettings(
                                username = "jensim",
                                keepLocalRepos = KeepLocalRepos("/tmp/vcs"),
                            )
                        )
                    )
                )
            )
        ),
    )

    @Test
    fun `serialize deserialize`() {
        // given

        // when
        val json = SerializationHolder.readable.writeValueAsString(testData)
        val deserialized: MegaManipulatorSettings = SerializationHolder.readable.readValue(json, MegaManipulatorSettings::class.java)

        // then
        assertEquals(deserialized, testData)
        println(json)
    }

    @Test
    fun `fail if too few entries`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            MegaManipulatorSettings(
                defaultHttpsOverride = null,
                searchHostSettings = mapOf(
                    "sg" to SearchHostSettingsGroup(
                        sourceGraph = SourceGraphSettings(
                            username = "jensim",
                            baseUrl = "https://sourcegraph.example.com",
                            httpsOverride = null,
                            codeHostSettings = mapOf()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `test default settings`() {
        val file = File("src/main/resources/base-files/soft/mega-manipulator.json")
        // file.writeText(SerializationHolder.readable.writeValueAsString(testData))
        val defaultFileContent = file.readText()

        val fromFile = SerializationHolder.readable.readValue(defaultFileContent, MegaManipulatorSettings::class.java)
        assertEquals(fromFile, testData)
    }

    @Test
    fun `generate json schema and compare to file`() {
        val baseFile = File("src/main/resources/base-files/hard/mega-manipulator-schema.json")
        val schemaGen = JsonSchemaGenerator(SerializationHolder.readable)
        val schema: JsonSchema = schemaGen.generateSchema(MegaManipulatorSettings::class.java)
        val jsonSchemaString = SerializationHolder.readable.writeValueAsString(schema)

        // baseFile.writeText(jsonSchemaString)
        val baseFileText = baseFile.readText()
        JSONAssert.assertEquals(baseFileText, jsonSchemaString, STRICT)
        assertThat(baseFileText, not(containsString(": \"\\")))
    }
}
