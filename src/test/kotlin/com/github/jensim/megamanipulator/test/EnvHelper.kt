package com.github.jensim.megamanipulator.test

import java.io.File
import java.io.FileInputStream
import java.util.Properties

class EnvHelper {

    enum class EnvProperty(val defaultValue: String?) {
        GITHUB_USERNAME("jensim"),
        SRC_COM_ACCESS_TOKEN(null),
        GITHUB_TOKEN(null),
    }

    private val dotEnvProperties: Properties by lazy {
        val file = File(".env")
        val prop = Properties()
        if (file.exists()) {
            FileInputStream(file).use { prop.load(it) }
        }
        prop
    }

    fun resolve(env: EnvProperty): String = dotEnvProperties[env.name]?.toString() ?: System.getenv()[env.name] ?: env.defaultValue!!
}
