package com.github.jensim.megamanipulator.settings.types.searchhost

import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.HostWithAuth
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup

sealed class SearchHostSettings : HostWithAuth {

    abstract val docLinkHref: String
    abstract val authMethod: AuthMethod
    abstract val httpsOverride: HttpsOverride?
    abstract val codeHostSettings: Map<String, CodeHostSettingsGroup>
}
