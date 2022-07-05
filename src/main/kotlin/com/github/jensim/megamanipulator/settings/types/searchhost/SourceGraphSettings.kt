package com.github.jensim.megamanipulator.settings.types.searchhost

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.AuthMethod.JUST_TOKEN
import com.github.jensim.megamanipulator.settings.types.AuthMethod.USERNAME_TOKEN
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.encodeToBase64String
import com.github.jensim.megamanipulator.settings.types.validateBaseUrl

data class SourceGraphSettings(
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
The names in this map is used to connect with the naming used on the search host.
!!! IT'S THEREFORE REALLY IMPORTANT !!!
"""
    )
    override val codeHostSettings: Map<String, CodeHostSettingsGroup>,
    override val authMethod: AuthMethod = JUST_TOKEN,
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
        password != null && authMethod == JUST_TOKEN -> "token $password"
        password != null && authMethod == USERNAME_TOKEN -> "Basic ${encodeToBase64String("$username:$password")}"
        else -> null
    }
}
