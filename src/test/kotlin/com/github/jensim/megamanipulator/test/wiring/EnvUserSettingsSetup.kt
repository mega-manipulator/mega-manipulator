package com.github.jensim.megamanipulator.test.wiring

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.ForkSetting.LAZY_FORK
import com.github.jensim.megamanipulator.settings.types.HttpsOverride.ALLOW_ANYTHING
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.BitBucketSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.codehost.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.codehost.GitLabSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.searchhost.SourceGraphSettings
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.BITBUCKET_SERVER_BASEURL
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.BITBUCKET_SERVER_USER
import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty.GITHUB_USERNAME
import com.github.jensim.megamanipulator.test.Login
import com.github.jensim.megamanipulator.test.Password
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import org.slf4j.LoggerFactory

object EnvUserSettingsSetup {

    private val log = LoggerFactory.getLogger(javaClass)

    const val sourcegraphName = "sourcegraph.com"
    val helper = EnvHelper()

    val passwordsOperator: PasswordsOperator by lazy {
        val githubLogin: Pair<Login, Password> = (githubSettings.second.value.username!! to githubSettings.second.value.baseUrl) to helper.resolve(EnvHelper.EnvProperty.GITHUB_TOKEN)!!
        val sourceGraphLogin: Pair<Login, Password> = sourceGraphSettings.username to sourceGraphSettings.baseUrl to helper.resolve(EnvHelper.EnvProperty.SRC_COM_ACCESS_TOKEN)!!
        val gitLabLogin: Pair<Login, Password> = gitlabSettings?.second?.value!!.let { it.username!! to it.baseUrl } to helper.resolve(EnvHelper.EnvProperty.GITLAB_TOKEN)!!
        val bitBucketLogin: Pair<Login, Password> = bitbucketSettings?.second?.value!!.let { it.username!! to it.baseUrl to helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_TOKEN)!! }
        TestPasswordOperator(
            listOfNotNull(sourceGraphLogin, githubLogin, gitLabLogin, bitBucketLogin).toMap()
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

    private val codeHostSettings: Map<String, CodeHostSettingsGroup> by lazy {
        listOfNotNull(
            githubSettings,
            bitbucketSettings,
            gitlabSettings,
        ).toMap()
    }

    val githubSettings: Pair<String, CodeHostSettingsGroup> by lazy {
        listOf(
            EnvHelper.EnvProperty.GITHUB_USERNAME,
            EnvHelper.EnvProperty.GITHUB_TOKEN,
            EnvHelper.EnvProperty.GITHUB_REPO,
            EnvHelper.EnvProperty.GITHUB_PROJECT,
        ).verifyUnset("GitHub")
        "github.com" to CodeHostSettingsGroup(
            gitHub = GitHubSettings(
                username = helper.resolve(GITHUB_USERNAME)!!,
                forkSetting = LAZY_FORK,
                cloneType = HTTPS,
            )
        )
    }

    private val bitbucketSettings: Pair<String, CodeHostSettingsGroup>? by lazy {
        try {
            listOf(
                EnvHelper.EnvProperty.BITBUCKET_SERVER_PROJECT,
                EnvHelper.EnvProperty.BITBUCKET_SERVER_REPO,
                EnvHelper.EnvProperty.BITBUCKET_SERVER_BASEURL,
                EnvHelper.EnvProperty.BITBUCKET_SERVER_USER,
            ).verifyUnset("BitBucket")
            "bitbucket_server" to CodeHostSettingsGroup(
                bitBucket = BitBucketSettings(
                    baseUrl = helper.resolve(BITBUCKET_SERVER_BASEURL)!!,
                    httpsOverride = ALLOW_ANYTHING,
                    username = helper.resolve(BITBUCKET_SERVER_USER)!!,
                    forkSetting = LAZY_FORK,
                    cloneType = HTTPS,
                )
            )
        } catch (e: MissingPropertyException) {
            log.warn(e.message)
            null
        }
    }

    val gitlabSettings: Pair<String, CodeHostSettingsGroup>? by lazy {
        try {
            listOf(
                EnvHelper.EnvProperty.GITLAB_USERNAME,
                EnvHelper.EnvProperty.GITLAB_TOKEN,
                EnvHelper.EnvProperty.GITLAB_PROJECT,
                EnvHelper.EnvProperty.GITLAB_GROUP,
            ).verifyUnset("GitLab")
            "gitlab.com" to CodeHostSettingsGroup(
                gitLab = GitLabSettings(
                    username = helper.resolve(EnvHelper.EnvProperty.GITLAB_USERNAME)!!,
                    forkSetting = ForkSetting.LAZY_FORK,
                    cloneType = CloneType.HTTPS,
                )
            )
        } catch (e: MissingPropertyException) {
            log.warn(e.message)
            null
        }
    }
    private val sourceGraphSettings: SourceGraphSettings by lazy {
        SourceGraphSettings(
            baseUrl = "https://sourcegraph.com",
            codeHostSettings = codeHostSettings,
        )
    }
    val settings: MegaManipulatorSettings by lazy {
        MegaManipulatorSettings(
            concurrency = 1,
            searchHostSettings = mapOf(
                sourcegraphName to SearchHostSettingsGroup(sourceGraph = sourceGraphSettings)
            )
        )
    }

    private class MissingPropertyException(service: String, missing: List<EnvHelper.EnvProperty>) : Exception("$service missing properties ${missing.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.name }}")

    private fun List<EnvHelper.EnvProperty>.verifyUnset(service: String) = this.filter { helper.resolve(it) == null }.let { if (it.isNotEmpty()) throw MissingPropertyException(service, it) }
}
