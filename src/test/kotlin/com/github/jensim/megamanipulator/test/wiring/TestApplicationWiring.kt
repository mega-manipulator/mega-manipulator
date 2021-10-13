package com.github.jensim.megamanipulator.test.wiring

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
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
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.passwords.ProjectOperator
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

    val gitUrlHelper by lazy {
        GitUrlHelper(
            project = mockProject,
            passwordsOperator = passwordsOperator,
        )
    }

    val processOperator by lazy {
        ProcessOperator(
            project = mockProject,
            projectOperator = projectOperator,
        )
    }
    val localRepoOperator by lazy {
        LocalRepoOperator(
            project = mockProject,
            projectOperator = projectOperator,
            processOperator = processOperator,
            uiProtector = uiProtector,
        )
    }
    val httpClientProvider by lazy {
        HttpClientProvider(
            project = mockProject,
            settingsFileOperator = settingsFileOperator,
            passwordsOperator = passwordsOperator,
            notificationsOperator = notificationsOperator,
        )
    }
    val gitLabClient by lazy {
        GitLabClient(
            project = mockProject,
            httpClientProvider = httpClientProvider,
            localRepoOperator = localRepoOperator,
        )
    }
    val bitbucketServerClient by lazy {
        BitbucketServerClient(
            project = mockProject,
            httpClientProvider = httpClientProvider,
            localRepoOperator = localRepoOperator,
        )
    }
    val githubComClient by lazy {
        GithubComClient(
            project = mockProject,
            httpClientProvider = httpClientProvider,
            localRepoOperator = localRepoOperator,
        )
    }
    val prRouter by lazy {
        PrRouter(
            project = mockProject,
            settingsFileOperator = settingsFileOperator,
            bitbucketServerClient = bitbucketServerClient,
            githubComClient = githubComClient,
            gitLabClient = gitLabClient,
            notificationsOperator = notificationsOperator,
        )
    }
    val cloneOperator by lazy {
        CloneOperator(
            project = mockProject,
            filesOperator = filesOperator,
            projectOperator = projectOperator,
            prRouter = prRouter,
            localRepoOperator = localRepoOperator,
            processOperator = processOperator,
            notificationsOperator = notificationsOperator,
            uiProtector = uiProtector,
            settingsFileOperator = settingsFileOperator,
            gitUrlHelper = gitUrlHelper,
        )
    }
    val applyOperator by lazy {
        ApplyOperator(
            project = mockProject,
            settingsFileOperator = settingsFileOperator,
            filesOperator = filesOperator,
            processOperator = processOperator,
            localRepoOperator = localRepoOperator,
            uiProtector = uiProtector,
        )
    }
    val commitOperator by lazy {
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
