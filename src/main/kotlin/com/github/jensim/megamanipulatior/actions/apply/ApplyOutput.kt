package com.github.jensim.megamanipulatior.actions.apply

data class ApplyOutput(
    val dir: String,
    val std: String,
    val err: String,
    val exitCode: Int,
) {
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
