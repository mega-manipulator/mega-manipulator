package com.github.jensim.megamanipulator.test

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.test.wiring.EnvUserSettingsSetup
import com.github.jensim.megamanipulator.test.wiring.TestApplicationWiring
import com.github.jensim.megamanipulator.toolswindow.MyToolWindowFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import com.intellij.util.io.delete
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
class IntegrationTest {

    companion object {

        @JvmStatic
        val searchResults: Array<SearchResult> = EnvUserSettingsSetup.searchResults.toTypedArray()
    }

    private val wiring = TestApplicationWiring()
    private val contentFactoryMock: ContentFactory = mockk(relaxed = true)
    private val toolWindowMock: ToolWindow = mockk(relaxed = true)

    @AfterEach
    internal fun tearDown() {
        wiring.tempDirPath.delete(true)
    }

    @BeforeEach
    internal fun setUp() {
        every { wiring.mockProject.basePath } returns wiring.tempDir.absolutePath
        every { wiring.projectOperator.project } returns wiring.mockProject
    }

    @Test
    internal fun `everything lazy`() {
        val myToolWindowFactory = MyToolWindowFactory(wiring.applicationWiring) { contentFactoryMock }

        myToolWindowFactory.createToolWindowContent(wiring.mockProject, toolWindowMock)
    }

    @ParameterizedTest
    @MethodSource(value = ["getSearchResults"])
    @SuppressWarnings("LongMethod")
    fun `run naive scenarios`(result: SearchResult) {
        // clone
        val results: Set<SearchResult> = setOf(result)
        wiring.applicationWiring.cloneOperator.clone(results)
        assertTrue(File(wiring.tempDir, "clones/${result.asPathString()}/.git").exists())

        // branch
        val branch = "integration_test/${UUID.randomUUID()}"
        wiring.applicationWiring.localRepoOperator.switchBranch(branch)

        // apply
        val confDir = File(wiring.tempDir, "config")
        confDir.mkdirs()
        val scriptFile = File(confDir, "mega-manipulator.bash")
        scriptFile.createNewFile()
        scriptFile.writeText(
            """
            #!/bin/bash
            echo "${"$"}RANDOM" >> README.md
            """.trimIndent()
        )
        every { wiring.settingsFileOperator.scriptFile } returns scriptFile

        val applyResult: List<ApplyOutput> = wiring.applicationWiring.applyOperator.apply()
        assertThat(applyResult, hasSize(1))
        assertThat(applyResult, everyItem(hasProperty("exitCode", equalTo(0))))

        // commit
        every {
            wiring.dialogGenerator.askForInput(
                eq("Create commits for all changes in all checked out repositories"),
                eq("Commit message"),
            )
        } returns "Integration test of branch $branch"
        every {
            wiring.dialogGenerator.showConfirm(
                eq("Also push?"),
                eq("Also push? Integration test of branch $branch"),
            )
        } returns true

        val commitResults: Map<String, List<Pair<String, ApplyOutput>>> = wiring.applicationWiring.commitOperator.commit()
        assertThat(commitResults.keys, hasSize(1))
        val commitResult: List<Pair<String, ApplyOutput>> = commitResults.values.first()
        val exitCode: Int = commitResult.last().second.exitCode
        assertThat(
            "Last exit code was not zero for ${commitResult.last().first}\n${commitResult.last().second.getFullDescription()}",
            exitCode,
            equalTo(0)
        )

        // pr
        val newPr = runBlocking { wiring.applicationWiring.prRouter.createPr(branch, "Don't mind me, im an integration test", result) }
        assertNotNull(newPr)
        println("PR Created: $newPr")

        // reword
        val updatedPR = runBlocking {
            wiring.applicationWiring.prRouter.commentPR("Woops! I need to update the PR!", newPr!!)
            wiring.applicationWiring.prRouter.updatePr("Updated: $branch", "#UPDATED! :-D\n----\n\n${newPr.body()}", newPr)
        }
        assertNotNull(updatedPR)
        println("PR Updated: $updatedPR")

        // decline
        runBlocking {
            try {
                wiring.applicationWiring.prRouter.closePr(dropFork = true, dropBranch = true, pullRequest = updatedPR!!)
            } catch (e: Exception) {
                e.printStackTrace()
                fail { "Failed closing PR" }
            }
        }
    }
}
