package com.github.jensim.megamanipulator.actions.vcs.githubcom

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GithubComRepoWrapping
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.types.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.ForkSetting.PLAIN_BRANCH
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings.SourceGraphSettings
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITHUB_USERNAME
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GithubComClientTest {

    private val envHelper = EnvHelper()
    private val githubSettings = GitHubSettings(
        username = envHelper.resolve(GITHUB_USERNAME),
        forkSetting = PLAIN_BRANCH,
        cloneType = HTTPS,
    )
    private val password = envHelper.resolve(GITHUB_USERNAME)
    private val codeHost = "github.com"
    private val searchHost = "sourcegraph.com"
    private val settings = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            searchHost to SourceGraphSettings(
                baseUrl = "https://sourcegraph.com",
                codeHostSettings = mapOf(codeHost to githubSettings)
            )
        )
    )
    private val settingsMock: SettingsFileOperator = mockk {
        every { readSettings() } returns settings
    }
    private val passwordsOperator = TestPasswordOperator(mapOf(githubSettings.username to githubSettings.baseUrl to password))
    private val notificationsMock: NotificationsOperator = mockk()
    private val clientProvider = HttpClientProvider(
        settingsFileOperator = settingsMock,
        passwordsOperator = passwordsOperator,
        notificationsOperator = notificationsMock
    )
    private val localRepoMock: LocalRepoOperator = mockk()
    private val client = GithubComClient(
        httpClientProvider = clientProvider,
        localRepoOperator = localRepoMock,
        json = SerializationHolder.instance.readableJson
    )

    companion object {
        private const val MEGA_MANIPULATOR_REPO = "mega-manipulator"
    }

    @Test
    fun getAllPrs() {
        // given

        // when
        val prs = runBlocking {
            client.getAllPrs(
                searchHost = settings.searchHostSettings.keys.first(),
                codeHost = codeHost,
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
            searchHostName = searchHost,
            codeHostName = codeHost,
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

    @Test
    internal fun `validate access token`() {
        val result: String = runBlocking {
            client.validateAccess(
                searchHost = searchHost,
                codeHost = codeHost,
                settings = githubSettings
            )
        }

        assertThat(result, startsWith("200:OK"))
    }
}
