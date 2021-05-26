package com.github.jensim.megamanipulator.actions.vcs.gitlab

import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming")
data class GitLabMergeRequestRequest(
    val source_branch: String,
    val target_branch: String,
    val target_project_id: Long,
    val title: String,
    val description: String,
)
