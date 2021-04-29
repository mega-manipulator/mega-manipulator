package com.github.jensim.megamanipulator.actions.git.commit

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.ForkSetting
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.project.Project
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
@ExtendWith(MockKExtension::class)
class CommitOperatorTest {

    @MockK
    private lateinit var dialogGenerator: DialogGenerator

    @MockK
    private lateinit var settingsFileOperator: SettingsFileOperator

    @MockK
    private lateinit var localRepoOperator: LocalRepoOperator

    @MockK
    private lateinit var processOperator: ProcessOperator

    @MockK
    private lateinit var prRouter: PrRouter

    @MockK(relaxed = true)
    private lateinit var uiProtector: UiProtector

    @MockK
    private lateinit var project: Project

    @InjectMockKs
    private lateinit var commitOperator: CommitOperator
    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())

    companion object {
        private const val CLONING_TITLE_AND_MESSAGE = "Cloning repos"
        private const val PROJECT = "mega-manipulator"
        private const val BASE_REPO = "base_repo"
    }

    @AfterEach
    internal fun tearDown() {
        tempDir.deleteRecursively()
    }

    @ParameterizedTest
    @NullAndEmptySource
    fun `commit with null or empty message`(response: String?) {
        every { dialogGenerator.askForInput(any(), any()) } returns response
        every { dialogGenerator.showConfirm(any(), any()) } returns true
        val result = commitOperator.commit()

        verify {
            dialogGenerator.askForInput(
                "Create commits for all changes in all checked out repositories",
                "Commit message"
            )
        }
        verify { dialogGenerator.showConfirm("Info", "No commit performed!") }

        assertThat(result.keys.first(), equalTo("no result"))
    }

    @Test
    fun commit() {
        val settings: MegaManipulatorSettings = mockk {}
        val file: File = mockk {
            every { path } returns ".tmp"
        }
        val commitMessage = "This is my first commit"
        every { dialogGenerator.askForInput(any(), any()) } returns commitMessage
        every { settingsFileOperator.readSettings() } returns settings
        every { dialogGenerator.showConfirm(any(), any()) } returns false
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(
            ApplyOutput(
                "any",
                "any",
                "any",
                0
            )
        )

        commitOperator.commit()

        verify {
            dialogGenerator.askForInput(
                "Create commits for all changes in all checked out repositories",
                "Commit message"
            )
        }
        verify { dialogGenerator.showConfirm("Also push?", "Also push? $commitMessage") }
    }

    @Test
    fun `commit process without pushing`() = runBlocking {
        val settings: MegaManipulatorSettings = mockk {}
        val file: File = mockk {
            every { path } returns ".tmp"
        }
        val commitMessage = "This is my first commit"
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(
            ApplyOutput(
                "any",
                "any",
                "any",
                0
            )
        )

        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        commitOperator.commitProcess(file, result, commitMessage, false, settings)

        verify { localRepoOperator wasNot Called }
        verify(exactly = 2) { processOperator.runCommandAsync(file, any()) }
    }

    @Test
    fun `commit process and push with fork`() = runBlocking {
        val codeHostSettings: CodeHostSettings = mockk {
            every { forkSetting } returns ForkSetting.PLAIN_BRANCH
        }
        val settings: MegaManipulatorSettings = mockk {
            every { resolveSettings(any())!!.second } returns codeHostSettings
        }
        val file: File = mockk {
            every { path } returns ".tmp"
        }
        val commitMessage = "This is my first commit"
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(
            ApplyOutput(
                "any",
                "any",
                "any",
                0
            )
        )
        every { localRepoOperator.hasFork(file) } returns true
        coEvery { localRepoOperator.push(file) } returns ApplyOutput("any", "any", "any", 0)

        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        commitOperator.commitProcess(file, result, commitMessage, true, settings)

        verify(exactly = 2) { processOperator.runCommandAsync(file, any()) }
        coVerify { localRepoOperator.push(file) }
    }
}
