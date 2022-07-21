package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.GitUrlHelper
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.types.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.BitBucketSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettings
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import com.github.jensim.megamanipulator.ui.TestUiProtector
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.project.Project
import io.mockk.coEvery
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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
internal class CloneOperatorTest {

    private val project: Project = mockk()
    private val codeHostSettings: CodeHostSettings = BitBucketSettings(username = "username", cloneType = HTTPS, baseUrl = "https://example")
    private val settings = mockk<MegaManipulatorSettings> {
        every { resolveSettings(any(), any()) } returns (
                mockk<SearchHostSettings>() to codeHostSettings)
    }
    private val settingsFileOperator: SettingsFileOperator = mockk {
        every { readSettings() } returns settings
    }
    private val passwordsOperator: PasswordsOperator = TestPasswordOperator(
        mapOf(
            "username" to "https://example" to "password"
        )
    )
    private val gitUrlHelper = GitUrlHelper(project = project, passwordsOperator = passwordsOperator)
    private val prRouter: PrRouter = mockk()
    private val notificationsOperator: NotificationsOperator = mockk(relaxUnitFun = true)
    private val remoteCloneOperator: RemoteCloneOperator = mockk()
    private val localCloneOperator: LocalCloneOperator = mockk()
    private val uiProtector = TestUiProtector()
    private val filesOperator: FilesOperator = mockk(relaxUnitFun = true)
    private val cloneOperator = CloneOperator(
        project,
        remoteCloneOperator,
        localCloneOperator,
        settingsFileOperator,
        filesOperator,
        prRouter,
        notificationsOperator,
        uiProtector,
        gitUrlHelper,
    )

    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())

    @BeforeEach
    internal fun setUp() {
        every { project.basePath } returns tempDir.absolutePath
    }

    @AfterEach
    internal fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `clone with pull request wrapper`() = runBlocking {
        // Given

        // When
        cloneOperator.clone(pullRequests = emptyList(), sparseDef = null)

        // Then
        verify { filesOperator.refreshClones() }
        verify {
            notificationsOperator.show(
                "Cloning done",
                "All 0 cloned successfully",
                INFORMATION
            )
        }
    }

    @Test
    fun `clone with search requests`() = runBlocking {
        // Given
        val input = SearchResult(searchHostName = "search", codeHostName = "code", project = "project", repo = "repo")
        coEvery { prRouter.getRepo(input) } returns mockk {
            every { getDefaultBranch() } returns "main"
            every { getCloneUrl(HTTPS) } returns "https://example.com"
        }
        coEvery {
            remoteCloneOperator.clone(
                dir = File(tempDir, "clones/search/code/project/repo"),
                cloneUrl = "https://username:password@example.com",
                defaultBranch = "main",
                branch = "main",
                shallow = false,
                sparseDef = null
            )
        } returns listOf("Cloning" to ApplyOutput(dir = tempDir.absolutePath, std = "👍👌!!", 0))

        // When
        cloneOperator.clone(repos = setOf(input), branchName = "main", shallow = false, sparseDef = null)

        // Then
        verify { filesOperator.refreshClones() }
        verify {
            notificationsOperator.show(
                "Cloning done",
                "All 1 cloned successfully",
                INFORMATION
            )
        }
    }
}
