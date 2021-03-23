package com.github.jensim.megamanipulatior.actions.localrepo

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.apply.ApplyOutput
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

object LocalRepoOperator {

    private const val depth = 4

    fun getLocalRepoFiles(): List<File> {
        val clonesDir = File(project.basePath, "clones")
        if (!clonesDir.exists()) {
            return emptyList()
        }
        val clonesPath = clonesDir.toPath()

        return Files.find(clonesDir.toPath(), depth, { path: Path, _: BasicFileAttributes -> isLikelyCloneDir(clonesPath, path) })
            .map { it.toFile() }
            .collect(Collectors.toList())
    }

    fun getLocalRepos(): List<SearchResult> = getLocalRepoFiles().map {
        SearchResult(
            repo = it.name,
            project = it.parentFile.name,
            codeHostName = it.parentFile.parentFile.name,
            searchHostName = it.parentFile.parentFile.parentFile.name
        )
    }

    fun getLocalRepositories(): List<Repository> = getLocalRepoFiles().map { gitDir ->
        FileRepositoryBuilder.create(gitDir)
    }

    fun getBranch(searchResult: SearchResult): String? {
        val dir = File("${project.basePath}/clones/${searchResult.searchHostName}/${searchResult.codeHostName}/${searchResult.project}/${searchResult.repo}")
        return getBranch(dir)
    }

    suspend fun push(repoDir: File): ApplyOutput {
        val branch = getBranch(repoDir)!!
        return if (hasFork(repoDir)) {
            ProcessOperator.runCommandAsync(repoDir, listOf("git", "push", "--set-upstream", "fork", branch)).await()
        } else {
            ProcessOperator.runCommandAsync(repoDir, listOf("git", "push", "--set-upstream", "origin", branch)).await()
        }
    }

    fun hasFork(repoDir: File): Boolean {
        val dir = File(repoDir, ".git")
        return try {
            FileRepositoryBuilder().apply {
                gitDir = dir
            }.build().remoteNames.contains("fork")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addForkRemote(repoDir: File, url: String): ApplyOutput {
        return ProcessOperator.runCommandAsync(repoDir, listOf("git", "remote", "add", "fork", url)).await()
    }

    fun getBranch(repoDir: File): String? {
        val dir = File(repoDir, ".git")
        return try {
            FileRepositoryBuilder().apply {
                gitDir = dir
            }.build().branch
        } catch (e: Exception) {
            null
        }
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
