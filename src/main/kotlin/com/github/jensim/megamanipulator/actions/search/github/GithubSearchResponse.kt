package com.github.jensim.megamanipulator.actions.search.github

data class GithubSearchResponse<T> (
    val total_count: Long,
    val incomplete_results: Boolean,
    val items: List<T>? = null,
)
