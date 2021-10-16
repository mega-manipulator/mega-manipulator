package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.project.ProjectOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings
import com.github.jensim.megamanipulator.ui.TestUiProtector
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
class ApplyOperatorTest {

    private val filesOperator: FilesOperator = mockk(relaxed = true)
    private val projectOperator: ProjectOperator = mockk()
    private val localRepoOperator: LocalRepoOperator = mockk()
    private val processOperator: ProcessOperator = mockk()
    private val uiProtector: UiProtector = TestUiProtector()
    private val successOutput = ApplyOutput("any", "any", "any", 0)

    private val project: Project = mockk()
    private val settingsFileOperator: SettingsFileOperator = mockk {
        every { readSettings() } returns mockk {
            every { resolveSettings(any(), any()) } returns (
                mockk<SearchHostSettings>() to mockk {
                    every { username } returns "username"
                    every { cloneType } returns CloneType.HTTPS
                    every { baseUrl } returns "https://example"
                }
                )
        }
    }
    private val applyOperator = ApplyOperator(
        project = project,
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
    fun applyEmptyScriptFile() = runBlocking {
        val file = mockk<File> {
            every { exists() } returns false
        }
        every { settingsFileOperator.scriptFile } returns file
        val list = applyOperator.apply()
        assertTrue(list.isEmpty())
    }

    @Test
    fun apply() = runBlocking {
        val file = mockk<File> {
            every { parentFile.parentFile.name } returns "all"
            every { parentFile.name } returns "project"
            every { name } returns "file1"
        }
        val file2 = mockk<File> {
            every { parentFile.parentFile.name } returns "all"
            every { parentFile.name } returns "project"
            every { name } returns "file2"
        }

        val scriptFile = mockk<File> {
            every { exists() } returns true
            every { absolutePath } returns ".tmp/"
        }
        val settings: MegaManipulatorSettings = mockk {
            every { concurrency } returns 1
        }
        every { settingsFileOperator.scriptFile } returns scriptFile
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file, file2)
        every { settingsFileOperator.readSettings() } returns settings
        every { processOperator.runCommandAsync(any(), any()) } returns CompletableDeferred(successOutput)
        val list = applyOperator.apply()
        assertFalse(list.isEmpty())
        assertThat(list[0], equalTo(successOutput))

        verify { filesOperator.refreshConf() }
        verify(exactly = 2) { filesOperator.refreshClones() }
        verify { processOperator.runCommandAsync(file, listOf("/bin/bash", ".tmp/")) }
        verify { processOperator.runCommandAsync(file2, listOf("/bin/bash", ".tmp/")) }
    }
}
