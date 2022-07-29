package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.Action
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.OnErrorAction.SKIP

class LocalCloneOperator @NonInjectable constructor(
    private val project: Project,
    processOperator: ProcessOperator?,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(project: Project) : this(
        project = project,
        processOperator = null,
    )

    private val processOperator: ProcessOperator by lazyService(project, processOperator)

    /**
     * If the repo is configured for local copy over remote clone, try to find the repo locally,
     * and create a clone from that.
     */
    suspend fun copyIf(settings: CodeHostSettings, repo: SearchResult, defaultBranch: String, branch: String): CloneAttemptResult {
        val localRepoFile = File(project.basePath!!, "clones/${repo.asPathString()}/.git")
        if (localRepoFile.exists()) {
            return CloneAttemptResult(actions = listOf(Action("Copy", ApplyOutput(std = "Repo exists already", dir = repo.asPathString(), exitCode = 1))), success = true, repo = repo, branch = branch)
        }

        val keepRoot = settings.keepLocalRepos?.path ?: return CloneAttemptResult(actions = emptyList(), success = false, repo = repo, branch = branch)
        val keepLocalRepoFile = File("$keepRoot/${repo.project}/${repo.repo}", ".git")

        val history = mutableListOf<Action>()

        if (keepLocalRepoFile.exists()) {
            unshallowAndFetch(keepLocalRepoFile.parentFile, history)
            return try {
                keepLocalRepoFile.copyRecursively(localRepoFile) { file, exception ->
                    val message = "File copy failed: '${file.path}', due to ${exception.message}"
                    history.add(Action("File copy fail", ApplyOutput(std = message, dir = repo.asPathString(), exitCode = 1)))
                    logger.warn(message)
                    SKIP
                }
                checkout(defaultBranch, branch, keepLocalRepoFile.parentFile, history)
                history.add(Action("Restore saved repo", ApplyOutput(repo.asPathString(), "Done", 0)))

                CloneAttemptResult(actions = history, success = true, repo = repo, branch = branch)
            } catch (e: Exception) {
                val message = "Failed copying files from keep location due to ${e.message}"
                logger.warn(message, e)
                history.add(Action("Restore saved repo", ApplyOutput(repo.asPathString(), "$message, more info+stacktrace in logs", 0)))
                CloneAttemptResult(actions = history, success = false, repo = repo, branch = branch)
            }
        } else {
            return CloneAttemptResult(actions = history, success = false, repo = repo, branch = branch)
        }
    }

    private suspend fun checkout(defaultBranch: String, branch: String, repo: File, history: MutableList<Action>) {
        /*
         * TODO:
         *  * Delete local branch if exists..
         *  * Handle PR clone, where branch should exist
         */
        processOperator.runCommandAsync(repo, listOf("git", "checkout", "-f", defaultBranch)).await().let {
            history.add(Action("git checkout default branch '$defaultBranch'", it))
            if (it.exitCode == 0) {
                processOperator.runCommandAsync(repo, listOf("git", "reset", "--hard", "HEAD")).await().let {
                    history.add(Action("git reset default branch '$defaultBranch'", it))
                }
            }
        }
        if (defaultBranch != branch) {
            if (history.last().how.exitCode == 0) {
                val p3 = processOperator.runCommandAsync(repo, listOf("git", "checkout", "-f", branch)).await()
                history.add(Action("Switch branch", p3))
                if (p3.exitCode != 0) {
                    val p4 = processOperator.runCommandAsync(repo, listOf("git", "checkout", "-f", "-b", branch)).await()
                    history.add(Action("Create branch", p4))
                } else {
                    processOperator.runCommandAsync(repo, listOf("git", "reset", "--hard", "HEAD")).await().let {
                        history.add(Action("git reset branch '$branch'", it))
                    }
                }
            }
        }
    }

    /**
     * Save a copy of a clone after it has been pulled into the configured location
     */
    suspend fun saveCopy(settings: CodeHostSettings, repo: SearchResult, defaultBranch: String): CloneAttemptResult {
        val localRepoFile = File(project.basePath!!, "clones/${repo.asPathString()}/.git")
        if (!localRepoFile.exists()) {
            return CloneAttemptResult(actions = listOf(Action("Save Copy", ApplyOutput.dummy(std = "Repo doesn't exist", dir = repo.asPathString()))), success = false, repo = repo, branch = defaultBranch)
        }

        val keepRoot = settings.keepLocalRepos?.path ?: return CloneAttemptResult(actions = emptyList(), success = false, repo = repo, branch = defaultBranch)
        val keepLocalRepo = "$keepRoot/${repo.project}/${repo.repo}"
        val keepLocalRepoFile = File(keepLocalRepo, ".git")
        if (keepLocalRepoFile.exists()) {
            return CloneAttemptResult(actions = listOf(Action("Save copy", ApplyOutput.dummy(std = "Copy saved already, wont update", dir = repo.asPathString(), exitCode = 0))), success = true, repo = repo, branch = defaultBranch)
        }

        val history = mutableListOf<Action>()

        localRepoFile.copyRecursively(keepLocalRepoFile) { file, exception ->
            val message = "Save copy failed: '${file.path}', due to ${exception.message}"
            history.add(Action("File copy fail", ApplyOutput.dummy(std = message, dir = repo.asPathString())))
            logger.warn(message)
            SKIP
        }
        File(keepLocalRepoFile, "info/sparse-checkout").delete().let { deletedSparse ->
            history.add(Action("Delete sparse checkout settings", ApplyOutput(repo.asPathString(), "Deleted: $deletedSparse", 0)))
        }
        processOperator.runCommandAsync(keepLocalRepoFile.parentFile, listOf("git", "config", "core.sparseCheckout", "false")).await().let {
            history.add(Action("disable sparseCheckout for local save", it))
        }

        unshallowAndFetch(keepLocalRepoFile.parentFile, history)
        processOperator.runCommandAsync(keepLocalRepoFile.parentFile, listOf("git", "checkout", defaultBranch)).await().let {
            history.add(Action("git checkout $defaultBranch", it))
        }

        return CloneAttemptResult(actions = history, success = true, repo = repo, branch = defaultBranch)
    }

    private suspend fun unshallowAndFetch(dir: File, history: MutableList<Action>) {
        processOperator.runCommandAsync(dir, listOf("git", "fetch", "--unshallow")).await().let {
            history.add(Action("git fetch --unshallow", it))
        }
        processOperator.runCommandAsync(dir, listOf("git", "fetch", "--all")).await().let {
            history.add(Action("git fetch --all", it))
        }
    }
}
