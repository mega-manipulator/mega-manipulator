package com.github.jensim.megamanipulator.settings.types.codehost

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.AuthMethod.USERNAME_TOKEN
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.CloneType.SSH
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.ForkSetting.LAZY_FORK
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.KeepLocalRepos
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsType.BITBUCKET_SERVER
import com.github.jensim.megamanipulator.settings.types.encodeToBase64String
import com.github.jensim.megamanipulator.settings.types.forkSettingDescription

data class BitBucketSettings(
    @JsonPropertyDescription("Base url, like https://bitbucket.example.com")
    override val baseUrl: String,
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

    override val codeHostType: CodeHostSettingsType = BITBUCKET_SERVER
    override val authMethod: AuthMethod = USERNAME_TOKEN

    init {
        validate()
    }

    override fun getAuthHeaderValue(password: String?): String? = when {
        // https://developer.atlassian.com/server/bitbucket/how-tos/example-basic-authentication/
        password != null && authMethod == USERNAME_TOKEN ->
            "Basic ${encodeToBase64String("$username:$password")}"
        else -> null
    }
}
