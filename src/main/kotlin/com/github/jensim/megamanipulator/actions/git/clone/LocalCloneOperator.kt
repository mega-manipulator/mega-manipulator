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
    sparseConfigSetupOperator: SparseConfigSetupOperator?,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(project: Project) : this(
        project = project,
        processOperator = null,
        sparseConfigSetupOperator = null,
    )

    private val processOperator: ProcessOperator by lazyService(project, processOperator)
    private val sparseConfigSetupOperator: SparseConfigSetupOperator by lazyService(project, sparseConfigSetupOperator)

    /**
     * If the repo is configured for local copy over remote clone, try to find the repo locally,
     * and create a clone from that.
     */
    suspend fun copyIf(settings: CodeHostSettings, repo: SearchResult, defaultBranch: String, branch: String, sparseDef: String?): CloneAttemptResult {
        val localRepoFile = File(project.basePath!!, "clones/${repo.asPathString()}/.git")
        if (localRepoFile.exists()) {
            return CloneAttemptResult(
                actions = listOf(
                    Action(
                        "Copy",
                        ApplyOutput(
                            std = "Repo exists already",
                            dir = repo.asPathString(),
                            exitCode = 0,
                        )
                    )
                ),
                success = true, repo = repo, branch = branch
            )
        }

        val keepRoot = settings.keepLocalRepos?.path ?: return CloneAttemptResult(actions = emptyList(), success = false, repo = repo, branch = branch)
        val keepLocalRepoFile = File("$keepRoot/${repo.project}/${repo.repo}", ".git")

        val history = mutableListOf<Action>()

        if (keepLocalRepoFile.exists()) {
            unshallowAndFetch(keepLocalRepoFile.parentFile, history)
            return try {
                keepLocalRepoFile.copyRecursively(localRepoFile) { file, exception ->
                    val message = "File copy failed: '${file.path}', due to ${exception.message}"
                    history.add(
                        Action(
                            "File copy fail",
                            ApplyOutput(
                                std = message,
                                dir = repo.asPathString(),
                                exitCode = 1,
                            )
                        )
                    )
                    logger.warn(message)
                    SKIP
                }
                history.addAll(sparseConfigSetupOperator.setupSparseDef(sparseDef, localRepoFile.parentFile))
                checkout(defaultBranch, branch, localRepoFile.parentFile, history)
                history.add(
                    Action(
                        "Restore saved repo",
                        ApplyOutput(
                            dir = repo.asPathString(),
                            std = "Done",
                            exitCode = 0,
                        )
                    )
                )

                CloneAttemptResult(actions = history, success = true, repo = repo, branch = branch)
            } catch (e: Exception) {
                val message = "Failed copying files from keep location due to ${e.message}"
                logger.warn(message, e)
                history.add(
                    Action(
                        "Restore saved repo",
                        ApplyOutput(
                            dir = repo.asPathString(),
                            std = "$message, more info+stacktrace in logs",
                            exitCode = 0,
                        )
                    )
                )
                CloneAttemptResult(actions = history, success = false, repo = repo, branch = branch)
            }
        } else {
            return CloneAttemptResult(actions = history, success = false, repo = repo, branch = branch)
        }
    }

    private suspend fun checkout(defaultBranch: String, branch: String, repo: File, history: MutableList<Action>) {
        processOperator.runCommandAsync(repo, listOf("git", "checkout", "-f", defaultBranch)).await().let {
            history.add(Action("git checkout '$defaultBranch' (default) branch '$defaultBranch'", it))
        }
        processOperator.runCommandAsync(repo, listOf("git", "reset", "--hard", "origin/$defaultBranch")).await().let {
            history.add(Action("git reset '$defaultBranch' (default) branch to origin", it))
        }
        if (defaultBranch != branch) {
            if (history.last().how.exitCode == 0) {
                val p3 = processOperator.runCommandAsync(repo, listOf("git", "checkout", branch)).await()
                history.add(Action("Checkout existing branch '$branch'", p3))
                if (p3.exitCode == 0) {
                    processOperator.runCommandAsync(repo, listOf("git", "reset", "--hard", "origin/$branch")).await().let {
                        history.add(Action("git reset '$branch' to origin", it))
                    }
                } else {
                    val p4 = processOperator.runCommandAsync(repo, listOf("git", "checkout", "-b", branch)).await()
                    history.add(Action("Create branch '$branch'", p4))
                }
            }
        }
    }

    /**
     * Save a copy of a clone after it has been pulled into the configured location
     */
    suspend fun saveCopy(settings: CodeHostSettings, repo: SearchResult, defaultBranch: String, sparseDef: String?): CloneAttemptResult {
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
        sparseConfigSetupOperator.setupSparseDef(sparseDef, localRepoFile.parentFile)

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
