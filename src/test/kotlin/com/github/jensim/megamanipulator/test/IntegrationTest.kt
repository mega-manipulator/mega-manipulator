package com.github.jensim.megamanipulator.test

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.toolswindow.MyToolWindowFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
class IntegrationTest : TestApplicationWiring() {

    private val contentFactoryMock: ContentFactory = mockk(relaxed = true)
    private val toolWindowMock: ToolWindow = mockk(relaxed = true)

    @Test
    internal fun `everything lazy`() {
        val myToolWindowFactory = MyToolWindowFactory(applicationWiring) { contentFactoryMock }

        myToolWindowFactory.createToolWindowContent(mockProject, toolWindowMock)
    }

    @Test
    internal fun `run naive scenario`() {
        // clone
        val result = SearchResult(
            searchHostName = searchHostName,
            codeHostName = codeHostName,
            project = githubUsername,
            repo = "mega-manipulator"
        )
        val results: Set<SearchResult> = setOf(result)
        applicationWiring.cloneOperator.clone(results)
        assertTrue(File(tempDir, "clones/${result.asPathString()}/.git").exists())

        // branch
        val branch = "integration_test/${UUID.randomUUID()}"
        applicationWiring.localRepoOperator.switchBranch(branch)

        // apply
        val confDir = File(tempDir, "config")
        confDir.mkdirs()
        val scriptFile = File(confDir, "mega-manipulator.bash")
        scriptFile.createNewFile()
        scriptFile.writeText(
            """
            #!/bin/bash
            echo "${"$"}RANDOM" >> README.md
            """.trimIndent()
        )
        every { settingsFileOperator.scriptFile } returns scriptFile

        val applyResult: List<ApplyOutput> = applicationWiring.applyOperator.apply()
        assertThat(applyResult, hasSize(1))
        assertThat(applyResult, everyItem(hasProperty("exitCode", equalTo(0))))

        // commit
        every {
            dialogGenerator.askForInput(
                eq("Create commits for all changes in all checked out repositories"),
                eq("Commit message"),
            )
        } returns "Integration test of branch $branch"
        every {
            dialogGenerator.showConfirm(
                eq("Also push?"),
                eq("Also push? Integration test of branch $branch"),
            )
        } returns true

        val commitResults: Map<String, List<Pair<String, ApplyOutput>>> = applicationWiring.commitOperator.commit()
        assertThat(commitResults.keys, hasSize(1))
        val commitResult: List<Pair<String, ApplyOutput>> = commitResults.values.first()
        val exitCode: Int = commitResult.last().second.exitCode
        assertThat(
            "Last exit code was not zero for ${commitResult.last().first}\n${commitResult.last().second.getFullDescription()}",
            exitCode,
            equalTo(0)
        )

        // pr
        val newPr = runBlocking { applicationWiring.prRouter.createPr(branch, "Don't mind me, im an integration test", result) }
        assertNotNull(newPr)
        println("PR Created: $newPr")

        // reword
        val updatedPR = runBlocking {
            applicationWiring.prRouter.commentPR("Woops! I need to update the PR!", newPr!!)
            applicationWiring.prRouter.updatePr("Updated: $branch", "#UPDATED! :-D\n----\n\n${newPr.body()}", newPr)
        }
        assertNotNull(updatedPR)
        println("PR Updated: $updatedPR")

        // decline
        runBlocking {
            applicationWiring.prRouter.closePr(dropFork = true, dropBranch = true, pullRequest = updatedPR!!)
        }
    }
}
