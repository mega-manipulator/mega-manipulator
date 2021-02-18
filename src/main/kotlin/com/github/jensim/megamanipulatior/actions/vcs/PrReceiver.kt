package com.github.jensim.megamanipulatior.actions.vcs

interface PrReceiver {

    fun getPr(pullRequest: PullRequest): PullRequest?
    fun createPr(pullRequest: PullRequest): PullRequest?
    fun updatePr(pullRequest: PullRequest): PullRequest?
    fun getAllPrs(): List<PullRequest>
}