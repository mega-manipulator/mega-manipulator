package com.github.jensim.megamanipulator.settings.types.searchhost

import com.github.jensim.megamanipulator.settings.types.AuthMethod.JUST_TOKEN
import com.github.jensim.megamanipulator.settings.types.AuthMethod.USERNAME_TOKEN
import com.github.jensim.megamanipulator.test.wiring.EnvUserSettingsSetup
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

internal class SourceGraphSettingsTest {

    private val codeHostSettings = listOf(EnvUserSettingsSetup.githubSettings).toMap()

    @Test
    internal fun `test getAuthHeaderValue token`() {
        val settings = SourceGraphSettings(username = "jensim", authMethod = JUST_TOKEN, codeHostSettings = codeHostSettings)

        val headerValue = settings.getAuthHeaderValue("foo")

        assertThat(headerValue, equalTo("token foo"))
    }

    @Test
    internal fun `test getAuthHeaderValue username`() {
        val settings = SourceGraphSettings(username = "jensim", authMethod = USERNAME_TOKEN, codeHostSettings = codeHostSettings)

        val headerValue = settings.getAuthHeaderValue("foo")

        assertThat(headerValue, equalTo("Basic amVuc2ltOmZvbw=="))
    }
}
