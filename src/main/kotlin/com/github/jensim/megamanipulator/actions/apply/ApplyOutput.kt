package com.github.jensim.megamanipulator.actions.apply

import com.fasterxml.jackson.annotation.JsonIgnore

data class ApplyOutput(
    val dir: String,
    val std: String,
    val exitCode: Int,
    val command: String? = null,
) {

    companion object {
        fun dummy(
            command: String? = null,
            dir: String = "",
            std: String = "",
            exitCode: Int = 1,
        ): ApplyOutput = ApplyOutput(
            command = command,
            dir = dir,
            std = std,
            exitCode = exitCode,
        )
    }

    @JsonIgnore
    val lastLine: String = try {
        std.lines().reversed().first { it.isNotEmpty() }
    } catch (e: NoSuchElementException) {
        ""
    }

    override fun toString(): String = dir
    @JsonIgnore
    fun getFullDescription() = """DIR: $dir
${command?.let {
        """COMMAND: $it
"""
    } ?: ""}EXIT_CODE: $exitCode
=== OUTPUT ===
$std"""
}
