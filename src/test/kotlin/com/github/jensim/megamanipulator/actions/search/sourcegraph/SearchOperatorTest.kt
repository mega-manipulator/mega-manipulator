package com.github.jensim.megamanipulator.actions.search.sourcegraph

import com.github.jensim.megamanipulator.actions.search.SearchOperator
import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.ForkSetting
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.SearchHostSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class SearchOperatorTest {

    @MockK
    private lateinit var settingsFileOperator: SettingsFileOperator

    @MockK
    private lateinit var sourcegraphSearchClient: SourcegraphSearchClient

    @InjectMockKs
    private lateinit var searchOperator: SearchOperator

    private val codeHostName = "github.com"
    private val sourceGraphSettings = SearchHostSettings.SourceGraphSettings(
        baseUrl = "https://sourcegraph.com",
        codeHostSettings = mapOf(
            codeHostName to CodeHostSettings.GitHubSettings(
                username = "jensim",
                forkSetting = ForkSetting.PLAIN_BRANCH,
            )
        )
    )
    private val searchHostName = "sourcegraph.com"
    private val settings = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            searchHostName to sourceGraphSettings
        )
    )

    @Test
    fun search() = runBlocking {
        //Given
        every { settingsFileOperator.readSettings() } returns settings
        coEvery { sourcegraphSearchClient.search(any(), any(), any()) } returns emptySet()

        //When
        val sourceGraphSettings = searchOperator.search(searchHostName, "Dockerfile")

        //Then
        assertEquals(sourceGraphSettings, emptySet())
        verify { settingsFileOperator.readSettings() }
        coVerify { sourcegraphSearchClient.search(any(), any(), any()) }

    }

    @Test
    fun searchNotFound() = runBlocking {
        //Given
        every { settingsFileOperator.readSettings() } returns settings

        //When
        val nullPointerException = assertThrows<NullPointerException> {
            searchOperator.search(
                "any-hostname",
                "Dockerfile"
            )
        }

        //Then
        assertThat(nullPointerException.localizedMessage, equalTo("No settings for search host named any-hostname"))
        verify { settingsFileOperator.readSettings() }
        coVerify { sourcegraphSearchClient.search(any(), any(), any()) wasNot Called }
    }
}
