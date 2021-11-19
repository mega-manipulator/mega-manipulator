package com.github.jensim.megamanipulator.actions

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

class ProcessOperatorImplTest {

    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())
    private val projectMock: Project = mockk {
        every { basePath } returns tempDir.absolutePath
    }
    private val target = ProcessOperatorImpl(projectMock)

    @Test
    fun `Run JQ`() = runBlocking {
        val result: ApplyOutput = target.runCommandAsync(tempDir, listOf("jq", "-r", "-n", "--argjson", "data", """{"key":"value"}""", "\$data.key")).await()

        assertThat(result.std, equalTo("value\n"))
    }

    @Test
    fun `run script file`() = runBlocking {
        val fileName = "script.bash"
        val scriptFile = File(tempDir, fileName)
        scriptFile.createNewFile()
        scriptFile.writeText(
            """
            #!/bin/bash
            set -e
            jq -r -n --argjson data '{"key":"value1"}' '${'$'}data.key' >&2
            jq -r -n --argjson data '{"key":"value2"}' '${'$'}data.key'
            """.trimIndent()
        )

        val result = target.runCommandAsync(tempDir, listOf("/bin/bash", scriptFile.absolutePath)).await()

        assertThat(result.exitCode, equalTo(0))
        assertThat(result.std, equalTo("value1\nvalue2\n"))
    }
}
