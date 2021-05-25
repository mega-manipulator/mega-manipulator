package com.github.jensim.megamanipulator.actions.vcs.gitlab

import kotlinx.serialization.Serializable

@Serializable
data class GitLabProject(
        val id: Long,
        val default_branch: String,
        val namespace: GitLabNamespace,
        val path: String,
        val ssh_url_to_repo: String, // ": "git@gitlab.com:jensim1/dump.git",
        val http_url_to_repo: String, // ": "https://gitlab.com/jensim1/dump.git",
        val web_url: String, // ": "https://gitlab.com/jensim1/dump",
)
