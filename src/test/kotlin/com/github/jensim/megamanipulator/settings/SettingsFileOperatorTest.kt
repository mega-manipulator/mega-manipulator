package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class SettingsFileOperatorTest {

    @Test
    fun `serialize deserialize`() {
        // given
        val testData = MegaManipulatorSettings(
            defaultHttpsOverride = null,
            searchHostSettings = mapOf(
                "sg" to SearchHostSettingsWrapper(
                    type = SearchHostType.SOURCEGRAPH,
                    settings = SourceGraphSettings(
                        baseUrl = "https://sourcegraph.example.com",
                        httpsOverride = null,
                    ),
                    codeHostSettings = mapOf(
                        "bb" to CodeHostSettingsWrapper(
                            type = CodeHostType.BITBUCKET_SERVER,
                            BitBucketSettings(
                                baseUrl = "https://bitbucket.example.com",
                                httpsOverride = null,
                                authMethod = AuthMethod.ACCESS_TOKEN,
                                username = "null",
                                forkSetting = ForkSetting.PLAIN_BRANCH,
                                forkRepoPrefix = "null_",
                            )
                        )
                    )
                ),
            ),
        )

        // when
        val yaml = SerializationHolder.yamlObjectMapper.writeValueAsString(testData)
        val deserialized: MegaManipulatorSettings = SerializationHolder.yamlObjectMapper.readValue(yaml)

        // then
        assertEquals(deserialized, testData)
        println(yaml)
    }

    @Test
    fun `fail if too few entries`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            MegaManipulatorSettings(
                defaultHttpsOverride = null,
                searchHostSettings = mapOf(
                    "sg" to SearchHostSettingsWrapper(
                        type = SearchHostType.SOURCEGRAPH,
                        settings = SourceGraphSettings(
                            baseUrl = "https://sourcegraph.example.com",
                            httpsOverride = null,
                        ),
                        codeHostSettings = mapOf()
                    ),
                ),
            )
        }
    }

    @Test
    fun `test default settings`() {
        val defaultFileContent = File("src/main/resources/base-files/mega-manipulator.yml").readText()
        SerializationHolder.yamlObjectMapper.readValue<MegaManipulatorSettings>(defaultFileContent)
    }

    @Test
    fun `generate json schema and compare to file`() {
        val root = SerializationHolder.jsonObjectMapper.generateJsonSchema(MegaManipulatorSettings::class.java)

        val searchHost: JsonNode = SerializationHolder.jsonObjectMapper.generateJsonSchema(SearchHostSettingsWrapper::class.java).schemaNode
        val node: JsonNode = root.schemaNode["properties"]["searchHostSettings"]
        if (node is ObjectNode) node.put("additionalProperties", searchHost)

        val codeHost: JsonNode = SerializationHolder.jsonObjectMapper.generateJsonSchema(CodeHostSettingsWrapper::class.java).schemaNode
        val node2: JsonNode = root.schemaNode["properties"]["searchHostSettings"]["additionalProperties"]["properties"]["codeHostSettings"]
        if (node2 is ObjectNode) node2.put("additionalProperties", codeHost)

        val fileContent = File("src/main/resources/base-files/mega-manipulator-schema.json").readText().trim()
        val generatedSchema = SerializationHolder.jsonObjectMapper.writeValueAsString(root)

        assertThat(generatedSchema, equalTo(fileContent))
    }
}
