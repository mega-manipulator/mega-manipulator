package com.github.jensim.megamanipulator.test.wiring

import com.github.jensim.megamanipulator.actions.search.SearchResult
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory

class EnvUserSettingsSetupTest {

    private val logger = LoggerFactory.getLogger(javaClass)

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
        wiring.passwordsOperator.getPassword(codeSettings.username, codeSettings.baseUrl)
            ?: fail("Failed to resolve search host password for ${codeSettings.username}@${codeSettings.baseUrl}")
        logger.info("Password verified for ${codeSettings.username}@${codeSettings.baseUrl}")
    }

    @Test
    internal fun `verify github dot com is in the list`() {
        Assertions.assertTrue(
            searchResults.any {
                it.codeHostName == EnvUserSettingsSetup.githubName && it.searchHostName == EnvUserSettingsSetup.githubName
            },
            "${EnvUserSettingsSetup.githubName} has got to be present as a search+code-host combo in this test. Otherwise we cannot verify the password properly in the password test."
        )
    }
}
