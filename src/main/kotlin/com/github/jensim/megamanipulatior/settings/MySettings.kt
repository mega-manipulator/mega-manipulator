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
        val names = codeHostSettings.map { it.settings.sourceGraphName }
        require(names.size == names.distinct().size) {
            "sourceGraphName have to be unique"
        }
    }
}

data class SourceGraphSettings(val baseUrl: String)

enum class CodeHostType {
    BITBUCKET_SERVER,
    GIT_LAB,
    GITHUB,
}

sealed class CodeHostSettings(open val baseUrl: String, open val sourceGraphName: String)
data class BitBucketSettings(override val baseUrl: String, override val sourceGraphName: String) : CodeHostSettings(baseUrl, sourceGraphName)
data class GitLabSettings(override val baseUrl: String, override val sourceGraphName: String) : CodeHostSettings(baseUrl, sourceGraphName)
data class GitHubSettings(override val baseUrl: String, override val sourceGraphName: String) : CodeHostSettings(baseUrl, sourceGraphName)
data class CodeHostSettingsWrapper(
    val type: CodeHostType,
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @JsonSubTypes(
        value = [
            JsonSubTypes.Type(value = BitBucketSettings::class, name = "BITBUCKET_SERVER"),
            JsonSubTypes.Type(value = GitLabSettings::class, name = "GIT_LAB"),
            JsonSubTypes.Type(value = GitHubSettings::class, name = "GITHUB"),
        ]
    )
    val settings: CodeHostSettings
)
