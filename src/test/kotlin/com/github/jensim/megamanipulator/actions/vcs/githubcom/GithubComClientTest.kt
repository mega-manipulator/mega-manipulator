package com.github.jensim.megamanipulator.actions.vcs.githubcom

import com.github.jensim.megamanipulator.TestHelper
import com.github.jensim.megamanipulator.TestHelper.GITHUB_TOKEN
import com.github.jensim.megamanipulator.TestHelper.MEGA_MANIPULATOR_REPO
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GithubComRepoWrapping
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.PasswordsOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings.SourceGraphSettings
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.jetbrains.rd.util.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class GithubComClientTest {

    private val githubSettings = TestHelper.githubCredentials
    private val password = System.getProperty(GITHUB_TOKEN) ?: System.getenv(GITHUB_TOKEN)
    private val settings = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            "sourcegraph.com" to SourceGraphSettings(
                baseUrl = "https://sourcegraph.com",
                codeHostSettings = mapOf("github.com" to githubSettings)
            )
        )
    )
    private val settingsMock: SettingsFileOperator = mock {
        on { readSettings() } doReturn settings
    }
    private val passwordMock: PasswordsOperator = mock {
        on { getPassword(githubSettings.username, githubSettings.baseUrl) } doReturn password
        on { isPasswordSet(githubSettings.username, githubSettings.baseUrl) } doReturn true
    }
    private val notificationsMock: NotificationsOperator = mock()
    private val clientProvider = HttpClientProvider(
        settingsFileOperator = settingsMock,
        passwordsOperator = passwordMock,
        notificationsOperator = notificationsMock
    )
    private val localRepoMock: LocalRepoOperator = mock()
    private val client = GithubComClient(
        httpClientProvider = clientProvider,
        localRepoOperator = localRepoMock,
        json = SerializationHolder.instance.readableJson
    )

    @Test
    fun getAllPrs() {
        // given

        // when
        val prs = runBlocking {
            client.getAllPrs(
                searchHost = settings.searchHostSettings.first().key,
                codeHost = "github.com",
                settings = githubSettings,
            )
        }

        // then
        assertThat(prs, not(empty()))
        // No duplicates
        val prIds = prs.map { it.pullRequest.id }
        val uniquePrIds = prIds.toSet()
        assertEquals(prIds.size, uniquePrIds.size)
    }

    @Test
    fun getRepo() {
        // given
        val repo = SearchResult(
            searchHostName = settings.searchHostSettings.first().key,
            codeHostName = "github.com",
            project = githubSettings.username,
            repo = MEGA_MANIPULATOR_REPO,
        )

        // when
        val result: GithubComRepoWrapping = runBlocking {
            client.getRepo(repo, githubSettings)
        }

        // then
        assertThat(result.repo.name, equalTo(MEGA_MANIPULATOR_REPO))
    }
}
