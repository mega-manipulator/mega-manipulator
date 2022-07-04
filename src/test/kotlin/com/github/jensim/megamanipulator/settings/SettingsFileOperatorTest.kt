package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.databind.JsonNode
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonModule
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class SettingsFileOperatorTest {

    private val testData = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            "sourcegraph_com" to SearchHostSettings.SourceGraphSettings(
                baseUrl = "https://sourcegraph.com",
                codeHostSettings = mapOf(
                    "github.com" to CodeHostSettings.GitHubSettings(
                        username = "jensim",
                    )
                )
            ),
            "private_sourcegraph" to SearchHostSettings.SourceGraphSettings(
                baseUrl = "https://sourcegraph.example.com",
                codeHostSettings = mapOf(
                    "github.com" to CodeHostSettings.GitHubSettings(
                        username = "jensim",
                    ),
                    "bitbucket" to CodeHostSettings.BitBucketSettings(
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
                    "sg" to SearchHostSettings.SourceGraphSettings(
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
        val defaultFileContent = File("src/main/resources/base-files/soft/mega-manipulator.json").readText()

        val fromFile = SerializationHolder.readable.readValue(defaultFileContent, MegaManipulatorSettings::class.java)
        assertEquals(fromFile, testData)
    }

    @Test
    @Disabled("Use to test schema validity, disabled because lib doesn't support minProperties for additionalProperties")
    fun `generate json schema and compare to file`() {
        val baseFile = File("src/main/resources/base-files/hard/mega-manipulator-schema.json")
        val fileSystemSchema: String = baseFile.readText().trim()
        val config: SchemaGeneratorConfig = SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
            .with(JacksonModule())
            .with(JakartaValidationModule(INCLUDE_PATTERN_EXPRESSIONS, NOT_NULLABLE_FIELD_IS_REQUIRED))
            .build()
        val generator = SchemaGenerator(config)
        val jsonSchema: JsonNode = generator.generateSchema(MegaManipulatorSettings::class.java)
        val jsonSchemaString = SerializationHolder.readable.writeValueAsString(jsonSchema)

        // baseFile.writeText(generatedSchema)
        assertThat(fileSystemSchema, equalTo(jsonSchemaString))
    }
}
