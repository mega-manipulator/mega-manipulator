package com.github.jensim.megamanipulator.test

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrActionStatus
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.test.wiring.EnvUserSettingsSetup
import com.github.jensim.megamanipulator.test.wiring.TestApplicationWiring
import com.github.jensim.megamanipulator.toolswindow.MyToolWindowFactory
import com.intellij.notification.NotificationType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import com.intellij.util.io.delete
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
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
        wiring.applicationWiring.cloneOperator.clone(results, "main")
        verify { wiring.applicationWiring.notificationsOperator.show(any(), any(), eq(NotificationType.INFORMATION)) }
        val dotGitDit = File(wiring.tempDir, "clones/${result.asPathString()}/.git")
        assertTrue(dotGitDit.exists())
        val repoDir = dotGitDit.parentFile

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
        val commitMessage = "Integration test of branch $branch"
        val commitResults = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        runBlocking {
            wiring.applicationWiring.commitOperator.commitProcess(repoDir, commitResults, commitMessage, true, wiring.settings)
        }
        assertThat(commitResults.keys, hasSize(1))
        val commitResult: List<Pair<String, ApplyOutput>> = commitResults.values.first()
        val exitCode: Int = commitResult.last().second.exitCode
        assertThat(
            "Of all the steps ${commitResult.map { it.first }}, the last exit code was not zero for ${commitResult.last().first}\n${commitResult.last().second.getFullDescription()}",
            exitCode,
            equalTo(0)
        )

        // pr
        val newPr = runBlocking {
            wiring.applicationWiring.prRouter.createPr(
                branch,
                "Don't mind me, im an integration test",
                result
            )
        }
        assertNotNull(newPr)
        println("PR Created: $newPr")

        // reword
        val newTitle = "Updated: $branch"
        val status: PrActionStatus = runBlocking {
            wiring.applicationWiring.prRouter.commentPR("Woops! I need to update the PR!", newPr!!)
            wiring.applicationWiring.prRouter.updatePr(
                newTitle = newTitle,
                newDescription = "#UPDATED! :-D\n----\n\n${newPr.body()}",
                pullRequest = newPr
            )
        }
        assertTrue(status.success)

        val updatedPR: PullRequestWrapper = org.awaitility.Awaitility.await("Finding PR")
            .atMost(10, TimeUnit.SECONDS).until({
                // Must fetch the updated PR in order to decline it
                runBlocking {
                    wiring.applicationWiring.prRouter.getAllAuthorPrs(newPr!!.searchHostName(), newPr.codeHostName())
                }?.firstOrNull { it.title() == newTitle }
            }) { it != null }!!

        val closeStatus: PrActionStatus = runBlocking {
            try {
                wiring.applicationWiring.prRouter.closePr(dropFork = true, dropBranch = true, pullRequest = updatedPR)
            } catch (e: Exception) {
                e.printStackTrace()
                fail { "Failed closing PR" }
            }
        }
        assertTrue(closeStatus.success, "Failed closing PR with msg: ${closeStatus.msg}")
    }
}
