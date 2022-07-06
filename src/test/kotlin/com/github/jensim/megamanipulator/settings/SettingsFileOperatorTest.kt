package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.module.jsonSchema.JsonSchema
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import com.github.jensim.megamanipulator.settings.types.HttpsOverride.ALLOW_ANYTHING
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.BitBucketSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.codehost.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.codehost.GitLabSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.searchhost.SourceGraphSettings
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import java.io.File

class SettingsFileOperatorTest {

    private val testData = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            "sourcegraph_com" to SearchHostSettingsGroup(
                sourceGraph = SourceGraphSettings(
                    baseUrl = "https://sourcegraph.com",
                    codeHostSettings = mapOf(
                        "github.com" to CodeHostSettingsGroup(
                            gitHub = GitHubSettings(
                                username = "jensim",
                            )
                        )
                    )
                )
            ),
            "private_sourcegraph" to SearchHostSettingsGroup(
                sourceGraph = SourceGraphSettings(
                    baseUrl = "http://localhost:7080",
                    httpsOverride = ALLOW_ANYTHING,
                    codeHostSettings = mapOf(
                        "github.com" to CodeHostSettingsGroup(
                            gitHub = GitHubSettings(
                                username = "jensim",
                            )
                        ),
                        "bitbucket" to CodeHostSettingsGroup(
                            bitBucket = BitBucketSettings(
                                baseUrl = "http://localhost:7081",
                                httpsOverride = ALLOW_ANYTHING,
                                username = "jensim",
                            )
                        ),
                        "gitlab.com" to CodeHostSettingsGroup(
                            gitLab = GitLabSettings(
                                username = "jensim",
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
        JSONAssert.assertEquals(baseFile.readText(), jsonSchemaString, STRICT)
    }
}
