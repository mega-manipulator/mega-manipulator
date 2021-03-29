package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.File

enum class HttpsOverride {
    ALLOW_SELF_SIGNED_CERT,
    ALLOW_ANYTHING,
}

enum class AuthMethod {
    ACCESS_TOKEN,
}

enum class ForkSetting {
    /**
     * Will require write access to the repo
     */
    PLAIN_BRANCH,

    /**
     * When not permitted to push into origin, attempt fork strategy
     */
    LAZY_FORK,

    /**
     * Fork before push, for every repo
     */
    EAGER_FORK,
}

data class MegaManipulatorSettings(
    val concurrency: Int = 5,
    val defaultHttpsOverride: HttpsOverride?,
    val searchHostSettings: Map<String, SearchHostSettingsWrapper>,
) {
    init {
        require(searchHostSettings.isNotEmpty()) {
            """
            |Please add one or search host settings.
            |Available types are ${SearchHostType.values()} 
            |""".trimMargin()
        }
    }

    fun resolveHttpsOverride(searchHostName: String): HttpsOverride? = searchHostSettings[searchHostName]
        ?.settings?.httpsOverride ?: defaultHttpsOverride

    fun resolveHttpsOverride(searchHostName: String, codeHostName: String): HttpsOverride? = searchHostSettings[searchHostName]
        ?.codeHostSettings?.get(codeHostName)?.settings?.httpsOverride ?: defaultHttpsOverride

    // fun resolveSettings(searchHostName: String): SearchHostSettings = TODO()

    fun resolveSettings(repoDir: File): Pair<SearchHostSettings, CodeHostSettings>? {
        val codeHostDir: String = repoDir.parentFile.parentFile.name
        val searchHostDir: String = repoDir.parentFile.parentFile.parentFile.name
        return resolveSettings(searchHostDir, codeHostDir)
    }

    fun resolveSettings(searchHostName: String, codeHostName: String): Pair<SearchHostSettings, CodeHostSettings>? {
        return searchHostSettings[searchHostName]?.settings?.let { first ->
            searchHostSettings[searchHostName]?.codeHostSettings?.get(codeHostName)?.settings?.let { second ->
                Pair(first, second)
            }
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

enum class SearchHostType {
    SOURCEGRAPH
}

data class SearchHostSettingsWrapper(
    val type: SearchHostType,
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @JsonSubTypes(
        value = [
            JsonSubTypes.Type(value = SourceGraphSettings::class, name = "SOURCEGRAPH"),
        ]
    )
    val settings: SearchHostSettings,
    val codeHostSettings: Map<String, CodeHostSettingsWrapper>,

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

sealed class SearchHostSettings(
    open val baseUrl: String,
    open val httpsOverride: HttpsOverride?,
    open val authMethod: AuthMethod,
    open val username: String,
) {
    fun validate() {
        validateBaseUrl(baseUrl)
    }
}

data class SourceGraphSettings(
    override val baseUrl: String,
    override val httpsOverride: HttpsOverride?,
    override val authMethod: AuthMethod,
    override val username: String,
) : SearchHostSettings(
    baseUrl = baseUrl,
    httpsOverride = httpsOverride,
    authMethod = authMethod,
    username = username
) {
    init {
        validate()
    }
}

enum class CodeHostType {
    BITBUCKET_SERVER,
    GITHUB,
}

sealed class CodeHostSettings
@SuppressWarnings("LongParameterList") constructor(
    open val baseUrl: String,
    open val httpsOverride: HttpsOverride?,
    open val authMethod: AuthMethod,
    open val username: String?,
    open val forkSetting: ForkSetting,
    open val forkRepoPrefix: String?,
) {
    internal fun validate() {
        validateBaseUrl(baseUrl)
        if (authMethod == AuthMethod.ACCESS_TOKEN) {
            require(!username.isNullOrEmpty()) { "$baseUrl: username is required for auth method USERNAME_PASSWORD" }
        }
        if (forkSetting != ForkSetting.PLAIN_BRANCH) {
            require(username != null) { "username is required if forkSetting is not ${ForkSetting.PLAIN_BRANCH.name}" }
            require(forkRepoPrefix != null) { "forkRepoPrefix is required if forkSetting is not ${ForkSetting.PLAIN_BRANCH.name}" }
        }
    }
}

data class BitBucketSettings(
    override val baseUrl: String,
    override val httpsOverride: HttpsOverride?,
    override val authMethod: AuthMethod = AuthMethod.ACCESS_TOKEN,
    override val username: String?,
    override val forkSetting: ForkSetting = ForkSetting.LAZY_FORK,
    override val forkRepoPrefix: String = "mm_",
) : CodeHostSettings(
    baseUrl = baseUrl,
    httpsOverride = httpsOverride,
    authMethod = authMethod,
    username = username,
    forkSetting = forkSetting,
    forkRepoPrefix = forkRepoPrefix
) {
    init {
        validate()
    }
}

data class GitHubSettings(
    override val httpsOverride: HttpsOverride? = null,
    override val username: String,
    override val forkSetting: ForkSetting = ForkSetting.LAZY_FORK,
    override val forkRepoPrefix: String = "mm_",
) : CodeHostSettings(
    baseUrl = "https://api.github.com",
    httpsOverride = httpsOverride,
    authMethod = AuthMethod.ACCESS_TOKEN,
    username = username,
    forkSetting = forkSetting,
    forkRepoPrefix = forkRepoPrefix,
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
            JsonSubTypes.Type(value = GitHubSettings::class, name = "GITHUB"),
        ]
    )
    val settings: CodeHostSettings
)
