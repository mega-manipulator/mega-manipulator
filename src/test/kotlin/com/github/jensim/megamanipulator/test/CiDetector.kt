package com.github.jensim.megamanipulator.test

import java.io.File

object CiDetector {

    val isCI: Boolean by lazy {
        val osName = System.getProperty("os.name")
        osName.toLowerCase().startsWith("linux") && File("/.dockerenv").exists()
    }
}
