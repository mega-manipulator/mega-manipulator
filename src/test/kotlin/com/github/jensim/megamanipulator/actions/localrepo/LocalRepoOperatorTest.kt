package com.github.jensim.megamanipulator.actions.localrepo

import com.github.jensim.megamanipulator.actions.ProcessOperator.runCommandAsync
import com.google.common.io.Files
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class LocalRepoOperatorTest {

    private lateinit var tempDir: File

    @BeforeEach
    internal fun setUp() {
        tempDir = Files.createTempDir()
    }

    @AfterEach
    internal fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test get branch`() = runBlocking {
        runCommandAsync(tempDir, listOf("git", "init")).await()
        runCommandAsync(tempDir, listOf("git", "checkout", "-b", "foo")).await()
        val actualBranch = LocalRepoOperator.getBranch(tempDir)
        assertEquals("foo", actualBranch)
    }
}
