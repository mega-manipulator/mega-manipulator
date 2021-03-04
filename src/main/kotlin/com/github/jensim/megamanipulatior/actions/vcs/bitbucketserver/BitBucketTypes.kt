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

data class BitBucketDefaultBranch(
    val id: String, // "refs/heads/main",
    val displayId: String, // "main",
    val type: String, // "BRANCH"
)

data class BitBucketRepo(
    val id: Long,
    val slug: String,
    val project: BitBucketProject,
)

data class BitBucketProject(
    val key: String
)

data class BitBucketPullRequestRequest(
    val id: Long?,
    // https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp293
    val title: String, //": "Talking Nerdy",
    val description: String, //": "It’s a kludge, but put the tuple from the database in the cache.",
    val state: String = "OPEN",
    val open: Boolean = true,
    val closed: Boolean = false,
    val locked: Boolean = false,
    val fromRef: BitBucketPrBranchRef,
    val toRef: BitBucketPrBranchRef,
    val reviewers: List<BitBucketPrReviewer>
)

data class BitBucketPrBranchRef(
    val id: String,
    val repository: BitBucketPrRepo,
)

data class BitBucketPrRepo(
    val slug: String,
    val project: BitBucketProject,
)

data class BitBucketPrReviewer(
    val user: BitBucketPrUser
)

data class BitBucketPrUser(
    val name: String
)
