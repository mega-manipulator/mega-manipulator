package com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver

data class BitBucketDashboardPullRequestResponse(
    val isLastPage: Boolean,
    val size: Long,
    val limit: Long,
    val values: List<BitBucketDashboardPullRequest>,
)

data class BitBucketDashboardPullRequest(
    val id: Long,
    val version: Int,
    val title: String,
    val description: String,
    val state: String,
    val fromRef: BitBucketBranchRef,
    val toRef: BitBucketBranchRef,
    val locked: Boolean,
    val author: BitBucketAuthor,
    val reviewers: List<BitBucketParticipant>,

    )

data class BitBucketAuthor(
    val user: BitBucketUser
)

data class BitBucketParticipant(
    val user: BitBucketUser
)

data class BitBucketUser(
    val name: String,
    val displayName: String,
    val emailAddress: String,
)

data class BitBucketBranchRef(
    val id: String,
    val repository: BitBucketRepo,
)

data class BitBucketRepo(
    val id: Long,
    val slug: String,
    val project: BitBucketProject,
)

data class BitBucketProject(
    val key: String
)
