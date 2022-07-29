package com.github.jensim.megamanipulator.settings.types

import java.nio.file.Files
import kotlin.io.path.Path

data class KeepLocalRepos(
    val path: String,
) {
    init {
        Path(path).let {
            if (!it.isAbsolute) {
                throw IllegalArgumentException("KeepLocalRepos-path is not absolute: '$path'")
            }
            if (!Files.exists(it)) {
                Files.createDirectories(it)
            }
        }
    }
}
