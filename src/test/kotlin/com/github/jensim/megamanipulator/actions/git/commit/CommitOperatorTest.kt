package com.github.jensim.megamanipulator.actions.git.commit

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.TestUiProtector
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
@ExtendWith(MockKExtension::class)
class CommitOperatorTest {

    private val dialogGenerator: DialogGenerator = mockk()
    private val settingsFileOperator: SettingsFileOperator = mockk()
    private val localRepoOperator: LocalRepoOperator = mockk()
    private val processOperator: ProcessOperator = mockk()
    private val prRouter: PrRouter = mockk()
    private val uiProtector: TestUiProtector = TestUiProtector()

    private val commitOperator: CommitOperator = CommitOperator(
        dialogGenerator,
        settingsFileOperator,
        localRepoOperator,
        processOperator,
        prRouter,
        uiProtector
    )
    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())

    private val successOutput = ApplyOutput("any", "any", "any", 0)
    private val unsuccessfulOutput = ApplyOutput("any", "any", "any", 1)

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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun commit(push: Boolean) {
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
        val commitMessage = "This is my first commit"
        every { dialogGenerator.askForInput(any(), any()) } returns commitMessage
        every { settingsFileOperator.readSettings() } returns settings
        every { dialogGenerator.showConfirm(any(), any()) } returns push
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)

        val searchResultSlot = slot<SearchResult>()
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns false
        coEvery { localRepoOperator.push(file) } returns unsuccessfulOutput
        coEvery { prRouter.createFork(capture(searchResultSlot)) } returns "clonedUrl"
        coEvery { localRepoOperator.addForkRemote(file, "clonedUrl") } returns successOutput

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
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)

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
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns true
        coEvery { localRepoOperator.push(file) } returns successOutput

        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        commitOperator.commitProcess(file, result, commitMessage, true, settings)

        verify(exactly = 2) { processOperator.runCommandAsync(file, any()) }
        coVerify { localRepoOperator.push(file) }
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

        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns false
        coEvery { localRepoOperator.push(file) } returns unsuccessfulOutput
        coEvery { prRouter.createFork(capture(searchResultSlot)) } returns "clonedUrl"
        coEvery { localRepoOperator.addForkRemote(file, "clonedUrl") } returns successOutput

        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        commitOperator.commitProcess(file, result, commitMessage, true, settings)

        verify(exactly = 2) { processOperator.runCommandAsync(file, any()) }
        coVerify { prRouter.createFork(searchResultSlot.captured) }

        val captured = searchResultSlot.captured
        assertThat(captured.searchHostName, equalTo("searchHostName"))
        coVerify { localRepoOperator.addForkRemote(file, "clonedUrl") }
        coVerify(exactly = 2) { localRepoOperator.push(file) }
    }

    @Test
    fun `commit process and push with eager fork`() = runBlocking {
        val codeHostSettings: CodeHostSettings = mockk {
            every { forkSetting } returns ForkSetting.EAGER_FORK
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

        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns false
        coEvery { localRepoOperator.push(file) } returns successOutput
        coEvery { prRouter.createFork(capture(searchResultSlot)) } returns "clonedUrl"
        coEvery { localRepoOperator.addForkRemote(file, "clonedUrl") } returns successOutput

        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        commitOperator.commitProcess(file, result, commitMessage, true, settings)

        verify(exactly = 2) { processOperator.runCommandAsync(file, any()) }
        coVerify { prRouter.createFork(searchResultSlot.captured) }

        val captured = searchResultSlot.captured
        assertThat(captured.searchHostName, equalTo("searchHostName"))
        coVerify { localRepoOperator.addForkRemote(file, "clonedUrl") }
        coVerify { localRepoOperator.push(file) }
    }

    @Test
    fun pushWithoutConfirmation() {
        every { dialogGenerator.showConfirm(any(), any()) } returns false
        val result = commitOperator.push()
        assertTrue(result.containsKey("no result"))
        verify { dialogGenerator.showConfirm("Push", "Push local commits to remote") }
    }

    @Test
    fun push() {
        val codeHostSettings: CodeHostSettings = mockk {
            every { forkSetting } returns ForkSetting.EAGER_FORK
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

        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)
        every { localRepoOperator.hasFork(file) } returns false
        coEvery { localRepoOperator.push(file) } returns successOutput
        coEvery { prRouter.createFork(capture(searchResultSlot)) } returns "clonedUrl"
        coEvery { localRepoOperator.addForkRemote(file, "clonedUrl") } returns successOutput
        every { dialogGenerator.showConfirm(any(), any()) } returns true
        every { settingsFileOperator.readSettings() } returns settings
        every { localRepoOperator.getLocalRepoFiles() } returns listOf(file)
        every { processOperator.runCommandAsync(file, any()) } returns CompletableDeferred(successOutput)

        commitOperator.push()
        verify { dialogGenerator.showConfirm("Push", "Push local commits to remote") }
    }
}
