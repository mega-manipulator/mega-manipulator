package com.github.jensim.megamanipulator.actions.localrepo

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
class LocalRepoOperatorTest {

    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())
    private val project: Project = mock {
        on { basePath } doReturn tempDir.absolutePath
    }
    private val projectOperator: ProjectOperator = mock {
        on { project } doReturn project
    }
    private val processOperator = ProcessOperator(projectOperator)
    private val localRepoOperator = LocalRepoOperator(projectOperator, processOperator)

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
}
