package com.github.jensim.megamanipulator.test.wiring

import com.github.jensim.megamanipulator.ApplicationWiring
import com.github.jensim.megamanipulator.MyBundle
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.files.FilesOperator
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
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
open class TestApplicationWiring {

    open val envHelper = EnvUserSettingsSetup.helper
    open val mockProject: Project = mockk(relaxed = true)
    open val projectOperator: ProjectOperator = mockk(relaxed = true)
    open val notificationsOperator: NotificationsOperator = mockk(relaxed = true)
    open val dialogGenerator: DialogGenerator = mockk(relaxed = true)
    open val filesOperator: FilesOperator = mockk(relaxed = true)

    val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    val tempDir: File = File(tempDirPath.toUri())
    open val settings get() = EnvUserSettingsSetup.settings
    open val passwordsOperator get() = EnvUserSettingsSetup.passwordsOperator
    open val uiProtector: UiProtector get() = TestUiProtector()

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
