package com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver

data class BitBucketMessage(
    // val context: String?
    val message: String,
    val exceptionName: String?
)

data class BitBucketPage<T>(
    val start: Int?,
    val size: Int?,
    val limit: Int?,
    val isLastPage: Boolean?,
    val values: List<T>?,
    val message: String?,
)

data class BitBucketPullRequest(
    val id: Long? = null,
    val version: Int? = null,
    val title: String,
    val description: String?,
    val state: String = "OPEN",
    val open: Boolean = true,
    val closed: Boolean = false,
    val locked: Boolean = false,
    val fromRef: BitBucketBranchRef,
    val toRef: BitBucketBranchRef,
    val author: BitBucketParticipant? = null,
    val reviewers: List<BitBucketParticipant>,
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
    val links: BitBucketRepoLinks? = null,
)

data class BitBucketForkRequest(
    val slug: String,
    val name: String? = null,
    val project: BitBucketProjectRequest,
    val defaultBranch: String? = null,
)

data class BitBucketProjectRequest(
    val key: String
)

data class BitBucketRepoLinks(
    val clone: List<BitBucketCloneLink>,
    val self: List<BitBucketPlainLink>,
)

data class BitBucketCloneLink(
    val href: String,
    val name: String,
)

data class BitBucketPlainLink(
    val href: String
)

data class BitBucketProject(
    val key: String
)
