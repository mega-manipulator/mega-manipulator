package com.github.jensim.megamanipulator.test.wiring

import com.github.jensim.megamanipulator.actions.search.SearchResult
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class EnvUserSettingsSetupTest {

    companion object {

        @JvmStatic
        val searchResults: Array<SearchResult> = EnvUserSettingsSetup.searchResults
    }

    private val wiring = TestApplicationWiring()

    @ParameterizedTest
    @MethodSource(value = ["getSearchResults"])
    internal fun verifyPasswords(result: SearchResult) {
        val (searchSettings, codeSettings) = wiring.settings.resolveSettings(result.searchHostName, result.codeHostName)
            ?: fail("Failed to resolve settings for $result")
        wiring.passwordsOperator.getPassword(searchSettings.username, searchSettings.baseUrl)
            ?: fail("Failed to resolve search host password for ${searchSettings.username}@${searchSettings.baseUrl}")
        (codeSettings.username ?: fail("Username not present for code host in test settings ${result.searchHostName}/${result.codeHostName}")).let {
            wiring.passwordsOperator.getPassword(it, codeSettings.baseUrl)
                ?: fail("Failed to resolve search host password for ${it}@${codeSettings.baseUrl}")

        }
    }
}
