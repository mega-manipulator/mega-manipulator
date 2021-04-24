package com.github.jensim.megamanipulator.test

import com.github.jensim.megamanipulator.ApplicationWiring
import com.github.jensim.megamanipulator.MyBundle
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.ForkSetting.PLAIN_BRANCH
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.PasswordsOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings.SourceGraphSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITHUB_TOKEN
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITHUB_USERNAME
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.SRC_COM_ACCESS_TOKEN
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.TestUiProtector
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.project.Project
import com.intellij.util.io.delete
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
abstract class TestApplicationWiring {

    @AfterEach
    internal fun tearDown() {
        tempDirPath.delete(true)
    }

    @BeforeEach
    internal fun setUp() {
        every { mockProject.basePath } returns tempDir.absolutePath
        every { projectOperator.project } returns mockProject
    }

    val envHelper = EnvHelper()
    val mockProject: Project = mockk(relaxed = true)
    val projectOperator: ProjectOperator = mockk(relaxed = true)
    val notificationsOperator: NotificationsOperator = mockk(relaxed = true)
    val dialogGenerator: DialogGenerator = mockk(relaxed = true)
    val filesOperator: FilesOperator = mockk(relaxed = true)

    val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    val tempDir: File = File(tempDirPath.toUri())
    val githubUsername = envHelper.resolve(GITHUB_USERNAME)
    val sourcegraphToken = envHelper.resolve(SRC_COM_ACCESS_TOKEN)
    val githubToken = envHelper.resolve(GITHUB_TOKEN)
    val codeHostName = "github.com"
    val gitHubSettings = GitHubSettings(
        username = githubUsername,
        forkSetting = PLAIN_BRANCH,
    )
    val sourceGraphSettings = SourceGraphSettings(
        baseUrl = "https://sourcegraph.com",
        codeHostSettings = mapOf(
            codeHostName to gitHubSettings
        )
    )
    val searchHostName = "sourcegraph.com"
    val settings = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            searchHostName to sourceGraphSettings
        )
    )

    val uiProtector: UiProtector = TestUiProtector()
    val passwordsOperator: PasswordsOperator = TestPasswordOperator(
        mapOf(
            "token" to sourceGraphSettings.baseUrl to sourcegraphToken,
            githubUsername to gitHubSettings.baseUrl to githubToken
        )
    )
    val settingsFileOperator: SettingsFileOperator = mockk {
        every { readSettings() } returns settings
        every { validationText } returns "Looks good..?"
    }

    val myBundle: MyBundle = mockk {
        every { message(any()) } returns UUID.randomUUID().toString()
    }

    val applicationWiring by lazy {
        ApplicationWiring(
            projectOperator = this.projectOperator,
            myBundleOverride = myBundle,
            filesOperatorOverride = filesOperator,
            dialogGeneratorOverride = dialogGenerator,
            settingsFileOperatorOverride = settingsFileOperator,
            uiProtectorOverride = uiProtector,
            notificationsOperatorOverride = notificationsOperator,
            notificationGroupManagerOverride = mockk(),
            passwordsOperatorOverride = passwordsOperator,
        )
    }
}
