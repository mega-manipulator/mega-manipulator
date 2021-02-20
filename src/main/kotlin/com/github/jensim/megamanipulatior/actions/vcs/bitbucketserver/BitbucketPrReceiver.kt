package com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver

import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.actions.vcs.PrReceiver
import com.github.jensim.megamanipulatior.actions.vcs.PullRequest
import com.github.jensim.megamanipulatior.settings.BitBucketSettings
import java.io.File

object BitbucketPrReceiver : PrReceiver<BitBucketSettings> {

    override fun getDefaultBranch(conf: BitBucketSettings, repo: SearchResult): String {

        TODO("Not yet implemented")
    }

    override fun getDefaultReviewers(conf: BitBucketSettings, repo: SearchResult): List<String> = TODO("not implemented")
    override fun getPr(conf: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")
    override fun createPr(conf: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")
    override fun updatePr(conf: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")
    override fun getAllPrs(conf: BitBucketSettings): List<PullRequest> {
        TODO("not implemented")
    }

    private fun getAllPrsForOneRepo(conf: BitBucketSettings, repo: File): List<PullRequest> {
        TODO("not implemented")
    }

    override fun closePr(conf: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")

    /*
    pull requests

        dashboard:
        https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp97

        per repo
        https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp292
    */

    /*
    default reviewers
    https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-default-reviewers-rest.html
     */
}
