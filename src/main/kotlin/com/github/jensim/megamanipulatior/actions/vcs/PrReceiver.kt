package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.settings.CodeHostSettings

interface PrReceiver<T : CodeHostSettings> {

    fun getDefaultBranch(conf: T, repo: SearchResult): String
    fun getDefaultReviewers(conf: T, repo: SearchResult): List<String>
    fun getPr(conf: T, pullRequest: PullRequest): PullRequest?
    fun createPr(conf: T, pullRequest: PullRequest): PullRequest?
    fun updatePr(conf: T, pullRequest: PullRequest): PullRequest?
    fun getAllPrs(conf: T): List<PullRequest>
    fun closePr(conf: T, pullRequest: PullRequest): PullRequest?
}
