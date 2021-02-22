package com.github.jensim.megamanipulatior.actions.localrepo

import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

object LocalRepoOperator {

    private const val depth = 4

    fun getLocalRepos(): List<File> {
        val clonesDir = File(project.basePath, "clones")
        val clonesPath = clonesDir.toPath()

        return Files.find(clonesDir.toPath(), depth, { path, attr -> isLikelyCloneDir(clonesPath, path) })
            .map { it.toFile() }
            .collect(Collectors.toList())
    }

    private fun isLikelyCloneDir(clonesPath: Path, evalPath: Path): Boolean {
        val evalFile = evalPath.toFile()
        val gitDir = File(evalFile, ".git")
        if (gitDir.exists()) {
            val relativePath = evalFile.absolutePath.removePrefix(clonesPath.toFile().absolutePath)
            if (relativePath.count { it == File.separatorChar } == depth) {
                return true
            }
        }
        return false
    }
}
