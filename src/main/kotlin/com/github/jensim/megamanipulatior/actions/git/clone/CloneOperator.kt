package com.github.jensim.megamanipulatior.actions.git.clone

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.settings.ProjectOperator
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import java.io.File
import javax.swing.JOptionPane
import javax.swing.JOptionPane.ERROR_MESSAGE
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object CloneOperator {

    suspend fun clone(branch: String, repos: Set<SearchResult>) {

        val settings = SettingsFileOperator.readSettings()!!
        val requestSemaphore = Semaphore(5)
        val basePath = ProjectOperator.project.basePath
        val noConf = mutableListOf<SearchResult>()
        val futures: List<Deferred<Any>> = repos.map { repo ->
            GlobalScope.async {
                settings.resolveSettings(repo.searchHostName, repo.codeHostName)?.let { (_, codeHostSettings) ->
                    val cloneUrl = codeHostSettings.cloneUrl(repo.project, repo.repo)
                    val dir = File(basePath, "clones/${repo.searchHostName}/${repo.codeHostName}/${repo.project}/${repo.repo}")
                    dir.mkdirs()
                    if (File(dir, ".git").exists()) {
                        // TODO Checkout branch from default branch
                        println("Dir already cloned ${dir.absolutePath}")
                    } else {
                        requestSemaphore.withPermit {
                            val p1 = ProcessOperator.runCommand(dir.parentFile, "git clone $cloneUrl")?.await()
                            if (p1?.exitCode != 0) {
                                JOptionPane.showMessageDialog(
                                    null, "Clone in dir ${p1?.dir} failed with code ${p1?.exitCode} and output ${p1?.std}",
                                    "Clone failed", ERROR_MESSAGE
                                )
                            } else {
                                val p2 = ProcessOperator.runCommand(dir, "git checkout -b $branch")?.await()
                                if (p2?.exitCode != 0) {
                                    JOptionPane.showMessageDialog(
                                        null,
                                        "Branch switch in dir ${p2?.dir} failed with code ${p2?.exitCode} and output ${p2?.std}",
                                        "Branch switch failed", ERROR_MESSAGE
                                    )
                                }
                            }
                        }
                    }
                } ?: noConf.add(repo)
            }
        }
        futures.awaitAll()
        if (noConf.isEmpty()) {
            println("All done")
        } else {
            println("All done, no conf found for $noConf")
        }
    }
}
