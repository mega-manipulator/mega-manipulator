package com.github.jensim.megamanipulator.actions.search.github

import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComRepo

data class GitHubCode(
    /** file name */
    val name: String,
    /** file path */
    val path: String,
    /** search score */
    val score: Long,
    val repository: GithubComRepo,
)
