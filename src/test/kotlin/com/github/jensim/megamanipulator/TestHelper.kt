package com.github.jensim.megamanipulator

import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitHubSettings.Companion.GITHUB_TOKEN
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitHubSettings.Companion.GITHUB_USERNAME
import com.github.jensim.megamanipulator.settings.ForkSetting
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

class TestHelper {

    companion object {
        fun getGithubCredentials(): CodeHostSettings.GitHubSettings {
            val prop = Properties()
            return try {
                val fis = FileInputStream(".env")
                prop.load(fis)
                val username = prop.getProperty(GITHUB_USERNAME)
                val token = prop.getProperty(GITHUB_TOKEN)
                    ?: throw RuntimeException("To run the test you must to provide a github token")
                System.setProperty("GITHUB_TOKEN", token)
                CodeHostSettings.GitHubSettings(
                    username = username,
                    forkSetting = ForkSetting.PLAIN_BRANCH,
                )
            } catch (e: FileNotFoundException) {
                val env = System.getenv()
                val username = env[GITHUB_USERNAME]
                env[GITHUB_TOKEN]
                    ?: throw RuntimeException("To run the test you must to provide a github token")
                CodeHostSettings.GitHubSettings(
                    username = username ?: throw RuntimeException("To run this test you must to provide a username"),
                    forkSetting = ForkSetting.PLAIN_BRANCH,
                )
            }
        }
    }
}