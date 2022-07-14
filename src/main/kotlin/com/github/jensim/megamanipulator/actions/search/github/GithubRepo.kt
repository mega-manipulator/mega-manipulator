package com.github.jensim.megamanipulator.actions.search.github

data class GithubRepo(
    val name: String,
    val owner: GithubRepoOwner,
) {
    data class GithubRepoOwner(
        val login: String,
    )
}
