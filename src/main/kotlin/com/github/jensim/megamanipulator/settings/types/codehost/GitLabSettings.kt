package com.github.jensim.megamanipulator.settings.types.codehost

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.AuthMethod.JUST_TOKEN
import com.github.jensim.megamanipulator.settings.types.AuthMethod.USERNAME_TOKEN
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.CloneType.SSH
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.ForkSetting.LAZY_FORK
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.KeepLocalRepos
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsType.GITLAB
import com.github.jensim.megamanipulator.settings.types.encodeToBase64String
import com.github.jensim.megamanipulator.settings.types.forkSettingDescription

data class GitLabSettings(
    @JsonPropertyDescription("Override the default, strict https validation")
    override val httpsOverride: HttpsOverride? = null,
    @JsonPropertyDescription("Your username at the code host")
    override val username: String,
    @JsonPropertyDescription(forkSettingDescription)
    override val forkSetting: ForkSetting = LAZY_FORK,
    @JsonPropertyDescription("It's strongly recommended to use SSH clone type.")
    override val cloneType: CloneType = SSH,
    override val baseUrl: String = "https://gitlab.com",
    override val keepLocalRepos: KeepLocalRepos? = null,
) : CodeHostSettings() {

    override val codeHostType: CodeHostSettingsType = GITLAB
    override val authMethod: AuthMethod = JUST_TOKEN

    override fun getAuthHeaderValue(password: String?): String? = when {
        // https://docs.gitlab.com/ee/api/README.html#personalproject-access-tokens
        password != null && authMethod == JUST_TOKEN -> "Bearer $password"
        password != null && authMethod == USERNAME_TOKEN -> "Basic ${encodeToBase64String("$username:$password")}"
        else -> null
    }
}
