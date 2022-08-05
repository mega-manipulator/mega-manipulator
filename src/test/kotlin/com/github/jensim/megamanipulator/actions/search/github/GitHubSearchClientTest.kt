package com.github.jensim.megamanipulator.actions.search.github

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.types.searchhost.GithubSearchSettings
import com.github.jensim.megamanipulator.test.wiring.EnvUserSettingsSetup
import com.github.jensim.megamanipulator.test.wiring.TestApplicationWiring
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
class GitHubSearchClientTest {

    private val wiring = TestApplicationWiring()
    private val clientProvider: HttpClientProvider = wiring.httpClientProvider
    private val notificationsOperatorMockk: NotificationsOperator = mockk {
        every { show(any(), any(), any()) } returns Unit
    }
    private val client = GitHubSearchClient(mockk(), clientProvider, notificationsOperatorMockk)

    @ValueSource(
        strings = [
            "https://github.com/search?q=repo%3Amega-manipulator/mega-manipulator.github.io+foo&type=code",
            "https://github.com/search?q=repo%3Amega-manipulator%2Fmega-manipulator.github.io+github&type=commits",
            "https://github.com/search?q=org%3Amega-manipulator+mega-manipulator.github.io&type=repositories",
        ]
    )
    @ParameterizedTest
    fun search(search: String) = runBlocking {
        val response: Set<SearchResult> = client.search(EnvUserSettingsSetup.sourcegraphName, GithubSearchSettings("jensim"), search)

        verify { notificationsOperatorMockk wasNot Called }
        assertThat(response, hasItem(SearchResult("mega-manipulator", "mega-manipulator.github.io", "github.com", EnvUserSettingsSetup.sourcegraphName)))
    }

    @Test
    internal fun `validate access`() = runBlocking {
        val access = client.validateAccess(EnvUserSettingsSetup.sourcegraphName, GithubSearchSettings("jensim"))

        assertThat(access, equalTo("200:OK"))
    }
}
