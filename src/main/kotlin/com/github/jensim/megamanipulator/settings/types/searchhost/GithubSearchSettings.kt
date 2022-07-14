package com.github.jensim.megamanipulator.settings.types.searchhost

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.github.jensim.megamanipulator.settings.types.AuthMethod.JUST_TOKEN
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.CloneType.SSH
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.ForkSetting.LAZY_FORK
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.codehost.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.forkSettingDescription

data class GithubSearchSettings(
    override val username: String,
    @JsonPropertyDescription(forkSettingDescription)
    val forkSetting: ForkSetting = LAZY_FORK,
    @JsonPropertyDescription("It's strongly recommended to use SSH clone type.")
    val cloneType: CloneType = SSH,
) : SearchHostSettings() {
    override val docLinkHref: String = "https://mega-manipulator.github.io"
    override val authMethod = JUST_TOKEN
    override val baseUrl: String = "https://api.github.com"
    override val httpsOverride = null
    override val codeHostSettings: Map<String, CodeHostSettingsGroup> = mapOf(
        "github.com" to CodeHostSettingsGroup(
            gitHub = GitHubSettings(
                username = username,
                forkSetting = forkSetting,
                cloneType = cloneType,
            )
        )
    )

    override fun getAuthHeaderValue(password: String?): String? = when {
        // https://docs.github.com/en/rest/guides/getting-started-with-the-rest-api#authentication
        password != null && authMethod == JUST_TOKEN -> "token $password"
        else -> null
    }
}