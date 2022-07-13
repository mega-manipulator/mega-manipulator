package com.github.jensim.megamanipulator.settings.types.searchhost

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.github.jensim.megamanipulator.settings.types.AuthMethod.NONE
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup

data class HoundSettings(
    @JsonPropertyDescription(
        """Base url to your SourceGraph installation
For example https://sourcegraph.com"""
    )
    override val baseUrl: String,
    @JsonPropertyDescription("Override the default strict https validation")
    override val httpsOverride: HttpsOverride? = null,
    @JsonPropertyDescription(
        """Code hosts.
The names in this map is used to connect with the hostname.
!!! IT'S THEREFORE REALLY IMPORTANT !!!"""
    )
    override val codeHostSettings: Map<String, CodeHostSettingsGroup>,
) : SearchHostSettings() {
    @JsonIgnore
    override val docLinkHref: String = "https://mega-manipulator.github.io/docs/Search%20hosts/etsy_hound"
    override val username = "none"
    override val authMethod = NONE
    override fun getAuthHeaderValue(password: String?): String? = null
}
