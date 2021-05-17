package com.github.jensim.megamanipulator.actions.vcs.bitbucketserver

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.types.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.BitBucketSettings
import com.github.jensim.megamanipulator.settings.types.ForkSetting.PLAIN_BRANCH
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings.SourceGraphSettings
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.BITBUCKET_SERVER_BASEURL
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.BITBUCKET_SERVER_TOKEN
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.BITBUCKET_SERVER_USER
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf

@EnabledIf("com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitbucketServerClientTest#enabled", disabledReason = "We don't have a CI installation of bitbucket server")
internal class BitbucketServerClientTest {

    companion object {

        private val envHelper = EnvHelper()

        @JvmStatic
        fun enabled(): Boolean = try {
            envHelper.resolve(BITBUCKET_SERVER_BASEURL)
            true
        } catch (e: NullPointerException) {
            false
        }
    }

    private val bitBucketSettings = BitBucketSettings(
        username = envHelper.resolve(BITBUCKET_SERVER_USER),
        forkSetting = PLAIN_BRANCH,
        baseUrl = envHelper.resolve(BITBUCKET_SERVER_BASEURL),
        cloneType = HTTPS,
    )
    private val password = envHelper.resolve(BITBUCKET_SERVER_TOKEN)
    private val codeHost = "bitbucket_server"
    private val searchHost = "sourcegraph.com"
    private val settings = MegaManipulatorSettings(
        searchHostSettings = mapOf(
            searchHost to SourceGraphSettings(
                baseUrl = "https://sourcegraph.com",
                codeHostSettings = mapOf(codeHost to bitBucketSettings)
            )
        )
    )
    private val settingsMock: SettingsFileOperator = mockk {
        every { readSettings() } returns settings
    }
    private val passwordsOperator = TestPasswordOperator(mapOf(bitBucketSettings.username to bitBucketSettings.baseUrl to password))
    private val notificationsMock: NotificationsOperator = mockk()
    private val clientProvider = HttpClientProvider(
        settingsFileOperator = settingsMock,
        passwordsOperator = passwordsOperator,
        notificationsOperator = notificationsMock
    )
    private val localRepoMock: LocalRepoOperator = mockk()
    private val client = BitbucketServerClient(
        httpClientProvider = clientProvider,
        localRepoOperator = localRepoMock,
        json = SerializationHolder.instance.readableJson,
        notificationsOperator = notificationsMock
    )

    @Test
    fun validateAccess() {
        // given

        // when
        val response = runBlocking {
            client.validateAccess(searchHost, codeHost, bitBucketSettings)
        }

        // then
        assertThat(response, equalTo("200:OK"))
    }
}
