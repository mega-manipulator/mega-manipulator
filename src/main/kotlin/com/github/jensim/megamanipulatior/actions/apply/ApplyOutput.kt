package com.github.jensim.megamanipulatior.actions.apply

data class ApplyOutput(
    val dir: String,
    val std: String,
    val err: String,
    val exitCode: Int,
) {

    companion object {
        fun dummy(
            dir: String = "",
            std: String = "",
            err: String = "",
            exitCode: Int = 1,
        ): ApplyOutput = ApplyOutput(
            dir = dir,
            std = std,
            err = err,
            exitCode = exitCode,
        )
    }

    override fun toString(): String = dir
    fun getFullDescription() = """
DIR: $dir
EXIT_CODE: $exitCode
=== STD_OUT ===
$std
===============
=== STD_ERR ===
$err
===============
""".trimIndent()
}
