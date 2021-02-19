package com.github.jensim.megamanipulatior.actions.git.clone

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.settings.ProjectOperator
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import java.io.File
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object CloneOperator {

    suspend fun clone(repos: Set<SearchResult>) {
        val settings = SettingsFileOperator.readSettings()!!
        val requestSemaphore = Semaphore(5)
        val basePath = ProjectOperator.project.basePath
        val noConf = mutableListOf<SearchResult>()
        val futures: List<Deferred<Any>> = repos.map { repo ->
            GlobalScope.async {
                settings.codeHostSettings.firstOrNull { codeHost ->
                    codeHost.settings.sourceGraphName == repo.codeHostName
                }?.let {
                    val cloneUrl = it.settings.cloneUrl(repo.project, repo.repo)
                    val dir = File(basePath, "clones/${repo.codeHostName}/${repo.project}/${repo.repo}")
                    dir.mkdirs()
                    if (!File(dir, ".git").exists()) {
                        requestSemaphore.withPermit {
                            ProcessOperator.runCommand(dir, "git clone $cloneUrl .")?.await()
                        }
                    } else {
                        println("Dir already cloned ${dir.absolutePath}")
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