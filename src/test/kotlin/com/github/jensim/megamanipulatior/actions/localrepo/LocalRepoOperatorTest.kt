package com.github.jensim.megamanipulatior.actions.localrepo

import com.github.jensim.megamanipulatior.actions.ProcessOperator.runCommandAsync
import com.google.common.io.Files
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocalRepoOperatorTest {

    @Test
    fun `test get branch`() = runBlocking {
        val tempDir = Files.createTempDir()
        runCommandAsync(tempDir, listOf("git", "init")).await()
        runCommandAsync(tempDir, listOf("git", "checkout", "-b", "foo")).await()
        val actualBranch = LocalRepoOperator.getBranch(tempDir)
        assertEquals("foo", actualBranch)
    }
}
