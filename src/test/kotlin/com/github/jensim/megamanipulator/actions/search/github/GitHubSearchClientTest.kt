package com.github.jensim.megamanipulator.actions.search.github

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.test.wiring.EnvUserSettingsSetup
import com.github.jensim.megamanipulator.test.wiring.TestApplicationWiring
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GitHubSearchClientTest {

    private val wiring = TestApplicationWiring()
    private val client = GitHubSearchClient(mockk(), wiring.httpClientProvider, wiring.notificationsOperator)

    @ValueSource(
        strings = [
            "https://github.com/search?q=repo%3Amega-manipulator/mega-manipulator.github.io+foo&type=code",
            "https://github.com/search?q=repo%3Amega-manipulator%2Fmega-manipulator.github.io+github&type=commits",
            "https://github.com/search?q=org%3Amega-manipulator+mega-manipulator.github.io&type=repositories",
        ]
    )
    @ParameterizedTest
    fun search(search: String) = runBlocking {
        val response: Set<SearchResult> = client.search(EnvUserSettingsSetup.sourcegraphName, EnvUserSettingsSetup.githubSearchSettings, search)

        verify { wiring.notificationsOperator wasNot Called }
        assertThat(response, hasItem(SearchResult("mega-manipulator", "mega-manipulator.github.io", "github.com", EnvUserSettingsSetup.sourcegraphName)))
    }

    @Test
    internal fun `validate access`() = runBlocking {
        val access = client.validateAccess(EnvUserSettingsSetup.sourcegraphName, EnvUserSettingsSetup.githubSearchSettings)

        assertThat(access, equalTo("200:OK"))
    }
}
