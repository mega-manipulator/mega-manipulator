package com.github.jensim.megamanipulator.actions.localrepo

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.settings.passwords.ProjectOperator
import com.github.jensim.megamanipulator.ui.TestUiProtector
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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

    @MockK
    private lateinit var project: Project

    @MockK
    private lateinit var projectOperator: ProjectOperator

    private lateinit var processOperator: ProcessOperator
    private lateinit var localRepoOperator: LocalRepoOperator
    private val uiProtector = TestUiProtector()

    @BeforeEach
    internal fun setUp() {
        every { project.basePath } returns tempDir.absolutePath
        every { projectOperator.project } returns project

        processOperator = ProcessOperator(projectOperator)
        localRepoOperator = LocalRepoOperator(projectOperator, processOperator, uiProtector)
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
}
