package com.github.jensim.megamanipulatior.settings

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class MegaManipulatorSettings(
    val sourceGraphSettings: SourceGraphSettings,
    val codeHostSettings: List<CodeHostSettingsWrapper>
) {
    init {

        require(codeHostSettings.isNotEmpty()) {
            """
            |Please add one or code host settings.
            |Available types are ${CodeHostType.values()} 
            |""".trimMargin()
        }
    }
}

data class SourceGraphSettings(val baseUrl: String)

enum class CodeHostType {
    BITBUCKET_SERVER
}

sealed class CodeHostSettings(open val baseUrl: String)
data class BitBucketSettings(override val baseUrl: String) : CodeHostSettings(baseUrl)
data class CodeHostSettingsWrapper(
    val type: CodeHostType,
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @JsonSubTypes(
        value = [
            JsonSubTypes.Type(value = BitBucketSettings::class, name = "BITBUCKET_SERVER")
        ]
    )
    val settings: CodeHostSettings
)
