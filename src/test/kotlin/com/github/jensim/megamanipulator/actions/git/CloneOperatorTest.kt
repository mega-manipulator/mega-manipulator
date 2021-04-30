package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.PasswordsOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.TestUiProtector
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
@ExtendWith(MockKExtension::class)
class CloneOperatorTest {

    private val filesOperator: FilesOperator = mockk(relaxed = true)
    private val projectOperator: ProjectOperator = mockk()
    private val prRouter: PrRouter = mockk()
    private val localRepoOperator: LocalRepoOperator = mockk()
    private val processOperator: ProcessOperator = mockk()
    private val notificationsOperator: NotificationsOperator = mockk(relaxed = true)
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
    private val passwordsOperator: PasswordsOperator = mockk {
        every { getPassword("username", "https://example") } returns "password"
    }
    private val cloneOperator = CloneOperator(
        filesOperator = filesOperator,
        projectOperator = projectOperator,
        prRouter = prRouter,
        localRepoOperator = localRepoOperator,
        processOperator = processOperator,
        notificationsOperator = notificationsOperator,
        uiProtector = uiProtector,
        settingsFileOperator = settingsFileOperator,
        passwordsOperator = passwordsOperator
    )

    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())

    companion object {

        private const val PROJECT = "mega-manipulator"
        private const val BASE_REPO = "base_repo"
    }

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
    fun `clone with search requests`() = runBlocking {
        // Given
        val input = SearchResult(searchHostName = "search", codeHostName = "code", project = "project", repo = "repo")
        coEvery { prRouter.getRepo(input) } returns mockk {
            every { getDefaultBranch() } returns "main"
            every { getCloneUrl(HTTPS) } returns "https://example.com"
        }
        coEvery { processOperator.runCommandAsync(any(), any()) } returns GlobalScope.async { ApplyOutput.dummy(exitCode = 0) }

        // When
        cloneOperator.clone(setOf(input))

        // Then
        verify { filesOperator.refreshClones() }
        verify {
            notificationsOperator.show(
                "Cloning done",
                "All 1 cloned successfully",
                NotificationType.INFORMATION
            )
        }
    }

    @Test
    fun `clone with pull request wrapper`() = runBlocking {
        // Given

        // When
        cloneOperator.clone(emptyList())

        // Then
        verify { filesOperator.refreshClones() }
        verify {
            notificationsOperator.show(
                "Cloning done",
                "All 0 cloned successfully",
                NotificationType.INFORMATION
            )
        }
    }

    @Test
    fun `clone repos failed`() = runBlocking {
        // Given
        val good = CompletableDeferred(ApplyOutput.dummy(exitCode = 0))
        val bad = CompletableDeferred(ApplyOutput.dummy(exitCode = 1))
        coEvery { processOperator.runCommandAsync(any(), any()) } returns bad andThen good
        val pullRequest: PullRequestWrapper = mockk(relaxed = true) {
            every { searchHostName() } returns "test"
            every { codeHostName() } returns "codeHostName"
            every { project() } returns PROJECT
            every { baseRepo() } returns BASE_REPO
            every { cloneUrlFrom() } returns "any-url-from"
            every { fromBranch() } returns "main"
            every { isFork() } returns false
        }

        // When
        cloneOperator.clone(listOf(pullRequest, pullRequest))

        // Then
        verify { filesOperator.refreshClones() }
        verify {
            notificationsOperator.show(
                "Cloning done with failures",
                "Failed cloning 1/2 repos, details in ide logs",
                NotificationType.WARNING
            )
        }
    }

    @Test
    fun `clone repos`() = runBlocking {
        // Given
        val pullRequest: PullRequestWrapper = mockk(relaxed = true) {
            every { searchHostName() } returns "test"
            every { codeHostName() } returns "codeHostName"
            every { project() } returns PROJECT
            every { baseRepo() } returns BASE_REPO
            every { cloneUrlFrom() } returns "any-url-from"
            every { fromBranch() } returns "main"
            every { isFork() } returns false
        }
        val applyOutput = ApplyOutput("anydir", "anystd", "", 0)

        coEvery { processOperator.runCommandAsync(any(), any()) } returns CompletableDeferred(applyOutput)

        // When
        cloneOperator.cloneRepos(pullRequest)

        // Then
        verify { processOperator.runCommandAsync(any(), any()) }
    }

    @Test
    fun `clone repos and promote origin to fork remote`() = runBlocking {
        // Given
        val pullRequest: PullRequestWrapper = mockk(relaxed = true) {
            every { searchHostName() } returns "test"
            every { codeHostName() } returns "codeHostName"
            every { project() } returns PROJECT
            every { baseRepo() } returns BASE_REPO
            every { cloneUrlFrom() } returns "any-url-from"
            every { fromBranch() } returns "main"
            every { cloneUrlTo() } returns "clone-url-to"
            every { isFork() } returns true
        }
        val applyOutput = ApplyOutput("anydir", "anystd", "", 0)

        every { processOperator.runCommandAsync(any(), any()) } returns CompletableDeferred(applyOutput)
        coEvery { localRepoOperator.promoteOriginToForkRemote(any(), any()) } returns emptyList()

        // When
        cloneOperator.cloneRepos(pullRequest)

        // Then
        verify { processOperator.runCommandAsync(any(), any()) }
        coVerify { localRepoOperator.promoteOriginToForkRemote(any(), "clone-url-to") }
        confirmVerified(localRepoOperator, processOperator)
    }

    @Test
    fun `clone repos failed on both attempts`() = runBlocking {
        // Given
        val pullRequest: PullRequestWrapper = mockk(relaxed = true) {
            every { searchHostName() } returns "test"
            every { codeHostName() } returns "codeHostName"
            every { project() } returns PROJECT
            every { baseRepo() } returns BASE_REPO
            every { cloneUrlFrom() } returns "any-url-from"
            every { fromBranch() } returns "main"
            every { isFork() } returns false
        }
        val applyOutput = ApplyOutput("anydir", "anystd", "", 1)

        every { processOperator.runCommandAsync(any(), any()) } returns CompletableDeferred(applyOutput)

        // When
        val states = cloneOperator.cloneRepos(pullRequest)

        // Then
        verify(exactly = 2) { processOperator.runCommandAsync(any(), any()) }
        assertThat(states.size, equalTo(2))
        assertThat(states[0].first, equalTo("Failed shallow clone attempt"))
        assertThat(states[1].first, equalTo("Failed full clone attempt"))
    }

    @Test
    fun `clone repos and failed to switch branch`() = runBlocking {
        // Given
        val pullRequest = mockk<PullRequestWrapper>()
        val file = mockFile(pullRequest)
        val applyOutput = ApplyOutput("anydir", "anystd", "", 1)
        val applyOutputCloneSuccess = ApplyOutput("anydir", "anystd", "", 0)

        val fullPath = "${project.basePath}/clones/${pullRequest.asPathString()}"

        every { file.mkdirs() } returns true
        every { file.exists() } returns false
        every { file.parentFile } returns file
        every { processOperator.runCommandAsync(any(), any()) } returns CompletableDeferred(applyOutput)
        every {
            processOperator.runCommandAsync(
                any(),
                listOf("git", "clone", "any-url-from", fullPath)
            )
        } returns CompletableDeferred(applyOutputCloneSuccess)

        // When
        val states = cloneOperator.cloneRepos(pullRequest)

        // Then
        verify(exactly = 4) { processOperator.runCommandAsync(any(), any()) }
        assertThat(states.size, equalTo(2))
        assertThat(states[0].first, equalTo("Failed shallow clone attempt"))
        assertThat(states[1].first, equalTo("Branch switch failed"))
    }

    @Test
    fun `clone existent repository`() = runBlocking {
        // Given
        val pullRequest = mockk<PullRequestWrapper>()
        mockFile(pullRequest)
        val fullPath = "${project.basePath}/clones/${pullRequest.asPathString()}"
        File(fullPath).mkdirs()
        File(fullPath, ".git").createNewFile()

        // When
        val states = cloneOperator.cloneRepos(pullRequest)

        // Then
        assertThat(states.size, equalTo(1))
        assertThat(states[0].first, equalTo("Repo already cloned"))
        confirmVerified(filesOperator)
    }

    private fun mockFile(pullRequest: PullRequestWrapper): File {
        val file = mockk<File>(relaxed = true)
        every { pullRequest.searchHostName() } returns "test"
        every { pullRequest.codeHostName() } returns "codeHostName"
        every { pullRequest.project() } returns PROJECT
        every { pullRequest.baseRepo() } returns BASE_REPO
        every { pullRequest.cloneUrlFrom() } returns "any-url-from"
        every { pullRequest.fromBranch() } returns "main"
        every { pullRequest.isFork() } returns false
        every { pullRequest.asPathString() } returns "${pullRequest.searchHostName()}/${pullRequest.codeHostName()}/$PROJECT/$BASE_REPO"
        return file
    }
}
