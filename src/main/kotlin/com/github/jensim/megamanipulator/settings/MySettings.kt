package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.settings.AuthMethod.NONE
import com.github.jensim.megamanipulator.settings.CloneType.SSH
import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
enum class CloneType {

    @JsonSchema.Description(["If you have a passphrase in your ssh key the it must be added via ssh-agent and ssh-add prior to clone/fetch/push."])
    SSH,
    @JsonSchema.Description(["It's not recommended to use for your daily work, as the password/token will be stored in the git settings in plain text"])
    HTTPS,
}

@Serializable
enum class HttpsOverride {
    @JsonSchema.Description(["A self signed cert is expected to have only one level"])
    ALLOW_SELF_SIGNED_CERT,
    @JsonSchema.Description(["Do not validate certificate at all"])
    ALLOW_ANYTHING,
}

@Serializable
enum class AuthMethod {
    @JsonSchema.Description(["Username and access token combination"])
    ACCESS_TOKEN,

    @JsonSchema.Description(["Token without username"])
    JUST_TOKEN,
    NONE,
}

@Serializable
enum class ForkSetting {
    @JsonSchema.Description(["Will require write access to the repo"])
    PLAIN_BRANCH,
    @JsonSchema.Description(["When not permitted to push into origin, attempt fork strategy"])
    LAZY_FORK,
    @JsonSchema.Description(["Fork before push, for every repo"])
    EAGER_FORK,
}

