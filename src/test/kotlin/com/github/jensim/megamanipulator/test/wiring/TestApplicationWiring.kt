package com.github.jensim.megamanipulator.test.wiring

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.ProcessOperatorImpl
import com.github.jensim.megamanipulator.actions.apply.ApplyOperator
import com.github.jensim.megamanipulator.actions.git.GitUrlHelper
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitbucketServerClient
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComClient
import com.github.jensim.megamanipulator.actions.vcs.gitlab.GitLabClient
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.project.ProjectOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.TestUiProtector
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
open class TestApplicationWiring {

    open val envHelper = EnvUserSettingsSetup.helper
    val mockProject: Project = mockk(relaxed = true)
    val projectOperator: ProjectOperator = mockk(relaxed = true)
    val notificationsOperator: NotificationsOperator = mockk(relaxed = true)
    val dialogGenerator: DialogGenerator = mockk(relaxed = true)
    val filesOperator: FilesOperator = mockk(relaxed = true)

    val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    val tempDir: File = File(tempDirPath.toUri())
    val settings get() = EnvUserSettingsSetup.settings
    val passwordsOperator get() = EnvUserSettingsSetup.passwordsOperator
    val uiProtector: UiProtector get() = TestUiProtector()

    val settingsFileOperator: SettingsFileOperator = mockk {
        every { readSettings() } returns settings
        every { validationText } returns "Looks good..?"
    }

    val gitUrlHelper: GitUrlHelper by lazy {
        GitUrlHelper(
            project = mockProject,
            passwordsOperator = passwordsOperator,
        )
    }

    val processOperator: ProcessOperator by lazy {
        ProcessOperatorImpl(
            project = mockProject,
        )
    }
    val localRepoOperator: LocalRepoOperator by lazy {
        LocalRepoOperator(
            project = mockProject,
            processOperator = processOperator,
            uiProtector = uiProtector,
        )
    }
    val httpClientProvider: HttpClientProvider by lazy {
        HttpClientProvider(
            project = mockProject,
            settingsFileOperator = settingsFileOperator,
            passwordsOperator = passwordsOperator,
            notificationsOperator = notificationsOperator,
        )
    }
    val gitLabClient: GitLabClient by lazy {
        GitLabClient(
            project = mockProject,
            httpClientProvider = httpClientProvider,
            localRepoOperator = localRepoOperator,
        )
    }
    val bitbucketServerClient: BitbucketServerClient by lazy {
        BitbucketServerClient(
            project = mockProject,
            httpClientProvider = httpClientProvider,
            localRepoOperator = localRepoOperator,
        )
    }
    val githubComClient: GithubComClient by lazy {
        GithubComClient(
            project = mockProject,
            httpClientProvider = httpClientProvider,
            localRepoOperator = localRepoOperator,
        )
    }
    val prRouter: PrRouter by lazy {
        PrRouter(
            project = mockProject,
            settingsFileOperator = settingsFileOperator,
            bitbucketServerClient = bitbucketServerClient,
            githubComClient = githubComClient,
            gitLabClient = gitLabClient,
            notificationsOperator = notificationsOperator,
        )
    }
    val cloneOperator: CloneOperator by lazy {
        CloneOperator(
            project = mockProject,
            filesOperator = filesOperator,
            prRouter = prRouter,
            localRepoOperator = localRepoOperator,
            processOperator = processOperator,
            notificationsOperator = notificationsOperator,
            uiProtector = uiProtector,
            settingsFileOperator = settingsFileOperator,
            gitUrlHelper = gitUrlHelper,
        )
    }
    val applyOperator: ApplyOperator by lazy {
        ApplyOperator(
            project = mockProject,
            settingsFileOperator = settingsFileOperator,
            filesOperator = filesOperator,
            processOperator = processOperator,
            localRepoOperator = localRepoOperator,
            uiProtector = uiProtector,
        )
    }
    val commitOperator: CommitOperator by lazy {
        CommitOperator(
            project = mockProject,
            settingsFileOperator = settingsFileOperator,
            localRepoOperator = localRepoOperator,
            processOperator = processOperator,
            prRouter = prRouter,
            gitUrlHelper = gitUrlHelper,
        )
    }
}
