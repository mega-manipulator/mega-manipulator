package com.github.jensim.megamanipulator.test

object CiDetector {

    // TODO improve the ci detection
    val isCI: Boolean by lazy {
        val osName = System.getProperty("os.name")
        osName.toLowerCase().startsWith("linux")
    }
}
