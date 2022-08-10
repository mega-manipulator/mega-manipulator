package com.github.jensim.megamanipulator.actions.vcs.githubcom

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.git.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GithubComPullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.GithubComRepoWrapping
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.types.ForkSetting.PLAIN_BRANCH
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsType
import com.github.jensim.megamanipulator.settings.types.codehost.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.searchhost.SourceGraphSettings
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.KotlinMatcher.kMatch
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.hamcrest.Matchers.`is` as Is

class GithubComClientTest {

    private val project: Project = mockk()
    private val envHelper = EnvHelper()
    private val githubSettings = GitHubSettings(
        username = envHelper.resolve(EnvHelper.EnvProperty.GITHUB_USERNAME)!!,
        forkSetting = PLAIN_BRANCH,
        cloneType = HTTPS,
    )
    private val password = envHelper.resolve(EnvHelper.EnvProperty.GITHUB_TOKEN)!!
    private val codeHost = "github.com"
    private val searchHost = "sourcegraph.com"
    private val settings = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            searchHost to SearchHostSettingsGroup(
                sourceGraph = SourceGraphSettings(
                    username = envHelper.resolve(EnvHelper.EnvProperty.SRC_COM_USERNAME)!!,
                    baseUrl = "https://sourcegraph.com",
                    codeHostSettings = mapOf(codeHost to CodeHostSettingsGroup(gitHub = githubSettings))
                )
            )
        )
    )
    private val settingsMock: SettingsFileOperator = mockk {
        every { readSettings() } returns settings
    }
    private val passwordsOperator = TestPasswordOperator(mapOf(githubSettings.username to githubSettings.baseUrl to password))
    private val notificationsMock: NotificationsOperator = mockk()
    private val clientProvider = HttpClientProvider(
        project = project,
        settingsFileOperator = settingsMock,
        passwordsOperator = passwordsOperator,
        notificationsOperator = notificationsMock
    )
    private val localRepoMock: LocalRepoOperator = mockk()
    private val client = GithubComClient(
        project = project,
        httpClientProvider = clientProvider,
        localRepoOperator = localRepoMock,
    )

    companion object {
        private const val MEGA_MANIPULATOR_GROUP = "mega-manipulator"
        private const val MEGA_MANIPULATOR_REPO = "mega-manipulator.github.io"
    }

    @Test
    fun getAllPrs() {
        // given

        // when
        val prs: List<GithubComPullRequestWrapper> = runBlocking {
            client.getAllPrs(
                searchHost = settings.searchHostSettings.keys.first(),
                codeHost = codeHost,
                settings = githubSettings,
                limit = 10,
                state = CodeHostSettingsType.GITHUB.prStateOpen,
                role = githubSettings.codeHostType.prRoleAuthor,
                project = null,
                repo = null,
            )
        }

        // then
        assertThat(prs, not(nullValue()))
        // No duplicates
        val prIds = prs.map { it.pullRequest.id }
        val uniquePrIds = prIds.toSet()
        assertEquals(prIds.size, uniquePrIds.size)
        assertThat(prs, everyItem(kMatch { it.state() == CodeHostSettingsType.GITHUB.prStateOpen }))
        assertThat(prs, everyItem(kMatch { it.author() == githubSettings.username }))
    }

    @Test
    fun getRepo() {
        // given
        val repo = SearchResult(
            searchHostName = searchHost,
            codeHostName = codeHost,
            project = MEGA_MANIPULATOR_GROUP,
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
        val result: String? = runBlocking {
            client.validateAccess(
                searchHost = searchHost,
                codeHost = codeHost,
                settings = githubSettings
            )
        }

        assertThat(result, Is(nullValue()))
    }
}
