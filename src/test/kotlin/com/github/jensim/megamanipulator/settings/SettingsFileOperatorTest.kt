package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.settings.CodeHostSettings.BitBucketSettings
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.SearchHostSettings.SourceGraphSettings
import com.github.ricky12awesome.jss.encodeToSchema
import com.github.ricky12awesome.jss.globalJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class SettingsFileOperatorTest {

    private val readableJson: Json = SerializationHolder.instance.readableJson
    private val testData = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            "sourcegraph_com" to SourceGraphSettings(
                baseUrl = "https://sourcegraph.com",
                codeHostSettings = mapOf(
                    "github.com" to GitHubSettings(
                        username = "jensim",
                    )
                )
            ),
            "private_sourcegraph" to SourceGraphSettings(
                baseUrl = "https://sourcegraph.example.com",
                codeHostSettings = mapOf(
                    "github.com" to GitHubSettings(
                        username = "jensim",
                    ),
                    "bitbucket" to BitBucketSettings(
                        "https://bitbucket.server.example.com",
                        username = "jensim",
                    )
                )
            )
        ),
    )

    @Test
    fun `serialize deserialize`() {
        // given

        // when
        val json = readableJson.encodeToString(testData)
        val deserialized: MegaManipulatorSettings = readableJson.decodeFromString(json)

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
                    "sg" to SourceGraphSettings(
                        baseUrl = "https://sourcegraph.example.com",
                        httpsOverride = null,
                        codeHostSettings = mapOf()
                    )
                )
            )
        }
    }

    @Test
    fun `test default settings`() {
        val defaultFileContent = File("src/main/resources/base-files/mega-manipulator.json").readText()

        val fromFile = readableJson.decodeFromString<MegaManipulatorSettings>(defaultFileContent)
        assertEquals(fromFile, testData)
    }

    @Test
    @Disabled("Use to test schema validity, disabled because lib doesnt support minProperties for additionalProperties")
    fun `generate json schema and compare to file`() {
        val fileSystemSchema = File("src/main/resources/base-files/mega-manipulator-schema.json").readText().trim()
        val generatedSchema = globalJson.encodeToSchema(MegaManipulatorSettings.serializer(), generateDefinitions = false)

        assertEquals(fileSystemSchema, generatedSchema)
    }
}
