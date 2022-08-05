package com.github.jensim.megamanipulator.settings.types

import io.ktor.client.plugins.logging.LogLevel
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

internal class HttpLoggingLevelTest {
    @Test
    internal fun `all match to ktor`() {
        assertThat(
            HttpLoggingLevel.values().map { it.name }.sorted(),
            equalTo(LogLevel.values().map { it.name }.sorted())
        )
    }
}
