package com.github.jensim.megamanipulator.actions.vcs.gitlab

@Suppress("ConstructorParameterNaming")
data class GitLabMergeRequestRequest(
    val source_branch: String,
    val target_branch: String,
    val target_project_id: Long,
    val title: String,
    val description: String,
)
