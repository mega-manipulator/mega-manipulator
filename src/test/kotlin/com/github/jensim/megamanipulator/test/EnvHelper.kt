package com.github.jensim.megamanipulator.test

import java.io.File
import java.io.FileInputStream
import java.util.Properties

class EnvHelper {

    enum class EnvProperty(val defaultValue: String? = null, val fallBackEnv: List<String> = emptyList()) {
        GITHUB_USERNAME(fallBackEnv = listOf("GITHUB_ACTOR"), defaultValue = "jensim"),
        SRC_COM_ACCESS_TOKEN(fallBackEnv = listOf("SRC_ACCESS_TOKEN")),
        GITHUB_TOKEN,
        BITBUCKET_SERVER_BASEURL(fallBackEnv = listOf("BITBUCKET_BASEURL")),
        BITBUCKET_SERVER_USER(fallBackEnv = listOf("BITBUCKET_USER")),
        BITBUCKET_SERVER_TOKEN(fallBackEnv = listOf("BITBUCKET_TOKEN"))
    }

    private val dotEnvProperties: Properties by lazy {
        val file = File(".env")
        val prop = Properties()
        if (file.exists()) {
            FileInputStream(file).use { prop.load(it) }
        }
        prop
    }

    fun resolve(env: EnvProperty): String {
        return dotEnvProperties[env.name]?.toString()
            ?: resolveSysEnv(env)
            ?: env.defaultValue!!
    }

    private fun resolveSysEnv(env: EnvProperty): String? {
        val sysEnv = System.getenv()
        return sysEnv[env.name] ?: env.fallBackEnv.mapNotNull { sysEnv[it] }.firstOrNull()
    }
}
