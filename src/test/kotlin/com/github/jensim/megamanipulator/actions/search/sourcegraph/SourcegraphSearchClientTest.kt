package com.github.jensim.megamanipulator.actions.search.sourcegraph

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.ForkSetting.PLAIN_BRANCH
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.SearchHostSettings.SourceGraphSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SourcegraphSearchClientTest {

    private val password = System.getenv("SRC_COM_ACCESS_TOKEN")
    private val codeHostName = "github.com"
    private val sourceGraphSettings = SourceGraphSettings(
        baseUrl = "https://sourcegraph.com",
        codeHostSettings = mapOf(
            codeHostName to GitHubSettings(
                username = "jensim",
                forkSetting = PLAIN_BRANCH,
            )
        )
    )
    private val searchHostName = "sourcegraph.com"
    private val settings = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            searchHostName to sourceGraphSettings
        )
    )
    private val settingsMock: SettingsFileOperator = mockk {
        every { readSettings() } returns settings
    }
    private val passwordsOperator = TestPasswordOperator(mapOf("token" to "https://sourcegraph.com" to password))
    private val notificationsMock: NotificationsOperator = mockk()
    private val clientProvider = HttpClientProvider(
        settingsFileOperator = settingsMock,
        passwordsOperator = passwordsOperator,
        notificationsOperator = notificationsMock
    )
    private val sourcegraphSearchClient = SourcegraphSearchClient(
        httpClientProvider = clientProvider,
        notificationsOperator = notificationsMock
    )

    @Test
    internal fun `search for sourcegraph itself`() {
        val project = "sourcegraph"
        val repo = "sourcegraph"
        val result: Set<SearchResult> = runBlocking {
            sourcegraphSearchClient.search(searchHostName, sourceGraphSettings, "repo:$codeHostName/$project/$repo$ file:.go$ foo")
        }

        assertEquals(
            result,
            setOf(
                SearchResult(
                    searchHostName = searchHostName,
                    codeHostName = codeHostName,
                    project = project,
                    repo = repo,
                )
            )
        )
    }
}
