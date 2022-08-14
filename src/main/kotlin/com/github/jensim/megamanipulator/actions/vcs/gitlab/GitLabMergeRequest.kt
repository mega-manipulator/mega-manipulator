package com.github.jensim.megamanipulator.actions.vcs.gitlab

@Suppress("ConstructorParameterNaming")
data class GitLabMergeRequest(
    val id: Long, // ": 1,
    val iid: Long, // ": 1,
    val project_id: Long, // ": 3,
    val title: String, // ": "test1",
    val author: GitLabAuthor?,
    val description: String, // ": "fixed login page css paddings",
    val state: String, // ": "merged",
    val target_branch: String, // ": "master",
    val source_branch: String, // ": "test1",
    val source_project_id: Long, // ": 2,
    val target_project_id: Long, // ": 3,
    val web_url: String, // ": "http://gitlab.example.com/my-group/my-project/merge_requests/1",
)

data class GitLabAuthor(
    val username: String?,
    val name: String?,
)
