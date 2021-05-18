package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.TestUiProtector
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.test.assertTrue

@ExperimentalPathApi
@ExtendWith(MockKExtension::class)
class ApplyOperatorTest {

    private val filesOperator: FilesOperator = mockk(relaxed = true)
    private val projectOperator: ProjectOperator = mockk()
    private val localRepoOperator: LocalRepoOperator = mockk()
    private val processOperator: ProcessOperator = mockk()
    private val uiProtector: UiProtector = TestUiProtector()

    private val project: Project = mockk()
    private val settingsFileOperator: SettingsFileOperator = mockk {
        every { readSettings() } returns mockk {
            every { resolveSettings(any(), any()) } returns (
                mockk<SearchHostSettings>() to mockk {
                    every { username } returns "username"
                    every { cloneType } returns HTTPS
                    every { baseUrl } returns "https://example"
                }
                )
        }
    }
    private val applyOperator = ApplyOperator(
        settingsFileOperator = settingsFileOperator,
        filesOperator = filesOperator,
        processOperator = processOperator,
        localRepoOperator = localRepoOperator,
        uiProtector = uiProtector,
    )

    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())

    @BeforeEach
    internal fun setUp() {
        every { project.basePath } returns tempDir.absolutePath
        every { projectOperator.project } returns project
    }

    @AfterEach
    internal fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun apply() = runBlocking {
        assertTrue(true)
    }
}
