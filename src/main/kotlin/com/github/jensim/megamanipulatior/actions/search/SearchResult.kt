package com.github.jensim.megamanipulatior.actions.search

data class SearchResult(
    val project: String,
    val repo: String,
    val codeHostName: String,
    val searchHostName: String,
)
