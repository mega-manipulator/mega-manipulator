package com.github.jensim.megamanipulatior.actions.vcs

data class PullRequest(
    val id: String,
    val version: String?,
    val codeHostName: String,
    val searchHostName: String,
    val project: String,
    val repo: String,
    val branchFrom: BranchRef,
    val branchTo: BranchRef,
    val title: String,
    val body: String,
    val reviewers: List<Reviewer>
)

data class Reviewer(
    val name: String,
    val displayName: String,
)

data class BranchRef(
    val branchName: String,
    val repo: String,
    val project: String,
)
