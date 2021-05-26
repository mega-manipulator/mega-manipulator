package com.github.jensim.megamanipulator.actions.vcs.gitlab

import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming")
data class GitLabNamespace(
    val path: String
)
