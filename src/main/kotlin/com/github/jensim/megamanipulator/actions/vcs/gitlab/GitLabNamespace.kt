package com.github.jensim.megamanipulator.actions.vcs.gitlab

import kotlinx.serialization.Serializable

@Serializable
data class GitLabNamespace(
        val path: String
)
