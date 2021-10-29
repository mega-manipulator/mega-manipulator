package com.github.jensim.megamanipulator.actions.apply

import kotlinx.serialization.Serializable

@Serializable
data class ApplyOutput(
    val dir: String,
    val std: String,
    val exitCode: Int,
) {

    companion object {
        fun dummy(
            dir: String = "",
            std: String = "",
            exitCode: Int = 1,
        ): ApplyOutput = ApplyOutput(
            dir = dir,
            std = std,
            exitCode = exitCode,
        )
    }

    val lastLine: String = try {
        std.lines().reversed().first { it.isNotEmpty() }
    } catch (e: NoSuchElementException) {
        ""
    }

    override fun toString(): String = dir
    fun getFullDescription() = """DIR: $dir
EXIT_CODE: $exitCode
=== OUTPUT ===
$std"""
}
