package com.github.jensim.megamanipulator.actions.localrepo

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.settings.passwords.ProjectOperator
import com.github.jensim.megamanipulator.ui.TestUiProtector
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.either
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
@ExtendWith(MockKExtension::class)
class LocalRepoOperatorTest {

    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())

    private val project: Project = mockk()
    private val projectOperator: ProjectOperator = mockk()

    private lateinit var processOperator: ProcessOperator
    private lateinit var localRepoOperator: LocalRepoOperator
    private val uiProtector = TestUiProtector()
    private val successOutput = ApplyOutput("any", "any", "any", 0)

    @BeforeEach
    internal fun setUp() {
        every { project.basePath } returns tempDir.absolutePath
        every { projectOperator.project } returns project

        processOperator = ProcessOperator(project, projectOperator)
        localRepoOperator = LocalRepoOperator(project, projectOperator, processOperator, uiProtector)
    }

    @AfterEach
    internal fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test get branch`() = runBlocking {
        processOperator.runCommandAsync(tempDir, listOf("git", "init")).await()
        processOperator.runCommandAsync(tempDir, listOf("git", "checkout", "-b", "foo")).await()
        val actualBranch = localRepoOperator.getBranch(tempDir)
        assertEquals("foo", actualBranch)
    }

    @Test
    fun `get branch`() = runBlocking {
        processOperator.runCommandAsync(tempDir, listOf("mkdir", "clones")).await()
        processOperator.runCommandAsync(
            File(tempDir, "clones"),
            listOf("mkdir", "-p", "projectGit1/depth1/depth2/depth3")
        ).await()
        processOperator.runCommandAsync(File(tempDir, "clones/projectGit1/depth1/depth2/depth3"), listOf("git", "init"))
            .await()

        val searchResult = localRepoOperator.getLocalRepos()[0]
        val branch = localRepoOperator.getBranch(searchResult)
        assertThat(branch, either(equalTo("master")).or(equalTo("main")))
    }

    @Test
    fun `get local repo files`() = runBlocking {
        createGitProjects()
        processOperator.runCommandAsync(File(tempDir, "clones"), listOf("mkdir", "projectNotGit")).await()

        val files = localRepoOperator.getLocalRepoFiles()
        assertThat(files.size, equalTo(2))
        assertThat(
            files.map { it.absolutePath },
            containsInAnyOrder(
                endsWith("projectGit1/depth1/depth2/depth3"),
                endsWith("projectGit2/depth1/depth2/depth3"),
            )
        )
    }

    @Test
    fun `get local repo files empty when clones not exists`() = runBlocking {
        val files = localRepoOperator.getLocalRepoFiles()
        assertTrue(files.isEmpty())
    }

    @Test
    fun `get local repos`() = runBlocking {
        createGitProjects()
        processOperator.runCommandAsync(File(tempDir, "clones"), listOf("mkdir", "projectNotGit")).await()

        val searchResults = localRepoOperator.getLocalRepos()
        assertThat(searchResults.size, equalTo(2))
        assertThat(searchResults.map { it.searchHostName }, containsInAnyOrder("projectGit1", "projectGit2"))
        assertThat(searchResults.map { it.codeHostName }, everyItem(equalTo("depth1")))
        assertThat(searchResults.map { it.project }, everyItem(equalTo("depth2")))
        assertThat(searchResults.map { it.repo }, everyItem(equalTo("depth3")))
    }

    @Test
    fun `push origin`() = runBlocking {
        val processOperatorMock: ProcessOperator = mockk(relaxed = true)
        val listSlot = slot<List<String>>()
        localRepoOperator = LocalRepoOperator(project, projectOperator, processOperatorMock, uiProtector)
        processOperator.runCommandAsync(tempDir, listOf("git", "init")).await()
        processOperator.runCommandAsync(tempDir, listOf("git", "checkout", "-b", "foo")).await()

        every { processOperatorMock.runCommandAsync(tempDir, capture(listSlot)) } returns CompletableDeferred(
            successOutput
        )
        localRepoOperator.push(tempDir)
        val list = listSlot.captured
        assertThat(list[3], equalTo("origin"))
    }

    @Test
    fun `push fork`() = runBlocking {
        val processOperatorMock: ProcessOperator = mockk(relaxed = true)
        val listSlot = slot<List<String>>()
        localRepoOperator = LocalRepoOperator(project, projectOperator, processOperatorMock, uiProtector)
        processOperator.runCommandAsync(tempDir, listOf("git", "init")).await()
        processOperator.runCommandAsync(tempDir, listOf("git", "checkout", "-b", "foo")).await()
        processOperator.runCommandAsync(
            tempDir,
            listOf("git", "remote", "add", "fork", "git@github.com:foo/mega-manipulator.git")
        ).await()

        every { processOperatorMock.runCommandAsync(tempDir, capture(listSlot)) } returns CompletableDeferred(
            successOutput
        )
        localRepoOperator.push(tempDir)
        val list = listSlot.captured
        assertThat(list[3], equalTo("fork"))
    }

    @Test
    fun `get fork project`() = runBlocking {
        processOperator.runCommandAsync(tempDir, listOf("mkdir", "clones")).await()
        processOperator.runCommandAsync(
            File(tempDir, "clones"),
            listOf("mkdir", "-p", "projectGit1/depth1/depth2/depth3")
        ).await()
        processOperator.runCommandAsync(File(tempDir, "clones/projectGit1/depth1/depth2/depth3"), listOf("git", "init"))
            .await()
        localRepoOperator.addForkRemote(
            File(tempDir, "clones/projectGit1/depth1/depth2/depth3"),
            "git@github.com:foo/mega-manipulator.git"
        )

        val fork = localRepoOperator.getForkProject(localRepoOperator.getLocalRepos()[0])
        assertThat(fork!!.first, equalTo("foo"))
        assertThat(fork.second, equalTo("mega-manipulator"))
    }

    @Test
    fun `get fork project exception`() = runBlocking {
        processOperator.runCommandAsync(tempDir, listOf("mkdir", "clones")).await()
        processOperator.runCommandAsync(
            File(tempDir, "clones"),
            listOf("mkdir", "-p", "projectGit1/depth1/depth2/depth3")
        ).await()
        processOperator.runCommandAsync(File(tempDir, "clones/projectGit1/depth1/depth2/depth3"), listOf("git", "init"))
            .await()
        localRepoOperator.addForkRemote(
            File(tempDir, "clones/projectGit1/depth1/depth2/depth3"),
            "git@github.comfoomega-manipulator.git"
        )

        val fork = localRepoOperator.getForkProject(localRepoOperator.getLocalRepos()[0])
        assertThat(fork, nullValue())
    }

    @Test
    fun `promote origin to fork remote`() = runBlocking {
        processOperator.runCommandAsync(tempDir, listOf("mkdir", "clones")).await()
        processOperator.runCommandAsync(File(tempDir, "clones"), listOf("mkdir", "-p", "projectGit1/depth1/depth2/depth3")).await()
        processOperator.runCommandAsync(File(tempDir, "clones/projectGit1/depth1/depth2/depth3"), listOf("git", "init")).await()
        localRepoOperator.addForkRemote(File(tempDir, "clones/projectGit1/depth1/depth2/depth3"), "git@github.com:foo/mega-manipulator.git")
        localRepoOperator.promoteOriginToForkRemote(File(tempDir, "clones/projectGit1/depth1/depth2/depth3"), "git@github.com:bar/mega-manipulator.git")
        val response = processOperator.runCommandAsync(
            File(tempDir, "clones/projectGit1/depth1/depth2/depth3"),
            listOf("git", "remote", "-v")
        ).await()

        assertThat(
            response.std,
            containsString(
                "fork\tgit@github.com:foo/mega-manipulator.git (fetch)\n" +
                    "fork\tgit@github.com:foo/mega-manipulator.git (push)\n" +
                    "origin\tgit@github.com:bar/mega-manipulator.git (fetch)\n" +
                    "origin\tgit@github.com:bar/mega-manipulator.git (push)"
            )
        )
    }

    @Test
    fun `switch branch`() = runBlocking {
        createGitProjects()
        localRepoOperator.switchBranch("test")
        val response = processOperator.runCommandAsync(
            File(tempDir, "clones/projectGit1/depth1/depth2/depth3"),
            listOf("git", "status")
        ).await()
        assertThat(response.std, containsString("On branch test"))
        val responseProject2 = processOperator.runCommandAsync(
            File(tempDir, "clones/projectGit2/depth1/depth2/depth3"),
            listOf("git", "status")
        ).await()
        assertThat(responseProject2.std, containsString("On branch test"))
    }

    private suspend fun createGitProjects() {
        processOperator.runCommandAsync(tempDir, listOf("mkdir", "clones")).await()
        processOperator.runCommandAsync(
            File(tempDir, "clones"),
            listOf("mkdir", "-p", "projectGit1/depth1/depth2/depth3")
        ).await()
        processOperator.runCommandAsync(File(tempDir, "clones/projectGit1/depth1/depth2/depth3"), listOf("git", "init"))
            .await()
        processOperator.runCommandAsync(
            File(tempDir, "clones"),
            listOf("mkdir", "-p", "projectGit2/depth1/depth2/depth3")
        ).await()
        processOperator.runCommandAsync(File(tempDir, "clones/projectGit2/depth1/depth2/depth3"), listOf("git", "init"))
            .await()
    }
}
