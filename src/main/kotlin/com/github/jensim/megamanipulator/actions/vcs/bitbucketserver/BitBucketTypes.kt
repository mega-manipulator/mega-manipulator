package com.github.jensim.megamanipulator.actions.vcs.bitbucketserver

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

@Serializable
data class BitBucketMessage(
    // val context: String?
    val message: String,
    val exceptionName: String?
)

@Serializable
data class BitBucketPage(
    val start: Int? = null,
    val size: Int? = null,
    val limit: Int? = null,
    val isLastPage: Boolean? = null,
    val values: List<JsonElement>? = null,
    val message: String? = null,
)

@Serializable
data class BitBucketPullRequestRequest(
    val title: String,
    val description: String? = null,
    val state: String = "OPEN",
    val open: Boolean = true,
    val closed: Boolean = false,
    val locked: Boolean = false,
    val fromRef: BitBucketBranchRef,
    val toRef: BitBucketBranchRef,
    val reviewers: List<BitBucketParticipant>,
)

@Serializable
data class BitBucketRemoveBranchRequest(
    val name: String,
    val dryRun: Boolean
)

@Serializable
data class BitBucketParticipantStatusRequest(
    val user: BitBucketUser,
    val approved: Boolean,
    val status: BitBucketPullRequestStatus
)

@Serializable
enum class BitBucketPullRequestStatus {
    APPROVED,
    UNAPPROVED,
    NEEDS_WORK
}

@Serializable
data class BitBucketComment(
    val text: String
)

@Serializable
data class BitBucketPullRequest(
    val id: Long? = null,
    val version: Int? = null,
    val title: String,
    val description: String? = null,
    val state: String? = null,
    val open: Boolean? = null,
    val closed: Boolean? = null,
    val locked: Boolean? = null,
    val fromRef: BitBucketBranchRef,
    val toRef: BitBucketBranchRef,
    val author: BitBucketParticipant? = null,
    val reviewers: List<BitBucketParticipant>? = null,
    val links: BitBucketPullRequestLinks? = null,
)

@Serializable
data class BitBucketPullRequestLinks(
    val self: List<BitBucketPlainLink>
)

@Serializable
data class BitBucketParticipant(
    val user: BitBucketUser
)

@Serializable
data class BitBucketUser(
    val name: String,
    val displayName: String? = null,
    val emailAddress: String? = null,
)

@Serializable
data class BitBucketBranchRef(
    val id: String,
    val repository: BitBucketRepo,
)

@Serializable
data class BitBucketDefaultBranch(
    val id: String, // "refs/heads/main",
    val displayId: String, // "main",
    val type: String, // "BRANCH"
)

@Serializable
data class BitBucketRepo(
    val id: Long? = null,
    val slug: String,
    val scmId: String? = null,
    val project: BitBucketProject? = null,
    val origin: BitBucketRepo? = null,
    val links: BitBucketRepoLinks? = null,
)

@Serializable
data class BitBucketForkRequest(
    val slug: String,
    val name: String? = null,
    val project: BitBucketProjectRequest,
    val defaultBranch: String? = null,
)

@Serializable
data class BitBucketProjectRequest(
    val key: String,
)

@Serializable
data class BitBucketRepoLinks(
    val clone: List<BitBucketCloneLink>? = null,
    val self: List<BitBucketPlainLink>? = null,
)

@Serializable
data class BitBucketCloneLink(
    val href: String,
    val name: String,
)

@Serializable
data class BitBucketPlainLink(
    val href: String,
)

@Serializable
data class BitBucketProject(
    val key: String,
)
