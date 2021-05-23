package com.github.jensim.megamanipulator.actions.vcs.gitlab

import kotlinx.serialization.Serializable

@Serializable
data class GitLabMergeRequest(
    val id: Int, // ": 1,
    val iid: Int, // ": 1,
    val project_id: Int, // ": 3,
    val title: String, // ": "test1",
    val description: String, // ": "fixed login page css paddings",
    val state: String, // ": "merged",
    val target_branch: String, // ": "master",
    val source_branch: String, // ": "test1",
    val source_project_id: Int, // ": 2,
    val target_project_id: Int, // ": 3,
    val web_url: String, // ": "http://gitlab.example.com/my-group/my-project/merge_requests/1",
)
