package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
                        authMethod = AuthMethod.USERNAME_PASSWORD,
                        username = "null",
                    ),
                    codeHostSettings = mapOf(
                        "bb" to CodeHostSettingsWrapper(
                            type = CodeHostType.BITBUCKET_SERVER,
                            BitBucketSettings(
                                    baseUrl = "https://bitbucket.example.com",
                                    httpsOverride = null,
                                    authMethod = AuthMethod.TOKEN,
                                    username = null,
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
                            authMethod = AuthMethod.USERNAME_PASSWORD,
                            username = "null"
                        ),
                        codeHostSettings = mapOf()
                    ),
                ),
            )
        }
    }

    @Test
    internal fun `test default settings`() {
        val defaultFileContent = File("src/main/resources/base-files/mega-manipulator.yml").readText()
        SerializationHolder.yamlObjectMapper.readValue<MegaManipulatorSettings>(defaultFileContent)
    }
}
