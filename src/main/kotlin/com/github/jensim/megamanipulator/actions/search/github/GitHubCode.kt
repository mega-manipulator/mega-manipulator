package com.github.jensim.megamanipulator.actions.search.github

data class GitHubCode(
    /** file name */
    val name: String,
    /** file path */
    val path: String,
    /** search score */
    val score: Long,
    val repository: GithubRepo,
)
