package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitBucketPullRequest

sealed class PullRequestWrapper {
    abstract fun codeHostName(): String
    abstract fun searchHostName(): String
    abstract fun project(): String
    abstract fun repo(): String
    abstract fun title(): String
    abstract fun body(): String
    abstract fun fromBranch(): String
    abstract fun toBranch(): String
    abstract fun alterCopy(
            codeHostName: String? = null,
            searchHostName: String? = null,
            title: String? = null,
            body: String? = null,
    ): PullRequestWrapper

    abstract fun isFork(): Boolean
    abstract fun cloneUrlFrom(): String?
    abstract fun cloneUrlTo(): String?
}

data class BitBucketPullRequestWrapper(
        val searchHost: String,
        val codeHost: String,
        val bitbucketPR: BitBucketPullRequest,
) : PullRequestWrapper() {
    override fun codeHostName(): String = codeHost
    override fun searchHostName(): String = searchHost
    override fun project(): String = bitbucketPR.toRef.repository.project!!.key
    override fun repo(): String = bitbucketPR.toRef.repository.slug
    override fun title(): String = bitbucketPR.title
    override fun body(): String = bitbucketPR.description ?: ""
    override fun fromBranch(): String = bitbucketPR.fromRef.id.removePrefix("refs/heads/")
    override fun toBranch(): String = bitbucketPR.toRef.id.removePrefix("refs/heads/")
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

    override fun isFork(): Boolean = bitbucketPR.toRef.repository.slug != bitbucketPR.fromRef.repository.slug
    override fun cloneUrlFrom(): String? = bitbucketPR.fromRef.repository.links?.clone
            ?.firstOrNull { it.name == "ssh" }?.href

    override fun cloneUrlTo(): String? = bitbucketPR.toRef.repository.links?.clone
            ?.firstOrNull { it.name == "ssh" }?.href
}
