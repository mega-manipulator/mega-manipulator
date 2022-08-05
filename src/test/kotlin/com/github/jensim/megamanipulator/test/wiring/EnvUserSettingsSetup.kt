package com.github.jensim.megamanipulator.test.wiring

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.BitBucketSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.codehost.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.codehost.GitLabSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.GithubSearchSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import com.github.jensim.megamanipulator.settings.types.searchhost.SourceGraphSettings
import com.github.jensim.megamanipulator.test.EnvHelper
import com.github.jensim.megamanipulator.test.Login
import com.github.jensim.megamanipulator.test.Password
import com.github.jensim.megamanipulator.test.TestPasswordOperator
import com.github.jensim.megamanipulator.test.Username
import org.slf4j.LoggerFactory

object EnvUserSettingsSetup {

    private val log = LoggerFactory.getLogger(javaClass)

    const val sourcegraphName = "sourcegraph.com"
    const val githubName = "github.com"

    val helper = EnvHelper()

    val passwordsOperator: PasswordsOperator by lazy {
        val sourceGraphLogin: Pair<Login, Password> = sourceGraphSettings.username to sourceGraphSettings.baseUrl to helper.resolve(EnvHelper.EnvProperty.SRC_COM_ACCESS_TOKEN)!!
        val gitHubSearchLogin: Pair<Login, Password> = githubSearchSettings.username to githubSearchSettings.baseUrl to helper.resolve(EnvHelper.EnvProperty.GITHUB_TOKEN)!!
        val githubLogin: Pair<Login, Password>? = toLoginAndPass(githubSettings, EnvHelper.EnvProperty.GITHUB_TOKEN)
        val gitLabLogin: Pair<Login, Password>? = toLoginAndPass(gitlabSettings, EnvHelper.EnvProperty.GITLAB_TOKEN)
        val bitBucketLogin: Pair<Login, Password>? = toLoginAndPass(bitbucketSettings, EnvHelper.EnvProperty.BITBUCKET_SERVER_TOKEN)
        TestPasswordOperator(
            listOfNotNull(sourceGraphLogin, gitHubSearchLogin, githubLogin, gitLabLogin, bitBucketLogin).toMap()
        )
    }

    private fun toLoginAndPass(settingsNode: Pair<String, CodeHostSettingsGroup>?, passwordProperty: EnvHelper.EnvProperty): Pair<Login, Password>? {
        return settingsNode?.second?.value()?.username?.let { username: Username ->
            val baseUrl = settingsNode.second.value().baseUrl
            helper.resolve(passwordProperty)?.let { password: Password ->
                username to baseUrl to password
            }
        }
    }

    val searchResults: Array<SearchResult> by lazy {
        listOfNotNull(
            SearchResult(
                searchHostName = sourcegraphName,
                codeHostName = githubName,
                project = helper.resolve(EnvHelper.EnvProperty.GITHUB_PROJECT)!!,
                repo = helper.resolve(EnvHelper.EnvProperty.GITHUB_REPO)!!,
            ),
            SearchResult(
                searchHostName = githubName,
                codeHostName = githubName,
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
        ).toTypedArray()
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
        githubName to CodeHostSettingsGroup(
            gitHub = GitHubSettings(
                username = helper.resolve(EnvHelper.EnvProperty.GITHUB_USERNAME)!!,
                forkSetting = ForkSetting.LAZY_FORK,
                cloneType = CloneType.HTTPS,
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
                    baseUrl = helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_BASEURL)!!,
                    httpsOverride = HttpsOverride.ALLOW_ANYTHING,
                    username = helper.resolve(EnvHelper.EnvProperty.BITBUCKET_SERVER_USER)!!,
                    forkSetting = ForkSetting.LAZY_FORK,
                    cloneType = CloneType.HTTPS,
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
    val sourceGraphSettings: SourceGraphSettings by lazy {
        SourceGraphSettings(
            baseUrl = "https://sourcegraph.com",
            codeHostSettings = codeHostSettings,
        )
    }
    val githubSearchSettings: GithubSearchSettings by lazy {
        GithubSearchSettings(
            username = helper.resolve(EnvHelper.EnvProperty.GITHUB_USERNAME)!!,
            cloneType = CloneType.HTTPS,
        )
    }
    val settings: MegaManipulatorSettings by lazy {
        MegaManipulatorSettings(
            concurrency = 1,
            searchHostSettings = mapOf(
                sourcegraphName to SearchHostSettingsGroup(sourceGraph = sourceGraphSettings),
                githubName to SearchHostSettingsGroup(gitHub = githubSearchSettings),
            )
        )
    }

    private class MissingPropertyException(service: String, missing: List<EnvHelper.EnvProperty>) : Exception("$service missing properties ${missing.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.name }}")

    private fun List<EnvHelper.EnvProperty>.verifyUnset(service: String) = this.filter { helper.resolve(it) == null }.let { if (it.isNotEmpty()) throw MissingPropertyException(service, it) }
}
