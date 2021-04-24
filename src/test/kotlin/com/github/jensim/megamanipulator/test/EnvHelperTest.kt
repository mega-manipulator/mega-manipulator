package com.github.jensim.megamanipulator.test

import com.github.jensim.megamanipulator.test.EnvHelper.EnvProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.util.Properties

class EnvHelperTest {

    @Test
    internal fun `example file contains all values`() {
        val file = File(".env-example")
        val prop = Properties()
        FileInputStream(file).use { prop.load(it) }
        val fileKeys: Set<String> = prop.keys as Set<String>
        val enumKeys: Set<String> = EnvProperty.values().map { it.name }.toSet()
        assertEquals(fileKeys, enumKeys)
    }
}
