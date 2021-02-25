package com.github.jensim.megamanipulatior.actions.git

import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.settings.ProjectOperator
import java.io.File
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

object GitOperator {
    fun getBranch(searchResult: SearchResult): String? {
        val dir = File("${ProjectOperator.project.basePath}/clones/${searchResult.searchHostName}/${searchResult.codeHostName}/${searchResult.project}/${searchResult.repo}")
        return try {
            FileRepositoryBuilder.create(dir).branch
        } catch (e: Exception) {
            null
        }
    }
}