@Serializable
data class MegaManipulatorSettings(
    @JsonSchema.IntRange(1, 100)
    @JsonSchema.Description(
        [
            "When applying changes using the scripted method,",
            "number of parallel executing changes"
        ]
    )
    val concurrency: Int = 5,
    @JsonSchema.Description(
        [
            "Override the default strict https validation",
            "May be set less strict on searchHost or codeHost level as well"
        ]
    )
    val defaultHttpsOverride: HttpsOverride? = null,
    @JsonSchema.Description(["Search host definitions"])
    val searchHostSettings: Map<String, SearchHostSettings>,
    @SerialName("\$schema")
    val schema: String = "mega-manipulator-schema.json",
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
        ?.httpsOverride ?: defaultHttpsOverride

    fun resolveHttpsOverride(searchHostName: String, codeHostName: String): HttpsOverride? = searchHostSettings[searchHostName]
        ?.codeHostSettings?.get(codeHostName)?.httpsOverride ?: defaultHttpsOverride

    fun resolveSettings(repoDir: File): Pair<SearchHostSettings, CodeHostSettings>? {
        val codeHostDir: String = repoDir.parentFile.parentFile.name
        val searchHostDir: String = repoDir.parentFile.parentFile.parentFile.name
        return resolveSettings(searchHostDir, codeHostDir)
    }

    fun resolveSettings(searchHostName: String, codeHostName: String): Pair<SearchHostSettings, CodeHostSettings>? {
        return searchHostSettings[searchHostName]?.let { first ->
            searchHostSettings[searchHostName]?.codeHostSettings?.get(codeHostName)?.let { second ->
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

@Serializable
sealed class SearchHostSettings {

    abstract val docLinkHref: String

    abstract val authMethod: AuthMethod
    abstract val username: String
    abstract val baseUrl: String
    abstract val httpsOverride: HttpsOverride?
    abstract val codeHostSettings: Map<String, CodeHostSettings>

    @Serializable
    @SerialName("HOUND")
    data class HoundSettings(
        @JsonSchema.Description(
            [
                "Base url to your SourceGraph installation",
                "For example https://sourcegraph.com",
            ]
        )
        override val baseUrl: String,
        @JsonSchema.Description(["Override the default strict https validation"])
        override val httpsOverride: HttpsOverride? = null,
        @JsonSchema.Description(
            [
                "Code hosts.",
                "The names in this map is used to connect with the hostname.",
                "!!! IT'S THEREFORE REALLY IMPORTANT !!!"
            ]
        )
        override val codeHostSettings: Map<String, CodeHostSettings>,
    ) : SearchHostSettings() {

        override val docLinkHref: String = "https://jensim.github.io/mega-manipulator/docs/Search%20hosts/etsy_hound"
        override val username = "none"
        override val authMethod = NONE
    }

    @Serializable
    @SerialName("SOURCEGRAPH")
    data class SourceGraphSettings(
        @JsonSchema.Description(
            [
                "Base url to your SourceGraph installation",
                "For example https://sourcegraph.com",
            ]
        )
        override val baseUrl: String,
        @JsonSchema.Description(["Override the default strict https validation"])
        override val httpsOverride: HttpsOverride? = null,
        @JsonSchema.Description(
            [
                "Code hosts.",
                "The names in this map is used to connect with the naming used on the search host.",
                "!!! IT'S THEREFORE REALLY IMPORTANT !!!"
            ]
        )
        override val codeHostSettings: Map<String, CodeHostSettings>,
    ) : SearchHostSettings() {

        override val authMethod: AuthMethod = AuthMethod.JUST_TOKEN
        override val username: String = "token"
        override val docLinkHref: String = "https://jensim.github.io/mega-manipulator/docs/Search%20hosts/sourcegraph"

        init {
            require(codeHostSettings.isNotEmpty()) {
                """
                |Please add one or code host settings.
                |Available types are ${CodeHostType.values()} 
                |""".trimMargin()
            }
            validateBaseUrl(baseUrl)
        }
    }
}

enum class CodeHostType {
    BITBUCKET_SERVER,
    GITHUB,
}

@Serializable
sealed class CodeHostSettings
@SuppressWarnings("LongParameterList") constructor() {
    abstract val baseUrl: String
    abstract val httpsOverride: HttpsOverride?
    abstract val authMethod: AuthMethod
    abstract val username: String?
    abstract val forkSetting: ForkSetting
    abstract val cloneType: CloneType

    internal fun validate() {
        validateBaseUrl(baseUrl)
        if (authMethod == AuthMethod.ACCESS_TOKEN) {
            require(!username.isNullOrEmpty()) { "$baseUrl: username is required for auth method USERNAME_PASSWORD" }
        }
        if (forkSetting != ForkSetting.PLAIN_BRANCH) {
            require(username != null) { "username is required if forkSetting is not ${ForkSetting.PLAIN_BRANCH.name}" }
        }
    }

    @Serializable
    @SerialName("BITBUCKET_SERVER")
    data class BitBucketSettings(
        @JsonSchema.Description(["Base url, like https://bitbucket.example.com"])
        override val baseUrl: String,
        @JsonSchema.Description(["Override the default, strict https validation"])
        override val httpsOverride: HttpsOverride? = null,
        @JsonSchema.Description(["Your username at the code host"])
        override val username: String,
        @JsonSchema.Description(
            [
                "Fork settings is used to decide when to fork a repo:",
                "PLAIN_BRANCH: Will require write access to the repo",
                "LAZY_FORK: When not permitted to push into origin, attempt fork strategy",
                "EAGER_FORK: Fork before push, for every repo",
            ]
        )
        override val forkSetting: ForkSetting = ForkSetting.LAZY_FORK,
        @JsonSchema.Description(["It's strongly recommended to use SSH clone type."])
        override val cloneType: CloneType = SSH,
        @JsonSchema.Description(["Prefix forked repos with a recognizable char sequence"])
        val forkRepoPrefix: String = "mm_",
    ) : CodeHostSettings() {

        override val authMethod: AuthMethod = AuthMethod.ACCESS_TOKEN

        init {
            validate()
        }
    }

    @Serializable
    @SerialName("GITHUB")
    data class GitHubSettings(
        @JsonSchema.Description(["Override the default, strict https validation"])
        override val httpsOverride: HttpsOverride? = null,
        @JsonSchema.Description(["Your username at the code host"])
        override val username: String,
        @JsonSchema.Description(
            [
                "Fork settings is used to decide when to fork a repo:",
                "PLAIN_BRANCH: Will require write access to the repo",
                "LAZY_FORK: When not permitted to push into origin, attempt fork strategy",
                "EAGER_FORK: Fork before push, for every repo",
            ]
        )
        override val forkSetting: ForkSetting = ForkSetting.LAZY_FORK,
        @JsonSchema.Description(["It's strongly recommended to use SSH clone type."])
        override val cloneType: CloneType = SSH,
    ) : CodeHostSettings() {

        override val baseUrl: String = "https://api.github.com"
        override val authMethod = AuthMethod.ACCESS_TOKEN

        init {
            validate()
        }
    }
}
