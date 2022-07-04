package com.github.jensim.megamanipulator.settings.types

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.jensim.megamanipulator.graphql.generated.gitlab.enums.MergeRequestState
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.BitBucketSettings
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitLabSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings.HoundSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings.SourceGraphSettings
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.io.File
import java.util.Base64

private val base64encoder = Base64.getEncoder()

data class MegaManipulatorSettings(
    @Min(1)
    @Max(100)
    @JsonPropertyDescription(
        """
When applying changes using the scripted method,
number of parallel executing changes
"""
    )
    val concurrency: Int = 5,
    @JsonPropertyDescription(
        """
Override the default strict https validation
May be set less strict on searchHost or codeHost level as well
"""
    )
    val defaultHttpsOverride: HttpsOverride? = null,
    @JsonPropertyDescription("Search host definitions")
    val searchHostSettings: Map<String, SearchHostSettings>,
    @JsonProperty(value = "\$schema")
    val schema: String = "mega-manipulator-schema.json",
) {
    init {
        require(searchHostSettings.isNotEmpty()) {
            """
            |Please add one or search host settings.
            |Available types are [HOUND, SOURCEGRAPH] 
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

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    value = [
        Type(value = HoundSettings::class, name = "HOUND"),
        Type(value = SourceGraphSettings::class, name = "SOURCEGRAPH"),
    ]
)
sealed class SearchHostSettings : HostWithAuth {

    abstract val docLinkHref: String

    abstract val authMethod: AuthMethod
    abstract val username: String
    abstract val baseUrl: String
    abstract val httpsOverride: HttpsOverride?
    abstract val codeHostSettings: Map<String, CodeHostSettings>

    data class HoundSettings(
        @JsonPropertyDescription(
            """
Base url to your SourceGraph installation
For example https://sourcegraph.com
"""
        )
        override val baseUrl: String,
        @JsonPropertyDescription("Override the default strict https validation")
        override val httpsOverride: HttpsOverride? = null,
        @JsonPropertyDescription(
            """
Code hosts.
The names in this map is used to connect with the hostname.
!!! IT'S THEREFORE REALLY IMPORTANT !!!"""
        )
        override val codeHostSettings: Map<String, CodeHostSettings>,
    ) : SearchHostSettings() {

        override val docLinkHref: String = "https://mega-manipulator.github.io/docs/Search%20hosts/etsy_hound"
        override val username = "none"
        override val authMethod = AuthMethod.NONE
        override fun getAuthHeaderValue(password: String?): String? = null
    }

    data class SourceGraphSettings(
        @JsonPropertyDescription("""
Base url to your SourceGraph installation
For example https://sourcegraph.com
""")
        override val baseUrl: String,
        @JsonPropertyDescription("Override the default strict https validation")
        override val httpsOverride: HttpsOverride? = null,
        @JsonPropertyDescription("""
Code hosts.
The names in this map is used to connect with the naming used on the search host.
!!! IT'S THEREFORE REALLY IMPORTANT !!!
""")
        override val codeHostSettings: Map<String, CodeHostSettings>,
        override val authMethod: AuthMethod = AuthMethod.JUST_TOKEN,
    ) : SearchHostSettings() {

        override val username: String = "token"
        override val docLinkHref: String = "https://mega-manipulator.github.io/docs/Search%20hosts/sourcegraph"

        init {
            require(codeHostSettings.isNotEmpty()) {
                "Please add one or code host settings."
            }
            validateBaseUrl(baseUrl)
        }

        override fun getAuthHeaderValue(password: String?): String? = when {
            // https://docs.sourcegraph.com/api/graphql
            password != null && authMethod == AuthMethod.JUST_TOKEN -> "token $password"
            password != null && authMethod == AuthMethod.USERNAME_TOKEN -> "Basic ${base64encoder.encodeToString("$username:$password".toByteArray())}"
            else -> null
        }
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    value = [
        Type(value = BitBucketSettings::class, name = "BITBUCKET_SERVER"),
        Type(value = GitHubSettings::class, name = "GITHUB"),
        Type(value = GitLabSettings::class, name = "GITLAB"),

    ]
)
sealed class CodeHostSettings
@SuppressWarnings("LongParameterList") constructor() : HostWithAuth {

    abstract val codeHostType: CodeHostSettingsType
    abstract val baseUrl: String
    abstract val httpsOverride: HttpsOverride?
    abstract val authMethod: AuthMethod
    abstract val username: String?
    abstract val forkSetting: ForkSetting
    abstract val cloneType: CloneType

    internal fun validate() {
        validateBaseUrl(baseUrl)
        if (authMethod == AuthMethod.USERNAME_TOKEN) {
            require(!username.isNullOrEmpty()) { "$baseUrl: username is required for auth method USERNAME_PASSWORD" }
        }
        if (forkSetting != ForkSetting.PLAIN_BRANCH) {
            require(username != null) { "username is required if forkSetting is not ${ForkSetting.PLAIN_BRANCH.name}" }
        }
    }

    enum class CodeHostSettingsType(
        val prRoleAuthor: String,
        val prRoleAssignee: String,
        val prRoles: Set<String?>,
        val prStateOpen: String,
        val prStates: Set<String?>,
    ) {
        BITBUCKET_SERVER(
            prRoleAuthor = "AUTHOR",
            prRoleAssignee = "REVIEWER",
            prRoles = setOf("AUTHOR", "REVIEWER", "PARTICIPANT", null),
            prStateOpen = "OPEN",
            prStates = setOf("OPEN", "DECLINED", "MERGED", null),
        ),
        GITHUB(
            prRoleAuthor = "author",
            prRoleAssignee = "assignee",
            prRoles = setOf("assignee", "author", "commenter", null),
            prStateOpen = "open",
            prStates = setOf("open", "closed", null),
        ),
        GITLAB(
            prRoleAuthor = "author",
            prRoleAssignee = "assignee",
            prRoles = setOf("assignee", "author"),
            prStateOpen = MergeRequestState.OPENED.name,
            prStates = (MergeRequestState.values().toSet() - MergeRequestState.__UNKNOWN_VALUE).map { it.name }.toSet(),
        );
    }

    data class BitBucketSettings(
        @JsonPropertyDescription("Base url, like https://bitbucket.example.com")
        override val baseUrl: String,
        @JsonPropertyDescription("Override the default, strict https validation")
        override val httpsOverride: HttpsOverride? = null,
        @JsonPropertyDescription("Your username at the code host")
        override val username: String,
        @JsonPropertyDescription(forkSettingDescription)
        override val forkSetting: ForkSetting = ForkSetting.LAZY_FORK,
        @JsonPropertyDescription("It's strongly recommended to use SSH clone type.")
        override val cloneType: CloneType = CloneType.SSH,
    ) : CodeHostSettings() {

        override val codeHostType: CodeHostSettingsType = CodeHostSettingsType.BITBUCKET_SERVER
        override val authMethod: AuthMethod = AuthMethod.USERNAME_TOKEN

        init {
            validate()
        }

        override fun getAuthHeaderValue(password: String?): String? = when {
            // https://developer.atlassian.com/server/bitbucket/how-tos/example-basic-authentication/
            password != null && authMethod == AuthMethod.USERNAME_TOKEN ->
                "Basic ${base64encoder.encodeToString("$username:$password".toByteArray())}"
            else -> null
        }
    }

    data class GitHubSettings(
        @JsonPropertyDescription("Override the default, strict https validation")
        override val httpsOverride: HttpsOverride? = null,
        @JsonPropertyDescription("Your username at the code host")
        override val username: String,
        @JsonPropertyDescription(forkSettingDescription)
        override val forkSetting: ForkSetting = ForkSetting.LAZY_FORK,
        @JsonPropertyDescription("It's strongly recommended to use SSH clone type.")
        override val cloneType: CloneType = CloneType.SSH,
    ) : CodeHostSettings() {

        override val codeHostType: CodeHostSettingsType = CodeHostSettingsType.GITHUB
        override val authMethod: AuthMethod = AuthMethod.USERNAME_TOKEN
        override val baseUrl: String = "https://api.github.com"
        val graphQLUrl: String = "https://graphql.github.com/graphql/proxy"

        init {
            validate()
        }

        override fun getAuthHeaderValue(password: String?): String? = when {
            // https://docs.github.com/en/rest/guides/getting-started-with-the-rest-api#authentication
            password != null && authMethod == AuthMethod.USERNAME_TOKEN -> "Basic ${base64encoder.encodeToString("$username:$password".toByteArray())}"
            else -> null
        }
    }

    data class GitLabSettings(
        @JsonPropertyDescription("Override the default, strict https validation")
        override val httpsOverride: HttpsOverride? = null,
        @JsonPropertyDescription("Your username at the code host")
        override val username: String,
        @JsonPropertyDescription(forkSettingDescription)
        override val forkSetting: ForkSetting = ForkSetting.LAZY_FORK,
        @JsonPropertyDescription("It's strongly recommended to use SSH clone type.")
        override val cloneType: CloneType = CloneType.SSH,
        override val baseUrl: String = "https://gitlab.com",
    ) : CodeHostSettings() {

        override val codeHostType: CodeHostSettingsType = CodeHostSettingsType.GITLAB
        override val authMethod: AuthMethod = AuthMethod.JUST_TOKEN

        override fun getAuthHeaderValue(password: String?): String? = when {
            // https://docs.gitlab.com/ee/api/README.html#personalproject-access-tokens
            password != null && authMethod == AuthMethod.JUST_TOKEN -> "Bearer $password"
            password != null && authMethod == AuthMethod.USERNAME_TOKEN -> "Basic ${base64encoder.encodeToString("$username:$password".toByteArray())}"
            else -> null
        }
    }
}
