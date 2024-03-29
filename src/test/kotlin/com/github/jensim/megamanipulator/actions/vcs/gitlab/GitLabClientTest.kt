package com.github.jensim.megamanipulator.actions.vcs.gitlab

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GitLabRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.settings.types.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsType.GITLAB
import com.github.jensim.megamanipulator.settings.types.codehost.GitLabSettings
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITLAB_GROUP
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITLAB_PROJECT
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITLAB_TOKEN
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITLAB_USERNAME
import com.github.jensim.megamanipulator.test.wiring.EnvUserSettingsSetup
import com.github.jensim.megamanipulator.test.wiring.TestApplicationWiring
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.fail
import kotlin.io.path.ExperimentalPathApi
import org.hamcrest.Matchers.`is` as Is

@ExperimentalPathApi
@EnabledIf("com.github.jensim.megamanipulator.actions.vcs.gitlab.GitLabClientTest#enabled", disabledReason = "We don't have a CI installation of bitbucket server")
internal class GitLabClientTest {

    companion object {

        private val envHelper = EnvHelper()

        @JvmStatic
        fun enabled(): Boolean = try {
            envHelper.resolve(GITLAB_TOKEN) != null
        } catch (e: NullPointerException) {
            false
        }
    }

    private val gitlabSettings = GitLabSettings(
        username = envHelper.resolve(GITLAB_USERNAME)!!,
        cloneType = HTTPS,
    )
    private val wiring = object : TestApplicationWiring() {
        override val envHelper: EnvHelper = GitLabClientTest.envHelper
    }

    @Test
    fun getRepo() {
        // given

        // when
        val repo: RepoWrapper = runBlocking {
            wiring.gitLabClient.getRepo(
                searchResult = SearchResult(
                    project = envHelper.resolve(GITLAB_GROUP)!!,
                    repo = envHelper.resolve(GITLAB_PROJECT)!!,
                    codeHostName = EnvUserSettingsSetup.gitlabSettings?.first!!,
                    searchHostName = EnvUserSettingsSetup.sourcegraphName,
                ),
                settings = gitlabSettings
            )
        }

        // then
        if (repo is GitLabRepoWrapping) {
            assertThat(repo.fullPath, equalTo("${envHelper.resolve(GITLAB_GROUP)}/${envHelper.resolve(GITLAB_PROJECT)}"))
        } else {
            fail("Repo is mot of type GitLabRepoWrapping")
        }
    }

    @Test
    fun validateAccess() {
        // given

        // when
        val result: String? = runBlocking {
            wiring.gitLabClient.validateAccess(
                searchHost = EnvUserSettingsSetup.sourcegraphName,
                codeHost = EnvUserSettingsSetup.gitlabSettings?.first!!,
                settings = gitlabSettings,
            )
        }

        // then
        assertThat(result, Is(nullValue()))
    }

    @Test
    fun getAllAuthoredPrs() {
        // given

        // when
        val prs: List<PullRequestWrapper> = runBlocking {
            wiring.gitLabClient.getAllPrs(
                searchHost = EnvUserSettingsSetup.sourcegraphName,
                codeHost = EnvUserSettingsSetup.gitlabSettings?.first!!,
                settings = gitlabSettings,
                limit = 10,
                state = GITLAB.prStateOpen,
                role = GITLAB.prRoleAuthor,
                project = null,
                repo = null,
            )
        }

        // then
        assertThat(prs, not(nullValue()))
    }

    @Test
    fun getAllAssignedPrs() {
        // given

        // when
        val prs: List<PullRequestWrapper> = runBlocking {
            wiring.gitLabClient.getAllPrs(
                searchHost = EnvUserSettingsSetup.sourcegraphName,
                codeHost = EnvUserSettingsSetup.gitlabSettings?.first!!,
                settings = gitlabSettings,
                limit = 10,
                state = GITLAB.prStateOpen,
                role = GITLAB.prRoleAssignee,
                project = null,
                repo = null,
            )
        }

        // then
        assertThat(prs, not(nullValue()))
    }
}
