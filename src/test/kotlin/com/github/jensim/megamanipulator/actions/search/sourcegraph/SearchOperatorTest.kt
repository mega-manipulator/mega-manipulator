package com.github.jensim.megamanipulator.actions.search.sourcegraph

import com.github.jensim.megamanipulator.actions.search.SearchOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.search.github.GitHubSearchClient
import com.github.jensim.megamanipulator.actions.search.hound.HoundClient
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.ForkSetting.PLAIN_BRANCH
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.codehost.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.searchhost.SourceGraphSettings
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.SRC_COM_USERNAME
import com.intellij.openapi.project.Project
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SearchOperatorTest {

    private val project: Project = mockk()
    private val settingsFileOperatorMock: SettingsFileOperator = mockk()
    private val sourcegraphSearchClientMock: SourcegraphSearchClient = mockk()
    private val houndClientMock: HoundClient = mockk()
    private val gitHubSearchClientMock: GitHubSearchClient = mockk()
    private val searchOperator = SearchOperator(
        project = project,
        settingsFileOperator = settingsFileOperatorMock,
        sourcegraphSearchClient = sourcegraphSearchClientMock,
        houndClient = houndClientMock,
        gitHubSearchClient = gitHubSearchClientMock,
    )

    private val envHelper = EnvHelper()

    private val codeHostName = "github.com"
    private val sourceGraphSettings = SourceGraphSettings(
        baseUrl = "https://sourcegraph.com",
        codeHostSettings = mapOf(
            codeHostName to CodeHostSettingsGroup(
                gitHub = GitHubSettings(
                    username = envHelper.resolve(SRC_COM_USERNAME)!!,
                    forkSetting = PLAIN_BRANCH,
                )
            )
        )
    )
    private val searchHostName = "sourcegraph.com"
    private val settings = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            searchHostName to SearchHostSettingsGroup(sourceGraph = sourceGraphSettings)
        )
    )

    @Test
    fun search() = runBlocking {
        // Given
        every { settingsFileOperatorMock.readSettings() } returns settings
        coEvery { sourcegraphSearchClientMock.search(any(), any(), any()) } returns emptySet()

        // When
        val sourceGraphSettings = searchOperator.search(searchHostName, "Dockerfile")

        // Then
        assertEquals(sourceGraphSettings, emptySet<SearchResult>())
        verify { settingsFileOperatorMock.readSettings() }
        coVerify { sourcegraphSearchClientMock.search(any(), any(), any()) }
    }

    @Test
    fun searchNotFound() = runBlocking {
        // Given
        every { settingsFileOperatorMock.readSettings() } returns settings

        // When
        val nullPointerException = assertThrows<NullPointerException> {
            searchOperator.search(
                "any-hostname",
                "Dockerfile"
            )
        }

        // Then
        assertThat(nullPointerException.localizedMessage, equalTo("No settings for search host named any-hostname"))
        verify { settingsFileOperatorMock.readSettings() }
        coVerify(exactly = 0) { sourcegraphSearchClientMock.search(any(), any(), any()) }
    }
}
