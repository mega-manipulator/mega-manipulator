package com.github.jensim.megamanipulator.actions.localrepo

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.ui.UiProtector
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

@SuppressWarnings("TooManyFunctions")
class LocalRepoOperator(
    private val projectOperator: ProjectOperator,
    private val processOperator: ProcessOperator,
    private val uiProtector: UiProtector,
) {

    companion object {

        private const val depth = 4
    }

    fun getLocalRepoFiles(): List<File> {
        val clonesDir = File(projectOperator.project.basePath!!, "clones")
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

    fun switchBranch(branch: String) {
        val localRepoFiles = getLocalRepoFiles()
        uiProtector.mapConcurrentWithProgress(title = "Checkout branch $branch", data = localRepoFiles) { dir ->
            switchBranch(dir, branch)
        }
    }

    private suspend fun switchBranch(repoDir: File, branch: String) {
        processOperator.runCommandAsync(repoDir, listOf("git", "checkout", "-b", branch)).await()
    }

    fun getBranch(searchResult: SearchResult): String? {
        val dir = File("${projectOperator.project.basePath!!}/clones/${searchResult.searchHostName}/${searchResult.codeHostName}/${searchResult.project}/${searchResult.repo}")
        return getBranch(dir)
    }

    suspend fun push(repoDir: File): ApplyOutput {
        val branch = getBranch(repoDir)!!
        return if (hasFork(repoDir)) {
            processOperator.runCommandAsync(repoDir, listOf("git", "push", "--set-upstream", "fork", branch)).await()
        } else {
            processOperator.runCommandAsync(repoDir, listOf("git", "push", "--set-upstream", "origin", branch)).await()
        }
    }

    suspend fun getForkProject(repo: SearchResult): Pair<String, String>? {
        val url = getGitUrl(repo, "fork")
        val parts = url?.removeSuffix(".git")?.split("/", ":")?.takeLast(2)
        val pair: Pair<String?, String?> = parts?.get(0) to parts?.get(1)
        return if (pair.first != null && pair.second != null) {
            pair as Pair<String, String>
        } else {
            null
        }
    }

    private suspend fun getGitUrl(repo: SearchResult, remote: String): String? {
        val dir = File("${projectOperator.project.basePath!!}/clones/${repo.searchHostName}/${repo.codeHostName}/${repo.project}/${repo.repo}")
        return try {
            processOperator.runCommandAsync(dir, listOf("git", "remote", "-v")).await().std.lines()
                .filter { it.startsWith(remote) && it.endsWith("(push)") }
                .map { it.split(" ", "\t").filter { it.isNotEmpty() }[1] }
                .firstOrNull()
        } catch (e: Exception) {
            null
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
        return processOperator.runCommandAsync(repoDir, listOf("git", "remote", "add", "fork", url)).await()
    }

    suspend fun promoteOriginToForkRemote(repoDir: File, originUrl: String): List<Pair<String, ApplyOutput>> {
        return listOf(
            "rename fork" to processOperator.runCommandAsync(repoDir, listOf("git", "remote", "rename", "origin", "fork")),
            "add origin" to processOperator.runCommandAsync(repoDir, listOf("git", "remote", "add", "origin", originUrl)),
        ).map { it.first to it.second.await() }
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
