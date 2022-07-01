package com.github.jensim.megamanipulator.actions.vcs.bitbucketserver

import com.fasterxml.jackson.databind.JsonNode

data class BitBucketMessage(
    // val context: String?
    val message: String,
    val exceptionName: String?
)

data class BitBucketPage(
    val start: Int? = null,
    val size: Int? = null,
    val limit: Int? = null,
    val isLastPage: Boolean? = null,
    val values: List<JsonNode>? = null,
    val message: String? = null,
)

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

data class BitBucketRemoveBranchRequest(
    val name: String,
    val dryRun: Boolean
)

data class BitBucketParticipantStatusRequest(
    val user: BitBucketUser,
    val approved: Boolean,
    val status: BitBucketPullRequestStatus
)

enum class BitBucketPullRequestStatus {
    APPROVED,
    UNAPPROVED,
    NEEDS_WORK
}

data class BitBucketComment(
    val text: String
)

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

data class BitBucketPullRequestLinks(
    val self: List<BitBucketPlainLink>
)

data class BitBucketParticipant(
    val user: BitBucketUser
)

data class BitBucketUser(
    val name: String,
    val displayName: String? = null,
    val emailAddress: String? = null,
)

data class BitBucketBranchRef(
    val id: String,
    val repository: BitBucketRepo,
)

data class BitBucketDefaultBranch(
    val id: String, // "refs/heads/main",
    val displayId: String, // "main",
    val type: String, // "BRANCH"
)

data class BitBucketRepo(
    val id: Long? = null,
    val slug: String,
    val scmId: String? = null,
    val project: BitBucketProject? = null,
    val origin: BitBucketRepo? = null,
    val links: BitBucketRepoLinks? = null,
)

data class BitBucketForkRequest(
    val slug: String,
    val name: String? = null,
    val project: BitBucketProjectRequest,
    val defaultBranch: String? = null,
)

data class BitBucketProjectRequest(
    val key: String,
)

data class BitBucketRepoLinks(
    val clone: List<BitBucketCloneLink>? = null,
    val self: List<BitBucketPlainLink>? = null,
)

data class BitBucketCloneLink(
    val href: String,
    val name: String,
)

data class BitBucketPlainLink(
    val href: String,
)

data class BitBucketProject(
    val key: String,
)
