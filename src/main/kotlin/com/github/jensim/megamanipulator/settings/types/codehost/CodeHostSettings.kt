package com.github.jensim.megamanipulator.settings.types.codehost

import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.AuthMethod.USERNAME_TOKEN
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.ForkSetting.PLAIN_BRANCH
import com.github.jensim.megamanipulator.settings.types.HostWithAuth
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.KeepLocalRepos
import com.github.jensim.megamanipulator.settings.types.validateBaseUrl

sealed class CodeHostSettings
@SuppressWarnings("LongParameterList") constructor() : HostWithAuth {

    abstract val codeHostType: CodeHostSettingsType
    abstract val baseUrl: String
    abstract val httpsOverride: HttpsOverride?
    abstract val authMethod: AuthMethod
    abstract val username: String?
    abstract val forkSetting: ForkSetting
    abstract val cloneType: CloneType
    abstract val keepLocalRepos: KeepLocalRepos?
    open val cloneSleepSeconds: Int = 0

    internal fun validate() {
        validateBaseUrl(baseUrl)
        if (authMethod == USERNAME_TOKEN) {
            require(!username.isNullOrEmpty()) { "$baseUrl: username is required for auth method USERNAME_PASSWORD" }
        }
        if (forkSetting != PLAIN_BRANCH) {
            require(username != null) { "username is required if forkSetting is not ${PLAIN_BRANCH.name}" }
        }
    }
}
