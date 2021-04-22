package com.github.jensim.megamanipulator.actions.vcs.githubcom

import com.github.jensim.megamanipulator.TestHelper
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GithubComRepoWrapping
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitHubSettings.Companion.GITHUB_TOKEN
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.PasswordsOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings.SourceGraphSettings
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.jetbrains.rd.util.first
import io.ktor.client.features.ClientRequestException
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GithubComClientTest {

    private val githubSettings = TestHelper.getGithubCredentials()
    private val password = System.getenv(GITHUB_TOKEN) ?: System.getProperty(GITHUB_TOKEN)
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
    fun createPr() {
        // given
        val repo = SearchResult(
            searchHostName = settings.searchHostSettings.first().key,
            codeHostName = "github.com",
            project = githubSettings.username,
            repo = "mega-manipulator",
        )
        whenever(localRepoMock.getBranch(any<SearchResult>())).thenReturn("main")

        // when
        runBlocking {
            val exception = assertThrows<ClientRequestException> {
                client.createPr(
                    title = "new test PR",
                    description = "This is a test PR",
                    settings = githubSettings,
                    repo = repo
                )
            }
            assertThat(exception.localizedMessage, containsString("No commits between main and main"))
        }
    }

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
            repo = "mega-manipulator",
        )

        // when
        val result: GithubComRepoWrapping = runBlocking {
            client.getRepo(repo, githubSettings)
        }

        // then
        assertThat(result.repo.name, equalTo("mega-manipulator"))
    }
}
