package com.github.jensim.megamanipulator.actions.git.commit

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.apply.StepResult
import com.github.jensim.megamanipulator.actions.git.GitUrlHelper
import com.github.jensim.megamanipulator.actions.git.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettings
import com.intellij.openapi.project.Project
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
@ExtendWith(MockKExtension::class)
class CommitOperatorTest {

    private val settingsFileOperator: SettingsFileOperator = mockk()
    private val localRepoOperator: LocalRepoOperator = mockk()
    private val processOperator: ProcessOperator = mockk()
    private val prRouter: PrRouter = mockk()
    private val gitUrlHelper: GitUrlHelper = mockk()
    private val project: Project = mockk()

    private val commitOperator: CommitOperator = CommitOperator(
        project = project,
        settingsFileOperator = settingsFileOperator,
        localRepoOperator = localRepoOperator,
        processOperator = processOperator,
        prRouter = prRouter,
        gitUrlHelper = gitUrlHelper,
    )
    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())

    private val successOutput = ApplyOutput(dir = "any", std = "any", exitCode = 0)
    private val unsuccessfulOutput = ApplyOutput(dir = "any", std = "any", exitCode = 1)

    @AfterEach
    internal fun tearDown() {
        tempDir.deleteRecursively()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun commit(push: Boolean) {
        val codeHostSettings: CodeHostSettings = mockk {
            every { forkSetting } returns ForkSetting.PLAIN_BRANCH
        }
        val settings: MegaManipulatorSettings = mockk {
            every { resolveSettings(any())!!.second } returns codeHostSettings
        }
        val file: File = mockk {
            every { path } returns ".tmp"
            every { name } returns "filename"
            every { parentFile.name } returns "parentName"
            every { parentFile.parentFile.name } returns "parentParentName"
            every { parentFile.parentFile.parentFile.name } returns "searchHostName"
        }
        val commitMessage = "This is my first commit"
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)

        every { gitUrlHelper.buildCloneUrl(any(), any<String>()) } returns "clonedUrl"
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns false
        coEvery { localRepoOperator.push(file, false) } returnsMany listOf(successOutput)
        val result = ConcurrentHashMap<String, MutableList<StepResult>>()

        runBlocking { commitOperator.commitProcess(file, result, commitMessage, push, false, settings) }

        assertThat(result.size, equalTo(1))
        val nextElement: MutableList<StepResult> = result.elements().nextElement()
        assertNotNull(nextElement)
        val (action, applyOutput) = nextElement.last()
        assertThat(action, equalTo(if (push) "push" else "commit"))
        assertThat(applyOutput.exitCode, equalTo(0))
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
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)

        val result = ConcurrentHashMap<String, MutableList<StepResult>>()
        commitOperator.commitProcess(repoDir = file, result = result, commitMessage = commitMessage, push = false, force = false, settings = settings)

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
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns true
        coEvery { localRepoOperator.push(file, false) } returns successOutput

        val result = ConcurrentHashMap<String, MutableList<StepResult>>()
        commitOperator.commitProcess(repoDir = file, result = result, commitMessage = commitMessage, push = true, force = false, settings = settings)

        verify(exactly = 2) { processOperator.runCommandAsync(file, any()) }
        coVerify { localRepoOperator.push(file, false) }
    }

    @Test
    fun `commit process and push with lazy fork`() = runBlocking {
        val codeHostSettings: CodeHostSettings = mockk {
            every { forkSetting } returns ForkSetting.LAZY_FORK
        }
        val settings: MegaManipulatorSettings = mockk {
            every { resolveSettings(any())!!.second } returns codeHostSettings
        }
        val file: File = mockk {
            every { path } returns ".tmp"
            every { name } returns "filename"
            every { parentFile.name } returns "parentName"
            every { parentFile.parentFile.name } returns "parentParentName"
            every { parentFile.parentFile.parentFile.name } returns "searchHostName"
        }
        val searchResultSlot = slot<SearchResult>()
        val commitMessage = "This is my first commit"
        every { gitUrlHelper.buildCloneUrl(any(), any<String>()) } returns "clonedUrl"
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns false
        coEvery { localRepoOperator.push(file, false) } returns unsuccessfulOutput
        coEvery { prRouter.createFork(capture(searchResultSlot)) } returns "clonedUrl"
        coEvery { localRepoOperator.addForkRemote(file, "clonedUrl") } returns successOutput

        val result = ConcurrentHashMap<String, MutableList<StepResult>>()
        commitOperator.commitProcess(repoDir = file, result = result, commitMessage = commitMessage, push = true, force = false, settings = settings)

        verify(exactly = 2) { processOperator.runCommandAsync(file, any()) }
        coVerify { prRouter.createFork(searchResultSlot.captured) }

        val captured = searchResultSlot.captured
        assertThat(captured.searchHostName, equalTo("searchHostName"))
        coVerify { localRepoOperator.addForkRemote(file, "clonedUrl") }
        coVerify(exactly = 2) { localRepoOperator.push(file, false) }
    }

    @Test
    fun `commit process and push with eager fork`() {
        // given
        val codeHostSettings: CodeHostSettings = mockk {
            every { forkSetting } returns ForkSetting.EAGER_FORK
        }
        val settings: MegaManipulatorSettings = mockk {
            every { resolveSettings(any()) } returns Pair(mockk(), codeHostSettings)
        }
        val file: File = mockk {
            every { path } returns ".tmp"
            every { name } returns "filename"
            every { parentFile.name } returns "parentName"
            every { parentFile.parentFile.name } returns "parentParentName"
            every { parentFile.parentFile.parentFile.name } returns "searchHostName"
        }
        val searchResultSlot = slot<SearchResult>()
        val commitMessage = "This is my first commit"
        every { gitUrlHelper.buildCloneUrl(any(), any<String>()) } returns "clonedUrl"
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns false
        coEvery { localRepoOperator.push(file, false) } returns successOutput
        coEvery { prRouter.createFork(capture(searchResultSlot)) } returns "clonedUrl"
        coEvery { localRepoOperator.addForkRemote(file, "clonedUrl") } returns successOutput
        val resultAggregate = ConcurrentHashMap<String, MutableList<StepResult>>()

        // when
        runBlocking {
            commitOperator.commitProcess(
                repoDir = file,
                result = resultAggregate,
                commitMessage = commitMessage,
                push = true,
                force = false,
                settings = settings
            )
        }

        // then
        verify(exactly = 2) { processOperator.runCommandAsync(file, any()) }
        coVerify { prRouter.createFork(searchResultSlot.captured) }

        val captured = searchResultSlot.captured
        assertThat(captured.searchHostName, equalTo("searchHostName"))
        coVerify { localRepoOperator.addForkRemote(file, "clonedUrl") }
        coVerify { localRepoOperator.push(file, false) }
    }

    @Test
    fun push() {
        // given
        val codeHostSettings: CodeHostSettings = mockk {
            every { forkSetting } returns ForkSetting.PLAIN_BRANCH
        }
        val settings: MegaManipulatorSettings = mockk {
            every { resolveSettings(any())!!.second } returns codeHostSettings
        }
        val file: File = mockk {
            every { path } returns ".tmp"
            every { name } returns "filename"
            every { parentFile.name } returns "parentName"
            every { parentFile.parentFile.name } returns "parentParentName"
            every { parentFile.parentFile.parentFile.name } returns "searchHostName"
        }
        every { gitUrlHelper.buildCloneUrl(any(), any<String>()) } returns "clonedUrl"
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns false
        coEvery { localRepoOperator.push(file, false) } returns successOutput
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)

        // when
        val result: List<StepResult> = runBlocking { commitOperator.push(settings, file, false) }

        // then
        assertThat(result.lastOrNull()?.result?.exitCode, equalTo(0))
        coVerify { localRepoOperator.push(file, false) }
    }
}
