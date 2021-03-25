package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.mapConcurrentWithProgress
import com.github.jensim.megamanipulator.ui.trimProjectPath
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.WARNING
import java.io.File

object CloneOperator {

    fun clone(branch: String, repos: Set<SearchResult>) {

        val settings = SettingsFileOperator.readSettings()!!
        val basePath = ProjectOperator.project?.basePath!!
        val noConf = mutableListOf<SearchResult>()
        repos.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() }
        ) { repo ->
            settings.resolveSettings(repo.searchHostName, repo.codeHostName)?.let { (_, codeHostSettings) ->
                val cloneUrl = codeHostSettings.cloneUrl(repo.project, repo.repo)
                val dir = File(basePath, "clones/${repo.asPathString()}")
                clone(dir, cloneUrl, branch)
            } ?: noConf.add(repo)
        }
        FilesOperator.refreshClones()
        if (noConf.isEmpty()) {
            println("All done")
        } else {
            println("All done, no conf found for $noConf")
        }
    }

    suspend fun clone(pullRequest: PullRequestWrapper) {
        val basePath = ProjectOperator.project?.basePath!!
        val fullPath = "$basePath/clones/${pullRequest.searchHostName()}/${pullRequest.codeHostName()}/${pullRequest.project()}/${pullRequest.repo()}"
        val dir = File(fullPath)
        clone(dir, pullRequest.cloneUrlTo()!!, pullRequest.fromBranch())
        if (pullRequest.isFork()) {
            LocalRepoOperator.addForkRemote(dir, pullRequest.cloneUrlFrom()!!)
        }
    }

    private suspend fun clone(dir: File, cloneUrl: String, branch: String) {
        dir.mkdirs()
        if (File(dir, ".git").exists()) {
            NotificationsOperator.show(
                    title = "Repo already cloned",
                    body = "Repo ${dir.trimProjectPath()} already cloned.\nWill not do anything.",
                    type = WARNING
            )
        } else {
            val p1 = ProcessOperator.runCommandAsync(dir.parentFile, listOf("git", "clone", cloneUrl)).await()
            if (p1.exitCode != 0) {
                NotificationsOperator.show(
                        "Clone failed",
                        "Clone in dir ${p1.dir} failed with code ${p1.exitCode} and output ${p1.std}",
                        NotificationType.ERROR
                )
            } else {
                val p2 = ProcessOperator.runCommandAsync(dir, listOf("git", "checkout", "-b", branch)).await()
                if (p2.exitCode != 0) {
                    NotificationsOperator.show(
                            "Branch switch failed",
                            "Branch switch in dir ${p2.dir} failed with code ${p2.exitCode} and output ${p2.std}",
                            NotificationType.ERROR
                    )
                }
            }
        }
    }
}
