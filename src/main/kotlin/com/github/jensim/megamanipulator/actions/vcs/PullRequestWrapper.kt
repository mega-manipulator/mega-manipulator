@file:SuppressWarnings("TooManyFunctions")

package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitBucketPullRequest
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComPullRequest
import com.github.jensim.megamanipulator.actions.vcs.gitlab.GitLabMergeRequest
import com.github.jensim.megamanipulator.settings.types.CloneType
import kotlinx.serialization.Serializable

@Serializable
sealed class PullRequestWrapper : GitCloneable {
    abstract fun codeHostName(): String
    abstract fun searchHostName(): String
    abstract fun title(): String
    abstract fun body(): String
    abstract fun fromBranch(): String
    abstract fun toBranch(): String
    abstract fun browseUrl(): String?
    abstract val raw: String
    fun asPathString(): String = "${searchHostName()}/${codeHostName()}/${project()}/${baseRepo()}"
}

@Serializable
data class BitBucketPullRequestWrapper(
    val searchHost: String,
    val codeHost: String,
    val bitbucketPR: BitBucketPullRequest,
    override val raw: String
) : PullRequestWrapper() {
    override fun codeHostName(): String = codeHost
    override fun searchHostName(): String = searchHost
    override fun project(): String = bitbucketPR.toRef.repository.project!!.key
    override fun baseRepo(): String = bitbucketPR.toRef.repository.slug
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

    override fun isFork(): Boolean = bitbucketPR.toRef.repository.let { "${it.project?.key}/${it.slug}" } != bitbucketPR.fromRef.repository.let { "${it.project?.key}/${it.slug}" }
    override fun cloneUrlFrom(cloneType: CloneType): String? = when (cloneType) {
        CloneType.SSH -> bitbucketPR.fromRef.repository.links?.clone?.firstOrNull { it.name == "ssh" }?.href
        CloneType.HTTPS -> bitbucketPR.fromRef.repository.links?.clone?.firstOrNull { it.name == "http" }?.href
    }

    override fun cloneUrlTo(cloneType: CloneType): String? = when (cloneType) {
        CloneType.SSH -> bitbucketPR.toRef.repository.links?.clone?.firstOrNull { it.name == "ssh" }?.href
        CloneType.HTTPS -> bitbucketPR.toRef.repository.links?.clone?.firstOrNull { it.name == "http" }?.href
    }

    override fun browseUrl(): String? = bitbucketPR.links?.self?.firstOrNull()?.href
}

@Serializable
data class GithubComPullRequestWrapper(
    val searchHost: String,
    val codeHost: String,
    val pullRequest: GithubComPullRequest,
    override val raw: String
) : PullRequestWrapper() {
    override fun codeHostName(): String = codeHost
    override fun searchHostName(): String = searchHost
    override fun project(): String = pullRequest.base?.repo?.owner?.login ?: "<?>"
    override fun baseRepo(): String = pullRequest.base?.repo?.name ?: "<?>"
    override fun title(): String = pullRequest.title
    override fun body(): String = pullRequest.body
    override fun fromBranch(): String = pullRequest.head?.ref ?: "<?>"
    override fun toBranch(): String = pullRequest.base?.ref ?: "<?>"
    override fun isFork(): Boolean = pullRequest.head?.repo?.fork ?: false && (pullRequest.base?.repo?.id != pullRequest.head?.repo?.id)
    override fun cloneUrlFrom(cloneType: CloneType): String = when (cloneType) {
        CloneType.SSH -> pullRequest.head?.repo?.ssh_url ?: "<?>"
        CloneType.HTTPS -> pullRequest.head?.repo?.clone_url ?: "<?>"
    }

    override fun cloneUrlTo(cloneType: CloneType): String = when (cloneType) {
        CloneType.SSH -> pullRequest.base?.repo?.ssh_url ?: "<?>"
        CloneType.HTTPS -> pullRequest.base?.repo?.clone_url ?: "<?>"
    }

    override fun browseUrl(): String = pullRequest.html_url
}

sealed class GitLabMergeRequestWrapper : PullRequestWrapper() {
    abstract val targetProjectId: Long
    abstract val sourceProjectId: Long
    abstract val mergeRequestId: Long
    abstract val mergeRequestIid: Long
}

