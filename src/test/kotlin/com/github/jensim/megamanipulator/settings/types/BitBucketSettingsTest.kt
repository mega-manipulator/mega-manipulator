package com.github.jensim.megamanipulator.settings.types

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class BitBucketSettingsTest {

    @Test
    internal fun name() {
        // given
        val settings = CodeHostSettings.BitBucketSettings(
            baseUrl = "https://foo.bar",
            username = "foo",
        )

        // when
        val base64Auth = settings.getAuthHeaderValue("bar")

        Assertions.assertEquals(base64Auth, "Basic Zm9vOmJhcg==")
    }
}
