package com.github.jensim.megamanipulatior.actions.git.clone

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.settings.ProjectOperator
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.github.jensim.megamanipulatior.ui.SingleThreadContext
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.WARNING
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

object CloneOperator {

    fun clone(branch: String, repos: Set<SearchResult>) {

        val settings = SettingsFileOperator.readSettings()!!
        val context: CoroutineContext = if (settings.forceSingleThreaded) {
            SingleThreadContext
        } else {
            Dispatchers.Default
        }
        val basePath = ProjectOperator.project.basePath
        val noConf = mutableListOf<SearchResult>()
        repos.mapConcurrentWithProgress(title = "Cloning repos", coroutineContext = context, cancelable = true) { repo ->
            settings.resolveSettings(repo.searchHostName, repo.codeHostName)?.let { (_, codeHostSettings) ->
                val cloneUrl = codeHostSettings.cloneUrl(repo.project, repo.repo)
                val dir = File(basePath, "clones/${repo.asPathString()}")
                dir.mkdirs()
                if (File(dir, ".git").exists()) {
                    // TODO
                    NotificationsOperator.show(
                        title = "Repo already cloned",
                        body = "Repo ${repo.asPathString()} already cloned.\nWill not do anything.",
                        type = WARNING
                    )
                } else {
                    val p1 = ProcessOperator.runCommand(dir.parentFile, arrayOf("git", "clone", cloneUrl))?.get()
                    if (p1?.exitCode != 0) {
                        NotificationsOperator.show(
                            "Clone failed",
                            "Clone in dir ${p1?.dir} failed with code ${p1?.exitCode} and output ${p1?.std}",
                            NotificationType.ERROR
                        )
                    } else {
                        val p2 = ProcessOperator.runCommand(dir, arrayOf("git", "checkout", "-b", branch))?.get()
                        if (p2?.exitCode != 0) {
                            NotificationsOperator.show(
                                "Branch switch failed",
                                "Branch switch in dir ${p2?.dir} failed with code ${p2?.exitCode} and output ${p2?.std}",
                                NotificationType.ERROR
                            )
                        }
                    }
                }
            } ?: noConf.add(repo)
        }

        if (noConf.isEmpty()) {
            println("All done")
        } else {
            println("All done, no conf found for $noConf")
        }
    }
}
