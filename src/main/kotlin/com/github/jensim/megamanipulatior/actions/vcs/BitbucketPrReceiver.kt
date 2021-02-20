package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.search.SearchResult

object BitbucketPrReceiver : PrReceiver {
    override fun getDefaultBranch(repo: SearchResult): String = TODO("Not yet implemented")
    override fun getDefaultReviewers(repo: SearchResult): List<String> = TODO("not implemented")
    override fun getPr(pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")
    override fun createPr(pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")
    override fun updatePr(pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")
    override fun getAllPrs(): List<PullRequest> = TODO("Not yet implemented")
}
