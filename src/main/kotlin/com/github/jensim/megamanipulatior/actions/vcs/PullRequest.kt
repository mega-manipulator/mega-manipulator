package com.github.jensim.megamanipulatior.actions.vcs

data class PullRequest(
    val id: String?,
    val codeHost: String,
    val project: String,
    val repo: String,
    val branchFrom: String,
    val branchTo: String,
    val title: String,
    val body: String,
)
