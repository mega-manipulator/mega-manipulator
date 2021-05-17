package com.github.jensim.megamanipulator.actions.vcs.gitlab

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GitLabRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.settings.CloneType
import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITLAB_GROUP
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITLAB_PROJECT
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITLAB_TOKEN
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITLAB_USERNAME
import com.github.jensim.megamanipulator.test.TestApplicationWiring
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.emptyIterable
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.fail
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
@EnabledIf("com.github.jensim.megamanipulator.actions.vcs.gitlab.GitLabClientTest#enabled", disabledReason = "We don't have a CI installation of bitbucket server")
internal class GitLabClientTest {

    companion object {

        private val envHelper = EnvHelper()

        @JvmStatic
        fun enabled(): Boolean = try {
            envHelper.resolve(GITLAB_TOKEN)
            true
        } catch (e: NullPointerException) {
            false
        }
    }

    private val gitlabSettings = CodeHostSettings.GitLabSettings(
        username = envHelper.resolve(GITLAB_USERNAME),
        cloneType = CloneType.HTTPS,
    )
    private val wiring = object : TestApplicationWiring() {
        override val envHelper: EnvHelper = GitLabClientTest.envHelper
        override val codeHostName: String = "gitlab.com"
        override val sourceGraphSettings = SearchHostSettings.SourceGraphSettings(
            baseUrl = "https://sourcegraph.com",
            codeHostSettings = mapOf(
                codeHostName to gitlabSettings
            )
        )
        override val passwordsOperator: PasswordsOperator = TestPasswordOperator(
            mapOf(
                "token" to sourceGraphSettings.baseUrl to sourcegraphToken,
                gitlabSettings.username to gitlabSettings.baseUrl to envHelper.resolve(GITLAB_TOKEN)
            )
        )
    }

    @Test
    fun getRepo() {
        // given

        // when
        val repo: RepoWrapper = runBlocking {
            wiring.applicationWiring.gitLabClient.getRepo(
                searchResult = SearchResult(project = envHelper.resolve(GITLAB_GROUP), repo = envHelper.resolve(GITLAB_PROJECT), codeHostName = wiring.codeHostName, searchHostName = wiring.searchHostName),
                settings = gitlabSettings
            )
        }

        // then
        if (repo is GitLabRepoWrapping) {
            assertThat(repo.repo.fullPath, equalTo("${envHelper.resolve(GITLAB_GROUP)}/${envHelper.resolve(GITLAB_PROJECT)}"))
        } else {
            fail("Repo is mot of type GitLabRepoWrapping")
        }
    }

    @Test
    fun validateAccess() {
        // given

        // when
        val result: String = runBlocking {
            wiring.applicationWiring.gitLabClient.validateAccess(
                searchHost = wiring.searchHostName, codeHost = wiring.codeHostName, settings = gitlabSettings
            )
        }

        // then
        assertThat(result, equalTo("OK"))
    }

    @Test
    fun getAllPrs() {
        // given

        // when
        val prs: List<PullRequestWrapper> = runBlocking {
            wiring.applicationWiring.gitLabClient.getAllPrs(
                searchHost = wiring.searchHostName, codeHost = wiring.codeHostName, settings = gitlabSettings
            )
        }

        // then
        assertThat(prs, not(emptyIterable()))
    }
}
