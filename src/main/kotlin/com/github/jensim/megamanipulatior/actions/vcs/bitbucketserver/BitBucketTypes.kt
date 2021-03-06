package com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver

data class BitBucketDashboardPullRequestResponse(
    val isLastPage: Boolean,
    val size: Long,
    val limit: Long,
    val values: List<BitBucketPullRequest>,
)

data class BitBucketPullRequest(
    val id: Long? = null,
    val version: Int? = null,
    val title: String,
    val description: String,
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
    val project: BitBucketProject,
)

data class BitBucketProject(
    val key: String
)
