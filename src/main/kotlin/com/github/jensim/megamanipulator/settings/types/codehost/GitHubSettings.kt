package com.github.jensim.megamanipulator.settings.types.codehost

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.AuthMethod.JUST_TOKEN
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.CloneType.SSH
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.ForkSetting.LAZY_FORK
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.KeepLocalRepos
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsType.GITHUB
import com.github.jensim.megamanipulator.settings.types.forkSettingDescription

data class GitHubSettings(
    @JsonPropertyDescription("Override the default, strict https validation")
    override val httpsOverride: HttpsOverride? = null,
    @JsonPropertyDescription("Your username at the code host")
    override val username: String,
    @JsonPropertyDescription(forkSettingDescription)
    override val forkSetting: ForkSetting = LAZY_FORK,
    @JsonPropertyDescription("It's strongly recommended to use SSH clone type.")
    override val cloneType: CloneType = SSH,
    override val keepLocalRepos: KeepLocalRepos? = null,
) : CodeHostSettings() {

    override val codeHostType: CodeHostSettingsType = GITHUB
    override val authMethod: AuthMethod = JUST_TOKEN
    override val baseUrl: String = "https://api.github.com"
    val graphQLUrl: String = "https://graphql.github.com/graphql/proxy"

    init {
        validate()
    }

    override fun getAuthHeaderValue(password: String?): String? = when {
        // https://docs.github.com/en/rest/guides/getting-started-with-the-rest-api#authentication
        password != null && authMethod == JUST_TOKEN -> "token $password"
        else -> null
    }
}
