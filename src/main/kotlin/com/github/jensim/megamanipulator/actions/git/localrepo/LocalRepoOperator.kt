package com.github.jensim.megamanipulator.actions.git.localrepo

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

@SuppressWarnings("TooManyFunctions")
class LocalRepoOperator @NonInjectable constructor(
    private val project: Project,
    processOperator: ProcessOperator?,
    uiProtector: UiProtector?,
) {

    companion object {

        private const val depth = 4
    }
    constructor(project: Project) : this(project, null, null)
    private val processOperator: ProcessOperator by lazyService(project, processOperator)
    private val uiProtector: UiProtector by lazyService(project, uiProtector)

    fun getLocalRepoFiles(): List<File> {
        val clonesDir = File(project.basePath!!, "clones")
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
        val dir = File("${project.basePath!!}/clones/${searchResult.asPathString()}")
        return getBranch(dir)
    }

    suspend fun push(repoDir: File, force: Boolean): ApplyOutput {
        val branch = getBranch(repoDir)!!
        val upstream = if (hasFork(repoDir)) "fork" else "origin"
        val forceFlag: String? = if (force) "--force" else null
        return processOperator.runCommandAsync(repoDir, listOfNotNull("git", "push", forceFlag, "--set-upstream", upstream, branch)).await()
    }

    suspend fun getForkProject(repo: SearchResult): Pair<String, String>? {
        val url = getGitUrl(repo, "fork")
        val parts = url?.removeSuffix(".git")?.split("/", ":")?.takeLast(2)
        return try {
            val pair: Pair<String?, String?> = parts?.get(0) to parts?.get(1)
            return if (pair.first != null && pair.second != null) {
                pair as Pair<String, String>
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getGitUrl(repo: SearchResult, remote: String): String? {
        val dir = File("${project.basePath!!}/clones/${repo.asPathString()}")
        return try {
            processOperator.runCommandAsync(dir, listOf("git", "remote", "-v")).await().std.lines()
                .filter { it.startsWith(remote) && it.endsWith("(push)") }
                .map { it.split(" ", "\t").filter { it.isNotEmpty() }[1] }
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun hasFork(repoDir: File): Boolean =
        repo(repoDir)?.remoteNames?.contains("fork") ?: false

    suspend fun addForkRemote(repoDir: File, url: String): ApplyOutput {
        return processOperator.runCommandAsync(repoDir, listOf("git", "remote", "add", "fork", url)).await()
    }

    suspend fun promoteOriginToForkRemote(repoDir: File, originUrl: String): List<Pair<String, ApplyOutput>> {
        return listOf(
            "rename fork" to processOperator.runCommandAsync(repoDir, listOf("git", "remote", "rename", "origin", "fork")),
            "add origin" to processOperator.runCommandAsync(repoDir, listOf("git", "remote", "add", "origin", originUrl)),
        ).map { it.first to it.second.await() }
    }

    fun getBranch(repoDir: File): String? = repo(repoDir)?.branch

    private fun repo(repoDir: File): Repository? = try {
        val dir = File(repoDir, ".git")
        FileRepositoryBuilder().apply {
            gitDir = dir
        }.build()
    } catch (e: Exception) {
        null
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
