package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.search.SearchResult

interface PrReceiver {

    fun getDefaultBranch(repo: SearchResult): String
    fun getDefaultReviewers(repo: SearchResult): List<String>
    fun getPr(pullRequest: PullRequest): PullRequest?
    fun createPr(pullRequest: PullRequest): PullRequest?
    fun updatePr(pullRequest: PullRequest): PullRequest?
    fun getAllPrs(): List<PullRequest>
}