data class GitLabMergeRequestListItemWrapper(
    val searchHost: String,
    val codeHost: String,
    val mergeRequest: com.github.jensim.megamanipulator.graphql.generated.gitlab.getauthoredpullrequests.MergeRequest,
    override val raw: String,
) : GitLabMergeRequestWrapper() {
    override val targetProjectId = mergeRequest.targetProject.id.removePrefix("gid://gitlab/Project/").toLong()
    override val sourceProjectId = mergeRequest.sourceProject?.id?.removePrefix("gid://gitlab/Project/")?.toLong()!!
    override val mergeRequestId = mergeRequest.id.removePrefix("gid://gitlab/MergeRequest/").toLong()
    override val mergeRequestIid = mergeRequest.iid.toLong()

    override fun codeHostName(): String = codeHost
    override fun searchHostName(): String = searchHost
    override fun project(): String = mergeRequest.targetProject.path
    override fun baseRepo(): String = mergeRequest.targetProject.group?.path ?: "<?>"
    override fun title(): String = mergeRequest.title
    override fun body(): String = mergeRequest.description ?: ""
    override fun fromBranch(): String = mergeRequest.sourceBranch
    override fun toBranch(): String = mergeRequest.targetBranch
    override fun isFork(): Boolean = mergeRequest.sourceProject?.path != mergeRequest.targetProject.path
    override fun cloneUrlFrom(cloneType: CloneType): String? = when (cloneType) {
        CloneType.SSH -> mergeRequest.sourceProject?.sshUrlToRepo
        CloneType.HTTPS -> mergeRequest.sourceProject?.httpUrlToRepo
    }

    override fun cloneUrlTo(cloneType: CloneType): String? = when (cloneType) {
        CloneType.SSH -> mergeRequest.targetProject.sshUrlToRepo
        CloneType.HTTPS -> mergeRequest.targetProject.httpUrlToRepo
    }

    override fun browseUrl(): String? = mergeRequest.webUrl
}

data class GitLabAssignedMergeRequestListItemWrapper(
    val searchHost: String,
    val codeHost: String,
    val mergeRequest: com.github.jensim.megamanipulator.graphql.generated.gitlab.getassignedpullrequests.MergeRequest,
    override val raw: String,
) : GitLabMergeRequestWrapper() {
    override val targetProjectId = mergeRequest.targetProject.id.removePrefix("gid://gitlab/Project/").toLong()
    override val sourceProjectId = mergeRequest.sourceProject?.id?.removePrefix("gid://gitlab/Project/")?.toLong()!!
    override val mergeRequestId = mergeRequest.id.removePrefix("gid://gitlab/MergeRequest/").toLong()
    override val mergeRequestIid = mergeRequest.iid.toLong()

    override fun codeHostName(): String = codeHost
    override fun searchHostName(): String = searchHost
    override fun project(): String = mergeRequest.targetProject.path
    override fun baseRepo(): String = mergeRequest.targetProject.group?.path ?: "<?>"
    override fun title(): String = mergeRequest.title
    override fun body(): String = mergeRequest.description ?: ""
    override fun fromBranch(): String = mergeRequest.sourceBranch
    override fun toBranch(): String = mergeRequest.targetBranch
    override fun isFork(): Boolean = mergeRequest.sourceProject?.path != mergeRequest.targetProject.path
    override fun cloneUrlFrom(cloneType: CloneType): String? = when (cloneType) {
        CloneType.SSH -> mergeRequest.sourceProject?.sshUrlToRepo
        CloneType.HTTPS -> mergeRequest.sourceProject?.httpUrlToRepo
    }

    override fun cloneUrlTo(cloneType: CloneType): String? = when (cloneType) {
        CloneType.SSH -> mergeRequest.targetProject.sshUrlToRepo
        CloneType.HTTPS -> mergeRequest.targetProject.httpUrlToRepo
    }

    override fun browseUrl(): String? = mergeRequest.webUrl
}

data class GitLabMergeRequestApiWrapper(
    val searchHost: String,
    val codeHost: String,
    val mergeRequest: GitLabMergeRequest,
    private val cloneable: GitCloneable,
    override val raw: String,
) : GitLabMergeRequestWrapper() {
    override val targetProjectId = mergeRequest.target_project_id
    override val sourceProjectId = mergeRequest.source_project_id
    override val mergeRequestId = mergeRequest.id
    override val mergeRequestIid = mergeRequest.iid

    override fun codeHostName(): String = codeHost
    override fun searchHostName(): String = searchHost
    override fun project(): String = cloneable.project()
    override fun baseRepo(): String = cloneable.baseRepo()
    override fun title(): String = mergeRequest.title
    override fun body(): String = mergeRequest.description
    override fun fromBranch(): String = mergeRequest.source_branch
    override fun toBranch(): String = mergeRequest.target_branch
    override fun isFork(): Boolean = mergeRequest.source_project_id != mergeRequest.target_project_id
    override fun cloneUrlFrom(cloneType: CloneType): String? = cloneable.cloneUrlFrom(cloneType)
    override fun cloneUrlTo(cloneType: CloneType): String? = cloneable.cloneUrlTo(cloneType)
    override fun browseUrl(): String = mergeRequest.web_url
}
