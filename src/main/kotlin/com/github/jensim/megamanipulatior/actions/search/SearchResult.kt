package com.github.jensim.megamanipulatior.actions.search

import java.io.File

data class SearchResult(
    val project: String,
    val repo: String,
    val codeHostName: String,
    val searchHostName: String,
) {
    companion object {
        fun fromPath(dir: File): SearchResult {
            return SearchResult(
                repo = dir.name,
                project = dir.parentFile.name,
                codeHostName = dir.parentFile.parentFile.name,
                searchHostName = dir.parentFile.parentFile.parentFile.name
            )
        }
    }

    fun asPathString(): String = "${searchHostName}/${codeHostName}/${project}/${repo}"
}
