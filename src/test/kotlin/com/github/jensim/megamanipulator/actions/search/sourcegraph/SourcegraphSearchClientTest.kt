package com.github.jensim.megamanipulator.actions.search.sourcegraph

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.test.wiring.EnvUserSettingsSetup
import com.github.jensim.megamanipulator.test.wiring.TestApplicationWiring
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SourcegraphSearchClientTest {

    private val wiring = TestApplicationWiring()

    private val sourcegraphSearchClient = SourcegraphSearchClient(
        project = wiring.mockProject,
        httpClientProvider = wiring.httpClientProvider,
        notificationsOperator = wiring.notificationsOperator,
    )

    @Test
    internal fun `search for sourcegraph itself`() {
        val project = "sourcegraph"
        val repo = "sourcegraph"
        val result: Set<SearchResult> = runBlocking {
            sourcegraphSearchClient.search(
                searchHostName = EnvUserSettingsSetup.sourcegraphName,
                settings = EnvUserSettingsSetup.sourceGraphSettings,
                search = "repo:${EnvUserSettingsSetup.githubName}/$project/$repo$ file:.go$ foo"
            )
        }

        assertEquals(
            result,
            setOf(
                SearchResult(
                    searchHostName = EnvUserSettingsSetup.sourcegraphName,
                    codeHostName = EnvUserSettingsSetup.githubName,
                    project = project,
                    repo = repo,
                )
            )
        )
    }

    @Test
    internal fun `validate token test`() {
        val result = runBlocking {
            sourcegraphSearchClient.validateToken(EnvUserSettingsSetup.sourcegraphName, EnvUserSettingsSetup.sourceGraphSettings)
        }

        assertThat(result, equalTo("OK"))
    }
}
