package com.github.jensim.megamanipulator.test.wiring

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import org.slf4j.LoggerFactory

object EnvUserSettingsSetup {

    private val log = LoggerFactory.getLogger(javaClass)

    const val sourcegraphName = "sourcegraph.com"
    val helper = EnvHelper()

    val passwordsOperator: PasswordsOperator by lazy {
        TestPasswordOperator(
            listOfNotNull(
                sourceGraphSettings.username to sourceGraphSettings.baseUrl to helper.resolve(EnvHelper.EnvProperty.SRC_COM_ACCESS_TOKEN)!!,
                githubSettings.second.username to githubSettings.second.baseUrl to helper.resolve(EnvHelper.EnvProperty.GITHUB_TOKEN)!!,
                gitlabSettings?.second?.let { it.username to it.baseUrl to helper.resolve(EnvHelper.EnvProperty.GITLAB_TOKEN)!! },
                bitbucketSettings?.second?.let { it.username to it.baseUrl to helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_TOKEN)!! },
            ).toMap()
        )
    }

    val searchResults: List<SearchResult> by lazy {
        listOfNotNull(
            SearchResult(
                searchHostName = sourcegraphName,
                codeHostName = githubSettings.first,
                project = helper.resolve(EnvHelper.EnvProperty.GITHUB_PROJECT)!!,
                repo = helper.resolve(EnvHelper.EnvProperty.GITHUB_REPO)!!,
            ),
            bitbucketSettings?.first?.let {
                SearchResult(
                    searchHostName = sourcegraphName,
                    codeHostName = it,
                    project = helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_PROJECT)!!,
                    repo = helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_REPO)!!,
                )
            },
            gitlabSettings?.first?.let {
                SearchResult(
                    searchHostName = sourcegraphName,
                    codeHostName = it,
                    project = helper.resolve(EnvHelper.EnvProperty.GITLAB_GROUP)!!,
                    repo = helper.resolve(EnvHelper.EnvProperty.GITLAB_PROJECT)!!
                )
            },
        )
    }

    private val codeHostSettings: Map<String, CodeHostSettings> by lazy {
        listOfNotNull(
            githubSettings,
            bitbucketSettings,
            gitlabSettings,
        ).toMap()
    }

    val githubSettings: Pair<String, CodeHostSettings.GitHubSettings> by lazy {
        "github.com" to CodeHostSettings.GitHubSettings(
            username = helper.resolve(EnvHelper.EnvProperty.GITHUB_USERNAME)!!,
            forkSetting = ForkSetting.EAGER_FORK,
            cloneType = CloneType.HTTPS,
        )
    }

    val bitbucketSettings: Pair<String, CodeHostSettings.BitBucketSettings>? by lazy {
        try {
            helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_PROJECT)!!
            helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_REPO)!!
            "bitbucket_server" to CodeHostSettings.BitBucketSettings(
                baseUrl = helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_BASEURL)!!,
                httpsOverride = HttpsOverride.ALLOW_ANYTHING,
                username = helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_USER)!!,
                forkSetting = ForkSetting.EAGER_FORK,
                cloneType = CloneType.HTTPS,
            )
        } catch (e: NullPointerException) {
            log.warn("BitBucket user settings not set up", e)
            null
        }
    }

    val gitlabSettings: Pair<String, CodeHostSettings.GitLabSettings>? by lazy {
        try {
            helper.resolve(EnvHelper.EnvProperty.GITLAB_GROUP)!!
            helper.resolve(EnvHelper.EnvProperty.GITLAB_PROJECT)!!
            "gitlab.com" to CodeHostSettings.GitLabSettings(
                username = helper.resolve(EnvHelper.EnvProperty.GITLAB_USERNAME)!!,
                forkSetting = ForkSetting.EAGER_FORK,
                cloneType = CloneType.HTTPS
            )
        } catch (e: NullPointerException) {
            log.warn("GitLab user settings not set up", e)
            null
        }
    }
    val sourceGraphSettings: SearchHostSettings.SourceGraphSettings by lazy {
        SearchHostSettings.SourceGraphSettings(
            baseUrl = "https://sourcegraph.com",
            codeHostSettings = codeHostSettings,
        )
    }
    val settings: MegaManipulatorSettings by lazy {
        MegaManipulatorSettings(
            concurrency = 1,
            searchHostSettings = mapOf(
                sourcegraphName to sourceGraphSettings
            )
        )
    }
}
