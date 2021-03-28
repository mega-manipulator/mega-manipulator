@file:SuppressWarnings("ConstructorParameterNaming")

package com.github.jensim.megamanipulator.actions.vcs.githubcom

data class GithubComRepo(
    // https://api.github.com/users/jensim/repos?page=1
    val id: Long, // 1296269,
    val name: String, // "Hello-World",
    val full_name: String, // "octocat/Hello-World",
    val owner: GithubComUser,
    val private: Boolean,
    val description: String?,
    val ssh_url: String, // git@github.com:spring-projects/spring-security.git,
    val fork: Boolean,
    val default_branch: String,
    val html_url: String,
    val clone_url: String, // https://github.com/spring-projects/spring-security.git,
    val forks_url: String,
    val parent: GithubComRepo?,
    val license: GithubComLicence?,
    val open_issues_count: Long,
)

data class GithubComUser(
    val login: String, // "octocat",
    val id: Long, // 1296269,
    val type: String, // Organization, User
)

data class GithubComLicence(
    val key: String?, // ": "apache-2.0",
    val name: String?, // ": "Apache License 2.0",
    val spdx_id: String?, // ": "Apache-2.0",
    val url: String?, // ": "https://api.github.com/licenses/apache-2.0",
    val node_id: String?, // ": "MDc6TGljZW5zZTI="
)

data class GithubComPullRequest(
    val id: Long,
    // https://api.github.com/repos/jensim/jensim.github.io/pulls/3
    val url: String,
    val html_url: String,
    val user: GithubComUser,
    val body: String,
    val state: String,
    val title: String,
    /** from */
    val head: GithubComRef?,
    /** to */
    val base: GithubComRef?,
)

data class GithubComRef(
    /** branch */
    val ref: String?,
    val repo: GithubComRepo?,
)

data class GithubComSearchResult<T>(
    val total_count: Long,
    val incomplete_results: Boolean,
    val items: List<T>,
)

data class GithubComIssue(
    val id: Long,
    val node_id: String,
    val pull_request: GithubComPullRequestLinks?,
)

data class GithubComPullRequestLinks(
    val url: String?
)
