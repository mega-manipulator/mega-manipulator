package com.github.jensim.megamanipulator.settings.types.searchhost

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

internal class GithubSearchSettingsTest {
    @Test
    internal fun `test getAuthHeaderValue`() {
        val settings = GithubSearchSettings("jensim")

        val headerValue = settings.getAuthHeaderValue("foo")

        assertThat(headerValue, equalTo("token foo"))
    }
}
