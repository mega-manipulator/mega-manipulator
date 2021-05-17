package com.github.jensim.megamanipulator.test

import com.github.jensim.megamanipulator.ApplicationWiring
import com.github.jensim.megamanipulator.MyBundle
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.passwords.ProjectOperator
import com.github.jensim.megamanipulator.settings.types.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.ForkSetting.PLAIN_BRANCH
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings.SourceGraphSettings
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
open class TestApplicationWiring {

    @AfterEach
    internal fun tearDown() {
        tempDirPath.delete(true)
    }

    @BeforeEach
    internal fun setUp() {
        every { mockProject.basePath } returns tempDir.absolutePath
        every { projectOperator.project } returns mockProject
    }

    open val envHelper = EnvHelper()
    open val mockProject: Project = mockk(relaxed = true)
    open val projectOperator: ProjectOperator = mockk(relaxed = true)
    open val notificationsOperator: NotificationsOperator = mockk(relaxed = true)
    open val dialogGenerator: DialogGenerator = mockk(relaxed = true)
    open val filesOperator: FilesOperator = mockk(relaxed = true)

    val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    val tempDir: File = File(tempDirPath.toUri())
    open val githubUsername get() = envHelper.resolve(GITHUB_USERNAME)
    open val sourcegraphToken get() = envHelper.resolve(SRC_COM_ACCESS_TOKEN)
    open val githubToken get() = envHelper.resolve(GITHUB_TOKEN)
    open val codeHostName get() = "github.com"
    open val gitHubSettings get() = GitHubSettings(
        username = githubUsername,
        forkSetting = PLAIN_BRANCH,
        cloneType = HTTPS,
    )
    open val sourceGraphSettings get() = SourceGraphSettings(
        baseUrl = "https://sourcegraph.com",
        codeHostSettings = mapOf(
            codeHostName to gitHubSettings
        )
    )
    open val searchHostName = "sourcegraph.com"
    open val settings get() = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            searchHostName to sourceGraphSettings
        )
    )

    open val uiProtector: UiProtector get() = TestUiProtector()
    open val passwordsOperator: PasswordsOperator
        get() = TestPasswordOperator(
            mapOf(
                "token" to sourceGraphSettings.baseUrl to sourcegraphToken,
                githubUsername to gitHubSettings.baseUrl to githubToken
            )
        )
    open val settingsFileOperator: SettingsFileOperator = mockk {
        every { readSettings() } returns settings
        every { validationText } returns "Looks good..?"
    }
    open val myBundle: MyBundle = mockk {
        every { message(any()) } returns UUID.randomUUID().toString()
    }

    open val applicationWiring by lazy {
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
