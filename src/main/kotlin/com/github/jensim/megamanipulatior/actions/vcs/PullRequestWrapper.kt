package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver.BitBucketPullRequest

sealed class PullRequestWrapper(
    open val searchHost: String,
    open val codeHost: String
) : PullRequest

data class BitBucketPullRequestWrapper(
    override val searchHost: String,
    override val codeHost: String,
    val bitbucketPR: BitBucketPullRequest,
) : PullRequestWrapper(searchHost, codeHost) {
    override fun codeHostName(): String = codeHost
    override fun searchHostName(): String = searchHost
    override fun project(): String = bitbucketPR.toRef.repository.project.key
    override fun repo(): String = bitbucketPR.toRef.repository.slug
    override fun title(): String = bitbucketPR.title
    override fun body(): String = bitbucketPR.description
    override fun fromBranch(): String = bitbucketPR.fromRef.id
    override fun toBranch(): String = bitbucketPR.toRef.id
    override fun alterCopy(
        codeHostName: String?,
        searchHostName: String?,
        title: String?,
        body: String?,
    ): BitBucketPullRequestWrapper = copy(
        codeHost = codeHostName ?: codeHostName(),
        searchHost = searchHostName ?: searchHostName(),
        bitbucketPR = bitbucketPR.copy(
            title = title ?: title(),
            description = body ?: body(),
        ),
    )
}
