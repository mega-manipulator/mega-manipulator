package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitBucketPullRequest
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComPullRequest

sealed class PullRequestWrapper {
    abstract fun codeHostName(): String
    abstract fun searchHostName(): String
    abstract fun project(): String
    abstract fun repo(): String
    abstract fun title(): String
    abstract fun body(): String
    abstract fun fromBranch(): String
    abstract fun toBranch(): String

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
    fun alterCopy(
            title: String?,
            body: String?,
    ): BitBucketPullRequestWrapper = copy(
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

data class GithubComPullRequestWrapper(
        val searchHost: String,
        val codeHost: String,
        val pullRequest: GithubComPullRequest,
) : PullRequestWrapper() {
    override fun codeHostName(): String = codeHost
    override fun searchHostName(): String = searchHost
    override fun project(): String = pullRequest.base?.repo?.owner?.login ?: "<?>"
    override fun repo(): String = pullRequest.base?.repo?.name ?: "<?>"
    override fun title(): String = pullRequest.title
    override fun body(): String = pullRequest.body
    override fun fromBranch(): String = pullRequest.head?.ref ?: "<?>"
    override fun toBranch(): String = pullRequest.base?.ref ?: "<?>"
    override fun isFork(): Boolean = pullRequest.head?.repo?.fork ?: false && (pullRequest.base?.repo?.id != pullRequest.head?.repo?.id)
    override fun cloneUrlFrom(): String = pullRequest.head?.repo?.ssh_url ?: "<?>"
    override fun cloneUrlTo(): String = pullRequest.base?.repo?.ssh_url ?: "<?>"
}
