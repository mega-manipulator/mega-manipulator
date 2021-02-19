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

private fun validateBaseUrl(baseUrl: String) {
    require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
        "baseUrl must start with http:// or https://"
    }
    listOf<Char>('/', '?', '=', '&').forEach {
        require(!baseUrl.endsWith(it)) {
            "baseUrl must not end in '$it'"
        }
    }
}

data class SourceGraphSettings(val baseUrl: String) {
    init {
        validateBaseUrl(baseUrl)
    }
}

enum class CodeHostType {
    BITBUCKET_SERVER,
    GIT_LAB,
    GITHUB,
}

sealed class CodeHostSettings(
    open val baseUrl: String,
    open val sourceGraphName: String,
    open val clonePattern: String
) {
    internal fun validate() {
        validateBaseUrl(baseUrl)
        for (word in listOf("project", "repo")) {
            require(clonePattern.contains("{$word}")) {
                "clonePattern must contain {$word}, try something like ssh://git@bitbucket.example.com/{project}/{repo}.git"
            }
        }
    }

    fun cloneUrl(project: String, repo: String) = clonePattern
        .replace("{project}", project)
        .replace("{repo}", repo)
}

data class BitBucketSettings(
    override val baseUrl: String,
    override val sourceGraphName: String,
    override val clonePattern: String,
) : CodeHostSettings(
    baseUrl,
    sourceGraphName,
    clonePattern,
) {
    init {
        validate()
    }
}

data class GitLabSettings(
    override val baseUrl: String,
    override val sourceGraphName: String,
    override val clonePattern: String,
) : CodeHostSettings(
    baseUrl,
    sourceGraphName,
    clonePattern,
) {
    init {
        validate()
    }
}

data class GitHubSettings(
    override val baseUrl: String,
    override val sourceGraphName: String,
    override val clonePattern: String,
) : CodeHostSettings(
    baseUrl,
    sourceGraphName,
    clonePattern,
) {
    init {
        validate()
    }
}

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
